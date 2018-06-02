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
import com.flow.platform.agent.mq.Pusher;
import com.flow.platform.cmd.CmdExecutor;
import com.flow.platform.cmd.ProcListener;
import com.flow.platform.domain.CmdType;
import com.flow.platform.domain.Jsonable;
import com.flow.platform.domain.v1.Cmd;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

/**
 * Singleton class to handle command
 * <p>
 *
 * @author gy@fir.im
 */
@Log4j2
public class CmdManager {

    private final static CmdManager Instance = new CmdManager();

    static CmdManager getInstance() {
        return Instance;
    }

    // Executor to execute operations
    private ExecutorService executor = Executors.newCachedThreadPool(AgentManager.DEFAULT_THREAD_FACTORY);

    // handle extra listeners
    @Getter
    private final List<ProcListener> extraProcEventListeners = new ArrayList<>(5);

    private final AgentConfig config = AgentConfig.getInstance();

    private final Pusher cmdCallback;

    private CmdManager() {
        this.cmdCallback = initCmdCallback(config.getQueue());
    }

    /**
     * Execute command from Cmd object by thread executor
     *
     * @param cmd Cmd object
     */
    void execute(final Cmd cmd) {
        if (cmd.getType() == CmdType.RUN_SHELL) {
            executor.execute(new ShellCmdRunner(cmd));
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

    private Pusher initCmdCallback(QueueConfig config) {
        String host = config.getHost();
        String callbackQueueName = config.getCallbackQueueName();
        return new Pusher(host, callbackQueueName);
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

        protected final Cmd cmd;

        CmdRunner(Cmd cmd) {
            this.cmd = cmd;
        }

        public Cmd getCmd() {
            return cmd;
        }
    }

    private class ShellCmdRunner extends CmdRunner {

        private final ProcEventHandler procEventHandler;

        private final LogEventHandler logEventHandler;

        ShellCmdRunner(Cmd cmd) {
            super(cmd);
            this.procEventHandler = new ProcEventHandler(cmdCallback, getCmd(), extraProcEventListeners);
            this.logEventHandler = new LogEventHandler(cmd);
        }

        @Override
        public void run() {
            try {
                CmdExecutor executor = new CmdExecutor(
                    procEventHandler,
                    logEventHandler,
                    cmd.getContext(),
                    cmd.getWorkDir(),
                    cmd.getOutputFilter(),
                    cmd.getTimeout(),
                    Lists.newArrayList(getCmd().getContent()));

                executor.run();
            } catch (Throwable e) {
                log.error("Cannot init CmdExecutor for cmd: " + cmd, e);
                procEventHandler.onException(e);
            }
        }
    }
}
