/*
 * Copyright 2017 flow.ci
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.flow.platform.plugin.service;

import static com.flow.platform.plugin.domain.PluginStatus.DELETE;
import static com.flow.platform.plugin.domain.PluginStatus.INSTALLED;
import static com.flow.platform.plugin.domain.PluginStatus.INSTALLING;
import static com.flow.platform.plugin.domain.PluginStatus.IN_QUEUE;
import static com.flow.platform.plugin.domain.PluginStatus.PENDING;

import com.flow.platform.plugin.dao.PluginDao;
import com.flow.platform.plugin.domain.Plugin;
import com.flow.platform.plugin.domain.PluginDetail;
import com.flow.platform.plugin.domain.PluginStatus;
import com.flow.platform.plugin.event.PluginRefreshEvent;
import com.flow.platform.plugin.event.PluginRefreshEvent.Status;
import com.flow.platform.plugin.event.PluginStatusChangeEvent;
import com.flow.platform.plugin.exception.PluginException;
import com.flow.platform.plugin.util.CmdUtil;
import com.flow.platform.plugin.util.YmlUtil;
import com.flow.platform.plugin.util.docker.Docker;
import com.flow.platform.util.CommandUtil.Unix;
import com.flow.platform.util.ExceptionUtil;
import com.flow.platform.util.git.GitException;
import com.flow.platform.util.git.JGitUtil;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.util.Strings;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.revwalk.RevCommit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

/**
 * @author yh@firim
 */
@Log4j2
@Service
public class PluginServiceImpl extends ApplicationEventService implements PluginService {

    private final static String GIT_SUFFIX = ".git";

    private final static String LOCAL_REMOTE = "local";

    private final static String ORIGIN_REMOTE = "origin";

    private final static int REFRESH_CACHE_TASK_HEARTBEAT = 2 * 60 * 60 * 1000;

    private final static String YML_FILE_NAME = ".flow-plugin.yml";

    private final static String MASTER_BRANCH = "master";

    private final static String DIST = "dist";

    private final static String TMP = "tmp";

    @Value("${api.run.indocker}")
    private boolean runInDocker;

    // git clone folder
    @Autowired
    private Path gitWorkspace;

    // local library
    @Autowired
    private Path gitCacheWorkspace;

    @Autowired
    private ThreadPoolTaskExecutor pluginPoolExecutor;

    @Autowired
    private PluginDao pluginDao;

    @Autowired
    private String pluginSourceUrl;

    private final Map<Plugin, Future<?>> taskCache = new ConcurrentHashMap<>();

    private final List<Processor> processors = ImmutableList.of(
        new InitGitProcessor(),
        new FetchProcessor(),
        new CompareCommitProcessor(),
        new AnalysisYmlProcessor(),
        new BuildProcessor(),
        new PushProcessor()
    );

    @Override
    public Plugin find(String name) {
        return pluginDao.get(name);
    }

    @Override
    public Collection<Plugin> list(Set<PluginStatus> status, String keyword, Set<String> labels) {
        return pluginDao.list(status, keyword, labels);
    }

    @Override
    public Collection<String> labels() {
        return pluginDao.labels();
    }

    @Override
    public Plugin install(String pluginName) {
        Plugin plugin = find(pluginName);

        if (Objects.isNull(plugin)) {
            throw new PluginException("Plugin '" + pluginName + " not found', ensure the plugin name is exist");
        }

        // not finish can install plugin
        if (!Plugin.RUNNING_AND_FINISH_STATUS.contains(plugin.getStatus())) {
            log.trace("Plugin {} Enter To Queue", pluginName);

            // update plugin status
            updatePluginStatus(plugin, IN_QUEUE);

            // record future task
            Future<?> submit = pluginPoolExecutor.submit(new InstallRunnable(plugin));
            taskCache.put(plugin, submit);
            log.trace("Plugin {} finish To Queue", pluginName);
        }

        return plugin;
    }

    @Override
    public Plugin stop(String name) {
        Plugin plugin = find(name);

        if (Objects.isNull(plugin)) {
            throw new PluginException("not found plugin, please ensure the plugin name is exist");
        }

        if (!ImmutableSet.of(IN_QUEUE, INSTALLING).contains(plugin.getStatus())) {
            throw new PluginException("Sorry can not stop");
        }

        try {
            Future<?> submit = taskCache.get(plugin);
            if (!Objects.isNull(submit)) {
                submit.cancel(true);
            } else {
                plugin.setStopped(true);
            }
        } catch (Throwable e) {
            log.warn("Cannot cancel future: " + e.getMessage());
        } finally {
            // update plugin status
            updatePluginStatus(plugin, PENDING);
            taskCache.remove(plugin);
        }

        return plugin;
    }

