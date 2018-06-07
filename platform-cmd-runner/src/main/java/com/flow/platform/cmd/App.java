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

package com.flow.platform.cmd;

import com.google.common.collect.Lists;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Main entrance of cmd runner
 * <p>
 * Created by gy@fir.im on 12/05/2017.
 */
public final class App {

    private final static ProcListener procListener = new ProcListener() {

        @Override
        public void onStarted() {

        }

        @Override
        public void onExecuted(int code, Map<String, String> output) {

        }

        @Override
        public void onException(Throwable e) {

        }
    };

    private final static LogListener logListener = new LogListener() {
        @Override
        public void onLog(Log log) {
            System.out.println(log);
        }

        @Override
        public void onFinish() {

        }
    };

    private final static ThreadFactory defaultFactory = r -> {
        Thread t = Executors.defaultThreadFactory().newThread(r);
        t.setDaemon(false);
        return t;
    };

    private final static ThreadPoolExecutor executor = new ThreadPoolExecutor(
        1,
        1,
        0L,
        TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(),
        defaultFactory,
        (r, executor) -> {
            System.out.println("Rejected !!!");
        });

    public static void main(String[] args) throws Throwable {
        Map<String, String> inputs = new HashMap<>();
        inputs.put("TEST", "echo \"hello\"");
        inputs.put("FLOW_INPUT", "HELLO");

        CmdExecutor executor = new CmdExecutor(
            procListener,
            logListener,
            inputs,
            null,
            Lists.newArrayList("FLOW_"), // find env start with FLOW_ and put to cmd result output map
            null,
            Lists.newArrayList("docker ps"));

        executor.run();
    }

    private static class MyThread implements Runnable {

        @Override
        public void run() {
            CmdExecutor executor = new CmdExecutor(
                procListener,
                logListener,
                null,
                null,
                null,
                null,
                Lists.newArrayList("sleep 10 && echo \"hello\""));

            executor.run();
        }
    }
}
