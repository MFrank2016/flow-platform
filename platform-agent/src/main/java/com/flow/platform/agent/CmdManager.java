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

package com.flow.platform.agent;

import com.flow.platform.agent.config.AgentConfig;
import com.flow.platform.agent.config.QueueConfig;
import com.flow.platform.agent.mq.RabbitClient;
import com.flow.platform.cmd.CmdExecutor;
import com.flow.platform.cmd.ProcListener;
import com.flow.platform.domain.CmdType;
import com.flow.platform.domain.Jsonable;
import com.flow.platform.domain.v1.Cmd;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

/**
 * Singleton class to handle command
 * <p>
 *
 * @author gy@fir.im
 */
@Log4j2
public final class CmdManager implements AutoCloseable {

    private final static CmdManager Instance = new CmdManager();

    public static CmdManager getInstance() {
        return Instance;
    }

    private final AgentConfig config = AgentConfig.getInstance();

    // Executor to execute operations
    @Getter
    private final ExecutorService executor = Executors.newCachedThreadPool(AgentManager.DEFAULT_THREAD_FACTORY);

    @Getter
    private ExecutorService cmdExecutor = createExecutorForRunShell();

    // handle extra listeners
    @Getter
    private final List<ProcListener> extraProcEventListeners = new ArrayList<>(5);

    @Getter
    private final Map<Cmd, CmdRunner> currentRunners = new ConcurrentHashMap<>(5);

    private final RabbitClient cmdCallbackClient;

    private CmdManager() {
        this.cmdCallbackClient = initCmdCallback(config.getQueue());
    }

    /**
     * Execute command from Cmd object by thread executor
     *
     * @param cmd Cmd object
     */
    public void execute(final Cmd cmd) {
        if (cmd.getType() == CmdType.RUN_SHELL) {
            cmdExecutor.execute(new ShellCmdRunner(cmd));
            return;
        }

        if (cmd.getType() == CmdType.KILL) {
            throw new UnsupportedOperationException();
        }

        if (cmd.getType() == CmdType.SHUTDOWN) {
            throw new UnsupportedOperationException();
        }

        if (cmd.getType() == CmdType.STOP) {
            throw new UnsupportedOperationException();
        }

        if (cmd.getType() == CmdType.SYSTEM_INFO) {
            throw new UnsupportedOperationException();
        }
    }

    public synchronized void kill() {
        Iterator<Entry<Cmd, CmdRunner>> iterator = currentRunners.entrySet().iterator();
        while(iterator.hasNext()) {
            Entry<Cmd, CmdRunner> entry = iterator.next();
            entry.getValue().kill();
            iterator.remove();
        }

        try {
            cmdExecutor.shutdownNow();
        } catch (Throwable ignore) {

        } finally {
            cmdExecutor = createExecutorForRunShell(); // reset cmd executor
            log.trace("Cmd thread terminated");
        }
    }

    @Override
    public void close() throws Exception {
        cmdExecutor.shutdownNow();

        if (!cmdExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
            cmdExecutor.shutdownNow();
        }

        currentRunners.clear();
    }

    private RabbitClient initCmdCallback(QueueConfig config) {
        String host = config.getHost();
        String callbackQueueName = config.getCallbackQueueName();
        return new RabbitClient(host, callbackQueueName, null);
    }

    private ThreadPoolExecutor createExecutorForRunShell() {
        return new ThreadPoolExecutor(
            config.getNumOfConcurrentCmd(),
            config.getNumOfConcurrentCmd(),
            0L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(),
            AgentManager.DEFAULT_THREAD_FACTORY,
            (r, executor) -> {
                if (r instanceof CmdRunner) {
                    CmdRunner runner = (CmdRunner) r;
                    runner.reject();
                }
            });
    }

    /**
     * collect agent info
     */
    private String collectionAgentInfo() {
        String javaVersion = System.getProperty("java.version");
        String osName = System.getProperty("os.name");
        Runtime runtime = Runtime.getRuntime();
        long total = runtime.totalMemory();
        long free = runtime.freeMemory();
        long use = total - free;

        Map<String, String> dic = new HashMap<>(7);
        dic.put("javaVersion", javaVersion);
        dic.put("osName", osName);
        dic.put("totalMemory", Long.toString(total));
        dic.put("useMemory", Long.toString(use));
        dic.put("zone", config.getPath().getZone());
        dic.put("name", config.getPath().getName());
        dic.put("agentVersion", "1.0");

        return Jsonable.GSON_CONFIG.toJson(dic);
    }

    private abstract class CmdRunner implements Runnable {

        @Getter
        protected final Cmd cmd;

        CmdRunner(Cmd cmd) {
            this.cmd = cmd;
        }

        void kill() {

        }

        void reject() {

        }
    }

    private class ShellCmdRunner extends CmdRunner {

        @Getter
        private final ProcEventHandler procEventHandler;

        @Getter
        private final LogEventHandler logEventHandler;

        @Getter
        private final CmdExecutor executor;

        ShellCmdRunner(Cmd cmd) {
            super(cmd);
            this.procEventHandler = new ProcEventHandler(cmdCallbackClient, getCmd(), extraProcEventListeners);
            this.logEventHandler = new LogEventHandler(cmd);

            this.executor = new CmdExecutor(
                procEventHandler,
                logEventHandler,
                cmd.getContext(),
                cmd.getWorkDir(),
                cmd.getOutputFilter(),
                cmd.getTimeout(),
                Lists.newArrayList(getCmd().getContent()));

            currentRunners.put(cmd, this);
        }

        @Override
        public void run() {
            try {
                executor.run();
            } finally {
                currentRunners.remove(cmd);
            }
        }

        @Override
        void kill() {
            executor.destroy();
        }

        @Override
        void reject() {
            String errMsg = "Reject cmd " + cmd.getId() + " since over the limit proc of agent";

            procEventHandler.onStarted();
            procEventHandler.onExecuted(-1, Collections.emptyMap());
            procEventHandler.onException(new IllegalStateException(errMsg));

            log.warn(errMsg);
        }
    }
}