    @Override
    public Plugin uninstall(String name) {
        Plugin plugin = find(name);

        if (Objects.isNull(plugin)) {
            throw new PluginException("not found plugin, please ensure the plugin name is exist");
        }

        // Running Plugin not uninstall
        if (Objects.equals(plugin.getStatus(), INSTALLING)) {
            throw new PluginException("running plugin not install");
        }

        // only finish to uninstall
        if (!Plugin.FINISH_STATUSES.contains(plugin.getStatus())) {
            throw new PluginException("running plugin not install");
        }

        for (Processor processor : processors) {
            processor.clean(plugin);
        }

        // update plugin status to PENDING therefore the status been reset
        updatePluginStatus(plugin, DELETE);
        return plugin;
    }

    @Override
    public void execInstallOrUpdate(Plugin plugin) {
        try {
            // update plugin status to INSTALLING
            updatePluginStatus(plugin, INSTALLING);

            for (Processor processor : processors) {
                processor.exec(plugin);
            }
        } catch (PluginException e) {
            plugin.setReason(ExceptionUtil.findRootCause(e).getMessage());
            updatePluginStatus(plugin, PENDING);
        } finally {
            taskCache.remove(plugin);
        }
    }

    @Override
    @Scheduled(fixedDelay = REFRESH_CACHE_TASK_HEARTBEAT)
    public void syncTask() {
        try {
            log.trace("Start Refresh Cache");
            dispatchEvent(new PluginRefreshEvent(this, pluginSourceUrl, Status.ON_PROGRESS));
            pluginDao.refresh();
        } catch (Throwable e) {
            log.warn(e.getMessage());
        } finally {
            dispatchEvent(new PluginRefreshEvent(this, pluginSourceUrl, Status.IDLE));
            log.trace("Finish Refresh Cache");
        }
    }

    private void updatePluginStatus(Plugin plugin, PluginStatus target) {
        switch (target) {
            case PENDING:
            case IN_QUEUE:
            case INSTALLING:
            case INSTALLED:
                plugin.setStatus(target);
                break;

            case DELETE:
                plugin.setStatus(PENDING);
                break;
        }

        pluginDao.update(plugin);
        dispatchEvent(new PluginStatusChangeEvent(this, plugin.getName(), plugin.getTag(), target));
    }

    /**
     * Git bare repos workspace
     */
    private Path gitRepoPath(Plugin plugin) {
        return Paths.get(gitWorkspace.toString(), plugin.getName() + GIT_SUFFIX);
    }

    /**
     * Build git clone path which clone repo from remote
     */
    private Path gitCachePath(Plugin plugin) {
        return Paths.get(gitCacheWorkspace.toString(), plugin.getName());
    }

    private interface Processor {

        void exec(Plugin plugin);

        void clean(Plugin plugin);
    }

    private class InitGitProcessor implements Processor {

        private final static String EMPTY_FILE = "empty.file";

        @Override
        public void exec(Plugin plugin) {
            log.trace("Start Init Git");
            try {
                // init bare
                Path cachePath = gitCachePath(plugin);
                Path localPath = gitRepoPath(plugin);

                JGitUtil.init(cachePath, false);
                JGitUtil.init(localPath, true);

                // remote set
                JGitUtil.remoteSet(cachePath, ORIGIN_REMOTE, plugin.getSource() + GIT_SUFFIX);
                JGitUtil.remoteSet(cachePath, LOCAL_REMOTE, localPath.toString());

                // if branch not exists then push branch
                if (!checkExistBranchOrNot(localPath)) {
                    log.trace("Not Found Branch Create Empty Branch");
                    commitSomething(cachePath);
                    JGitUtil.push(cachePath, LOCAL_REMOTE, "master");
                }
            } catch (Throwable e) {
                log.error("Git Init", e);
                throw new PluginException("Git Init", e);
            }
        }

        @Override
        public void clean(Plugin plugin) {
            try {
                FileUtils.deleteDirectory(gitCachePath(plugin).toFile());
                FileUtils.deleteDirectory(gitRepoPath(plugin).toFile());
            } catch (Throwable e) {
                log.error("Git Init Clean", e);
                throw new PluginException("Git Init Clean", e);
            }
        }

