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

package com.flow.platform.api.service;

import com.flow.platform.api.domain.job.Job;
import com.flow.platform.api.domain.job.NodeResult;
import com.flow.platform.api.service.job.JobService;
import com.flow.platform.api.service.job.NodeResultService;
import com.flow.platform.api.util.ZipUtil;
import com.flow.platform.cc.service.CmdCCService;
import com.flow.platform.core.exception.FlowException;
import com.flow.platform.core.exception.IllegalParameterException;
import com.flow.platform.domain.Cmd;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

/**
 * @author yh@firim
 */
@Service
public class LogServiceImpl implements LogService {

    @Autowired
    private NodeResultService nodeResultService;

    @Autowired
    private CmdCCService cmdCCService;

    @Autowired
    private JobService jobService;

    @Autowired
    private Path workspace;

    @Override
    public String findNodeLog(String path, Long number, Integer order) {
        Job job = jobService.find(path, number);
        NodeResult nodeResult = nodeResultService.find(job.getId(), order);

        if (!NodeResult.FINISH_STATUS.contains(nodeResult.getStatus())) {
            throw new FlowException("node result not finish");
        }

        return readStepLog(nodeResult);
    }

    @Override
    public Resource findJobLog(String path, Long buildNumber) {
        Job job = jobService.find(path, buildNumber);

        // only job finish can to download log
        if (!Job.FINISH_STATUS.contains(job.getStatus())) {
            throw new FlowException("job must finish");
        }

        Resource allResource;

        // read zip job log
        File zipFile = readJobLog(job);
        job.setLogPath(zipFile.getPath());
        jobService.update(job);

        InputStream inputStream;
        try {
            inputStream = new FileInputStream(zipFile);
            allResource = new InputStreamResource(inputStream);
        } catch (FileNotFoundException e) {
            throw new FlowException("read job log error");
        }

        return allResource;
    }

    /**
     * read step log from workspace/:flowName/log/:jobId/
     */
    private String readStepLog(NodeResult nodeResult) {

        Cmd cmd = cmdCCService.find(nodeResult.getCmdId());

        if (cmd == null) {
            throw new IllegalParameterException("Cmd not found");
        }

        Path filePath = Paths.get(cmd.getLogPath());

        try {
            return ZipUtil.readZipFile(new FileInputStream(filePath.toFile()));
        } catch (IOException e) {
            return "";
        }
    }

    /**
     * save job zip
     */
    private File cacheJobLog(Job job) {
        Path jobPath = getJobLogPath(job);
        Path zipPath = Paths.get(jobPath.getParent().toString(), job.getId().toString() + ".zip");
        Path destPath = Paths.get(jobPath.toString(), job.getId().toString() + ".zip");
        File folderFile = new File(jobPath.toString());
        File zipFile = new File(zipPath.toString());
        File destFile = new File(destPath.toString());

        try {
            ZipUtil.zipFolder(folderFile, zipFile);
            FileUtils.moveFile(zipFile, destFile);
        } catch (IOException e) {
            throw new FlowException("save zip log error");
        }

        return destFile;
    }

    /**
     * get job zip
     */
    private File readJobLog(Job job) {
        // read zip log from api
        Path jobPath = getJobLogPath(job);
        Path zipPath = Paths.get(jobPath.toString(), job.getId().toString() + ".zip");
        File zipFile = new File(zipPath.toString());
        if (zipFile.exists()) {
            return zipFile;
        }

        // read zip log from cc
        List<NodeResult> list = nodeResultService.list(job, true);
        if (list.isEmpty()) {
            throw new FlowException("node result is empty");
        }

        // download all log from cc
        for (NodeResult nodeResult : list) {
            try {
                FileUtils
                    .writeStringToFile(Paths.get(getJobLogPath(job).toString(), nodeResult.getName() + ".log").toFile(),
                        readStepLog(nodeResult));
            } catch (IOException e) {
            }
        }

        cacheJobLog(job);
        zipFile = new File(zipPath.toString());

        return zipFile;
    }

    /**
     * get job log path
     */
    private Path getJobLogPath(Job job) {
        return Paths.get(workspace.toString(), job.getNodeName(), "log", job.getId().toString());
    }
}
