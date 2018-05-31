/*
 * Copyright 2018 fir.im
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

package com.flow.platform.api.test.service;

import com.flow.platform.api.exception.AgentNotAvailableException;
import com.flow.platform.api.service.v1.AgentManagerService;
import com.flow.platform.api.test.JobHelper;
import com.flow.platform.api.test.TestBase;
import com.flow.platform.domain.Agent;
import com.flow.platform.domain.AgentPath;
import com.flow.platform.domain.AgentStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author yang
 */
public class AgentManagerServiceTest extends TestBase {

    @Autowired
    private JobHelper jobHelper;

    @Autowired
    private AgentManagerService agentManagerService;

    @Before
    public void init() {
        jobHelper.createAgent("default", "first", AgentStatus.IDLE);
        jobHelper.createAgent("default", "second", AgentStatus.BUSY);
    }

    @Test
    public void acquire_idle_agent_with_multi_request() throws InterruptedException {
        // init
        int numOfRequest = 10;
        ExecutorService pool = Executors.newFixedThreadPool(numOfRequest);

        List<Agent> succeeded = new ArrayList<>();
        AtomicInteger numOfFailure = new AtomicInteger(0);
        CountDownLatch countDown = new CountDownLatch(numOfRequest);

        // when: start to execute
        for (int i = 0; i < numOfRequest; i++) {
            pool.execute(() -> {
                try {
                    Agent agent = agentManagerService.acquire();
                    succeeded.add(agent);
                } catch (AgentNotAvailableException e) {
                    numOfFailure.incrementAndGet();
                } finally {
                    countDown.countDown();
                }
            });
        }

        // then: verify the result
        countDown.await(10, TimeUnit.SECONDS);
        Assert.assertEquals(1, succeeded.size());
        Assert.assertEquals(9, numOfFailure.get());

        AgentPath targetPath = new AgentPath("default", "first");
        Assert.assertEquals(targetPath, succeeded.get(0).getPath());
        Assert.assertEquals(AgentStatus.BUSY, succeeded.get(0).getStatus());
    }

    @Test
    public void enable_to_list_agent_with_status() {
        List<Agent> agents = agentManagerService.list();
        Assert.assertEquals(2, agents.size());

        Agent first = agents.get(0);
        Assert.assertEquals("default===first", first.getPath().toString());
        Assert.assertEquals("default===first", agentManagerService.getQueueName(first));
        Assert.assertEquals(AgentStatus.IDLE, first.getStatus());

        Agent second = agents.get(1);
        Assert.assertEquals("default===second", second.getPath().toString());
        Assert.assertEquals("default===second", agentManagerService.getQueueName(second));
        Assert.assertEquals(AgentStatus.BUSY, second.getStatus());
    }

    @Test
    public void release_agent_with_idle_status() {
        AgentPath path = new AgentPath("default", "second");
        Agent second = agentManagerService.find(path);
        Assert.assertEquals(AgentStatus.BUSY, second.getStatus());

        agentManagerService.release(second);
        Assert.assertEquals(AgentStatus.IDLE, agentManagerService.find(path).getStatus());
    }
}