        private void commitSomething(Path path) {
            try (Git git = Git.open(path.toFile())) {
                Path emptyFilePath = Paths.get(path.toString(), EMPTY_FILE);

                try {
                    Files.createFile(emptyFilePath);
                } catch (FileAlreadyExistsException ignore) {
                }

                git.add()
                    .addFilepattern(".")
                    .call();

                git.commit()
                    .setMessage("add test branch")
                    .call();

            } catch (Throwable e) {
                log.error("Method: commitSomething Exception", e);
            }
        }

        private boolean checkExistBranchOrNot(Path path) {
            try (Git git = Git.open(path.toFile())) {
                if (git.branchList().call().isEmpty()) {
                    return false;
                }
            } catch (Throwable e) {
                log.error("Method: checkExistBranchOrNot Exception", e);
            }
            return true;
        }
    }

    private class FetchProcessor implements Processor {

        @Override
        public void exec(Plugin plugin) {
            log.trace("Fetch tags");
            try {
                JGitUtil.fetchTags(gitCachePath(plugin), ORIGIN_REMOTE);
            } catch (Throwable e) {
                log.error("Git Fetch", e);
                throw new PluginException(e.getMessage());
            }
        }

        @Override
        public void clean(Plugin plugin) {

        }
    }

    private class CompareCommitProcessor implements Processor {

        @Override
        public void exec(Plugin plugin) {
            log.trace("Compare commit id");

            try {
                // first checkout tag branch
                JGitUtil.checkout(gitCachePath(plugin), plugin.getTag());

                // compare commit id is equal tag's latest commit id
                RevCommit commit = JGitUtil.latestCommit(gitCachePath(plugin));

                if (!Objects.equals(plugin.getLatestCommit(), commit.getId().getName())) {
                    throw new PluginException("Tag's latest commit id is not user provided");
                }
            } catch (GitException e) {
                throw new PluginException(e.getMessage());
            }
        }

        @Override
        public void clean(Plugin plugin) {
        }
    }

    private class AnalysisYmlProcessor implements Processor {

        @Override
        public void exec(Plugin plugin) {
            log.trace("Start analysis YML from plugin");

            try {
                // first checkout plugin tag
                JGitUtil.checkout(gitCachePath(plugin), plugin.getTag());

                Path ymlFilePath = Paths.get(gitCachePath(plugin).toString(), YML_FILE_NAME);

                // detect yml
                if (ymlFilePath.toFile().exists()) {
                    String body = FileUtils.readFileToString(ymlFilePath.toFile(), Charsets.UTF_8);
                    plugin.setPluginDetail(YmlUtil.fromYml(body, PluginDetail.class));
                    updatePluginStatus(plugin, INSTALLING);

                    // return to master branch
                    JGitUtil.checkout(gitCachePath(plugin), MASTER_BRANCH);
                    log.trace("Finish analysis YML from plugin");

                    return;
                }

                throw new PluginException("The plugin description file '" + YML_FILE_NAME + "' is missing");

            } catch (Throwable throwable) {
                log.warn("Found Exception " + throwable.getMessage());
                throw new PluginException(throwable.getMessage());
            }
        }

        @Override
        public void clean(Plugin plugin) {

        }

    }

    private class BuildProcessor implements Processor {

        @Override
        public void exec(Plugin plugin) {
            try {

                // only build and image all in value to pull image
                if (!Strings.isBlank(plugin.getPluginDetail().getBuild()) && !Strings
                    .isBlank(plugin.getPluginDetail().getImage())) {

                    log.trace("Start build code");

                    // put from cache to local git workspace
                    Path cachePath = gitCachePath(plugin);
                    String latestGitTag = plugin.getTag();
                    JGitUtil.checkout(cachePath, latestGitTag);

                    // first pull image and build
                    if (!runInDocker) {
                        dockerPullAndBuild(plugin);
                    } else {
                        // if run in docker only build
                        build(plugin);
                    }

                    // second detect outputs
                    detectBuildArtifacts(plugin);

                    // third push outputs to localRepo
                    pushArtifactsToLocalRepo(plugin);

                    log.trace("Finish build code");
                }


            } catch (Throwable e) {
                log.error("Git Build", e);
                throw new PluginException("Git Build", e);
            }
        }

