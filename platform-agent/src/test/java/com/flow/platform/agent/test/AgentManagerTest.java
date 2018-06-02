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
import com.flow.platform.agent.config.AgentConfig;
import com.flow.platform.domain.AgentPath;
import com.flow.platform.util.zk.ZKClient;
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
    public void should_agent_registered() throws InterruptedException {
        // when: start agent
        AgentManager agent = new AgentManager(AgentConfig.getInstance());
        ExecutorService pool = Executors.newFixedThreadPool(1);
        pool.execute(agent);

        pool.awaitTermination(10, TimeUnit.SECONDS);
        pool.shutdown();

        // when:
        AgentPath path = AgentConfig.getInstance().getPath();
        Assert.assertTrue(zkClient.exist(path.fullPath()));
        agent.stop();
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
