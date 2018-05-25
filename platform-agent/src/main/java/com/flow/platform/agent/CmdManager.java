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

import com.flow.platform.cmd.CmdExecutor;
import com.flow.platform.cmd.Log;
import com.flow.platform.cmd.Log.Type;
import com.flow.platform.cmd.ProcListener;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdResult;
import com.flow.platform.domain.CmdStatus;
import com.flow.platform.domain.CmdType;
import com.flow.platform.domain.Jsonable;
import com.flow.platform.tree.YmlEnvs;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import lombok.extern.log4j.Log4j2;

/**
 * Singleton class to handle command
 * <p>
 *
 * @author gy@fir.im
 */
@Log4j2
public class CmdManager {

    private final static CmdManager INSTANCE = new CmdManager();

    public static CmdManager getInstance() {
        return INSTANCE;
    }

    // Make thread to Daemon thread, those threads exit while JVM exist
    private final ThreadFactory defaultFactory = r -> {
        Thread t = Executors.defaultThreadFactory().newThread(r);
        t.setDaemon(true);
        return t;
    };

    // Executor to execute operations
    private ExecutorService defaultExecutor = Executors.newCachedThreadPool(defaultFactory);

    // handle extra listeners
    private List<ProcListener> extraProcEventListeners = new ArrayList<>(5);

    private CmdManager() {
    }

    public List<ProcListener> getExtraProcEventListeners() {
        return extraProcEventListeners;
    }


    /**
     * Execute command from Cmd object by thread executor
     *
     * @param cmd Cmd object
     */
    public void execute(final com.flow.platform.tree.Cmd cmd) {
        if (cmd.getType() == CmdType.RUN_SHELL) {
            // check max concurrent proc

            new TaskRunner(cmd) {
                @Override
                public void run() {
                    log.debug("start cmd ...");

//                    LogEventHandler logListener = new LogEventHandler(getCmd());

                    ProcEventHandler procEventHandler =
                        new ProcEventHandler(getCmd(), extraProcEventListeners);

                    try {
                        CmdExecutor executor = new CmdExecutor(
                            procEventHandler,
                            null,
                            cmd.getContext(),
                            cmd.get(YmlEnvs.WORK_DIR),
                            Collections.EMPTY_LIST, // TODO:  outputEnvFilters
                            Integer.parseInt(cmd.get(YmlEnvs.TIMEOUT)),
                            Lists.newArrayList(getCmd().getContent()));

                        executor.run();
                    } catch (Throwable e) {
                        log.error("Cannot init CmdExecutor for cmd: " + cmd, e);
                        CmdResult result = new CmdResult();
                        result.getExceptions().add(e);
                        procEventHandler.onException(result);
                    }
                }
            }.run();

            return;
        }
    }

    /**
     * collect agent info
     * @return
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
        dic.put("zone", Config.zone());
        dic.put("name", Config.name());
        dic.put("agentVersion", Config.getProperty("version"));

        return Jsonable.GSON_CONFIG.toJson(dic);
    }


    private abstract class TaskRunner implements Runnable {

        private final com.flow.platform.tree.Cmd cmd;

        public TaskRunner(com.flow.platform.tree.Cmd cmd) {
            this.cmd = cmd;
        }

        public com.flow.platform.tree.Cmd getCmd() {
            return cmd;
        }
    }
}
