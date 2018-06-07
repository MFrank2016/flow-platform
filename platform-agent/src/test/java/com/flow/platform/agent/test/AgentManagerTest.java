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

import com.flow.platform.agent.AgentManager;
import com.flow.platform.agent.CmdManager;
import com.flow.platform.agent.config.AgentConfig;
import com.flow.platform.agent.mq.RabbitClient;
import com.flow.platform.cmd.AbstractProcListener;
import com.flow.platform.domain.AgentPath;
import com.flow.platform.domain.v1.Cmd;
import com.flow.platform.util.ObjectUtil;
import com.flow.platform.util.zk.ZKClient;
import com.google.common.collect.Lists;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.apache.curator.test.TestingServer;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/**
 * @author gy@fir.im
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AgentManagerTest extends TestBase {

    private static TestingServer server;

    private ZKClient zkClient;

    @BeforeClass
    public static void init() throws Throwable {
        server = new TestingServer(2181);
        server.start();
    }

    @Before
    public void beforeEach() {
        String connectString = server.getConnectString();
        zkClient = new ZKClient(connectString, 1000, 1);
        zkClient.start();
        zkClient.create(AgentPath.ROOT, null);
    }

    @Test
    public void should_agent_registered_and_cmd_executed() throws Throwable {
        // when: start agent
        AgentConfig config = AgentConfig.getInstance();
        AgentManager agent = new AgentManager(config);

        ExecutorService pool = Executors.newFixedThreadPool(1);
        pool.execute(agent);
        Thread.sleep(5000);

        // when:
        AgentPath path = config.getPath();
        Assert.assertTrue(zkClient.exist(path.fullPath()));

        // when: send cmd to queue
        Cmd cmd = new Cmd();
        cmd.setId(Base64.getEncoder().encodeToString("10-0@hello/world".getBytes()));
        cmd.setTimeout(1800);
        cmd.setContent(getResourceContent("test.sh"));
        cmd.setWorkDir("/tmp");
        cmd.setOutputFilter(Lists.newArrayList("FLOW_UT_OUTPUT"));

        CountDownLatch countDown = new CountDownLatch(1);
        Map<String, String> cmdOutput = new HashMap<>();

        CmdManager.getInstance().getExtraProcEventListeners().add(new AbstractProcListener() {
            @Override
            public void onExecuted(int code, Map<String, String> output) {
                cmdOutput.putAll(output);
                countDown.countDown();
            }
        });

        RabbitClient sender = new RabbitClient(config.getQueue().getHost(), config.getCmdQueueName(), null);
        sender.send(cmd);

        // then: verify cmd been executed
        countDown.await(10, TimeUnit.SECONDS);
        Assert.assertEquals(2, cmdOutput.size());
        Assert.assertEquals("11", cmdOutput.get("FLOW_UT_OUTPUT_1"));
        Assert.assertEquals("2", cmdOutput.get("FLOW_UT_OUTPUT_2"));

        // finally
        agent.stop();
        sender.deleteQueue();

        pool.awaitTermination(10, TimeUnit.SECONDS);
        pool.shutdown();
    }

    @After
    public void after() throws Throwable {
        AgentPath path = AgentConfig.getInstance().getPath();
        zkClient.delete(path.fullPath(), true);
        zkClient.close();
    }

    @AfterClass
    public static void done() throws Throwable {
        server.stop();
    }
}
