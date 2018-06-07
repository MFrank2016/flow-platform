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

package com.flow.platform.agent.test;

import com.flow.platform.agent.CmdManager;
import com.flow.platform.agent.config.AgentConfig;
import com.flow.platform.cmd.AbstractProcListener;
import com.flow.platform.domain.CmdType;
import com.flow.platform.domain.v1.Cmd;
import com.google.common.collect.Lists;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.curator.shaded.com.google.common.io.Files;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/**
 * @author gy@fir.im
 */
@FixMethodOrder(value = MethodSorters.NAME_ASCENDING)
public class CmdManagerTest extends TestBase {

    private CmdManager cmdManager = CmdManager.getInstance();

    private String script;

    @Before
    public void init() throws IOException {
        script = getResourceContent("test.sh");
        cmdManager.getExtraProcEventListeners().clear();
    }

    @Test
    public void should_be_singleton() {
        Assert.assertEquals(cmdManager, CmdManager.getInstance());
    }

    @Test
    public void should_be_executed_from_cmd_manager() throws Throwable {
        Cmd cmd = new Cmd();
        cmd.setType(CmdType.RUN_SHELL);
        cmd.setContent(script);
        cmd.setId(UUID.randomUUID().toString());
        cmd.setOutputFilter(Lists.newArrayList("FLOW_UT_OUTPUT"));

        AtomicInteger sizeOfOutput = new AtomicInteger(0);
        cmdManager.getExtraProcEventListeners().add(new AbstractProcListener() {
            @Override
            public void onExecuted(int code, Map<String, String> output) {
                sizeOfOutput.set(output.size());
            }
        });

        cmdManager.execute(cmd);
        Assert.assertEquals(1, cmdManager.getCurrentRunners().size());

        ExecutorService cmdExecutor = cmdManager.getCmdExecutor();
        cmdExecutor.shutdown();
        cmdExecutor.awaitTermination(60, TimeUnit.SECONDS);

        AgentConfig config = AgentConfig.getInstance();
        String logDir = config.getLogDir().toString();

        Assert.assertEquals(0, cmdManager.getCurrentRunners().size());
        Assert.assertEquals(2, sizeOfOutput.get());
        Assert.assertTrue(java.nio.file.Files.exists(Paths.get(logDir, cmd.getId() + ".out.zip")));
    }

    @Test
    public void should_be_correct_status_for_killed_process() throws Throwable {
        // given
        Cmd cmd = new Cmd();
        cmd.setId(UUID.randomUUID().toString());
        cmd.setContent(script);
        cmd.setType(CmdType.RUN_SHELL);

        CountDownLatch startLatch = new CountDownLatch(1);

        cmdManager.getExtraProcEventListeners().add(new AbstractProcListener() {
            @Override
            public void onStarted() {
                startLatch.countDown();
            }
        });

        // when: start and kill task immediately
        cmdManager.execute(cmd);
        startLatch.await(30, TimeUnit.SECONDS);
        Thread.sleep(2);

        cmdManager.kill();
        Assert.assertEquals(0, cmdManager.getCurrentRunners().size());
    }
//
//
//    @Test
//    public void should_success_run_sys_cmd() throws InterruptedException {
//        String content = String.format("source %s", resourcePath);
//        Cmd cmd = new Cmd("zone1", "agent1", CmdType.RUN_SHELL, content);
//        cmd.setId(UUID.randomUUID().toString());
//        CountDownLatch finishCountDownLatch = new CountDownLatch(1);
//        CountDownLatch startCountDownLatch = new CountDownLatch(1);
//
//        cmdManager.getExtraProcEventListeners().add(new ProcListener() {
//            @Override
//            public void onStarted(CmdResult result) {
//                startCountDownLatch.countDown();
//                try {
//                    Thread.sleep(3000);
//                } catch (Throwable e) {
//                }
//            }
//
//            @Override
//            public void onLogged(CmdResult result) {
//                finishCountDownLatch.countDown();
//            }
//
//            @Override
//            public void onExecuted(CmdResult result) {
//
//            }
//
//            @Override
//            public void onException(CmdResult result) {
//
//            }
//        });
//
//        // when: start and kill task immediately
//        cmdManager.execute(cmd);
//
//        startCountDownLatch.await();
//        Assert.assertEquals(1, cmdManager.getRunning().size());
//
//        Cmd cmdSys = new Cmd("zone1", "agent1", CmdType.SYSTEM_INFO, "");
//        cmdSys.setId(UUID.randomUUID().toString());
//
//        cmdManager.execute(cmdSys);
//        Assert.assertEquals(CmdStatus.EXECUTED, cmdSys.getStatus());
//
//        finishCountDownLatch.await();
//        Assert.assertEquals(1, cmdManager.getFinished().size());
//    }
}