        private void build(Plugin plugin) {
            log.trace("Start build");
            Path cachePath = gitCachePath(plugin);
            String cmd = "cd " + cachePath.toString() + Unix.LINE_SEPARATOR + plugin.getPluginDetail().getBuild();
            CmdUtil.exeCmd(cmd);
            log.trace("Finish build");
        }

        private void dockerPullAndBuild(Plugin plugin) {
            Path cachePath = gitCachePath(plugin);
            Docker docker = new Docker();
            docker.pull(plugin.getPluginDetail().getImage());
            docker.runBuild(plugin.getPluginDetail().getImage(), plugin.getPluginDetail().getBuild(), cachePath);
            docker.close();
        }


        private void detectBuildArtifacts(Plugin plugin) {
            Path cachePath = gitCachePath(plugin);
            // default outputs is dist folder
            Path artifactPath = Paths.get(cachePath.toString(), DIST);
            if (!artifactPath.toFile().exists()) {
                throw new PluginException("Not found build outputs");
            }

            if (artifactPath.toFile().isDirectory() && Objects.equals(0, artifactPath.toFile().list().length)) {
                throw new PluginException("Not found build outputs");
            }
        }

        private void pushArtifactsToLocalRepo(Plugin plugin) {
            try {

                String latestGitTag = plugin.getTag();

                Path cachePath = gitCachePath(plugin);

                // default outputs is dist folder
                Path artifactPath = Paths.get(cachePath.toString(), DIST);

                // create tmp folder to store build outputs
                Path tmp = Paths.get(gitCacheWorkspace.toString(), "tmp");
                if (!tmp.toFile().exists()) {
                    Files.createDirectories(tmp);
                }

                // move artifacts to tmp folder
                Path actPath = Paths.get(tmp.toString(), DIST);
                if (actPath.toFile().exists()) {
                    FileUtils.deleteDirectory(actPath.toFile());
                }
                FileUtils.moveDirectory(artifactPath.toFile(), actPath.toFile());

                Path localPath = gitRepoPath(plugin);

                // init git and push tags
                JGitUtil.init(actPath, false);
                JGitUtil.remoteSet(actPath, LOCAL_REMOTE, localPath.toString());
                Git git = Git.open(actPath.toFile());

                git.add()
                    .addFilepattern(".")
                    .call();

                git.commit()
                    .setMessage("add build outputs")
                    .call();

                git.tag()
                    .setName(plugin.getTag())
                    .setMessage("add " + plugin.getTag())
                    .call();

                JGitUtil.push(actPath, LOCAL_REMOTE, latestGitTag);
                // set currentTag latestTag
                plugin.setCurrentTag(latestGitTag);
                updatePluginStatus(plugin, INSTALLED);

                // delete path
                FileUtils.deleteDirectory(actPath.toFile());
            } catch (Throwable e) {

            }

        }

        @Override
        public void clean(Plugin plugin) {

        }
    }

    private class PushProcessor implements Processor {

        @Override
        public void exec(Plugin plugin) {
            log.trace("Push tags to local");

            if (!Strings.isBlank(plugin.getPluginDetail().getImage()) && !Strings
                .isBlank(plugin.getPluginDetail().getBuild())) {
                return;
            }

            try {
                // put from cache to local git workspace
                Path cachePath = gitCachePath(plugin);
                String latestGitTag = plugin.getTag();
                JGitUtil.push(cachePath, LOCAL_REMOTE, latestGitTag);
                // set currentTag latestTag
                plugin.setCurrentTag(latestGitTag);
                updatePluginStatus(plugin, INSTALLED);
            } catch (GitException e) {
                log.error("Git Push", e);
                throw new PluginException("Git Push", e);
            }
        }

        @Override
        public void clean(Plugin plugin) {

        }
    }

    private class InstallRunnable implements Runnable {

        private final Plugin plugin;

        public InstallRunnable(Plugin plugin) {
            this.plugin = plugin;
        }

        @Override
        public void run() {
            if (Objects.equals(false, plugin.getStopped())) {
                log.trace("Plugin Start Install Or Update");
                execInstallOrUpdate(plugin);
                log.trace("Plugin Finish Install Or Update");
                return;
            }

            plugin.setStopped(false);
            plugin.setStatus(PluginStatus.PENDING);
            pluginDao.update(plugin);
            log.trace("Plugin Stopped");
        }
    }
}
