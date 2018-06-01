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

package com.flow.platform.api.test.integration;

import com.flow.platform.api.consumer.v1.CmdCallbackConsumer;
import com.flow.platform.api.domain.job.JobCategory;
import com.flow.platform.api.domain.job.JobStatus;
import com.flow.platform.api.domain.v1.Flow;
import com.flow.platform.api.domain.v1.JobV1;
import com.flow.platform.api.events.CmdSentEvent;
import com.flow.platform.api.events.JobStatusEvent;
import com.flow.platform.api.service.v1.AgentManagerService;
import com.flow.platform.api.service.v1.CmdManager;
import com.flow.platform.api.service.v1.JobNodeManager;
import com.flow.platform.api.service.v1.JobService;
import com.flow.platform.api.test.FlowHelper;
import com.flow.platform.api.test.JobHelper;
import com.flow.platform.api.test.TestBase;
import com.flow.platform.core.context.SpringContext;
import com.flow.platform.domain.Agent;
import com.flow.platform.domain.AgentStatus;
import com.flow.platform.domain.CmdStatus;
import com.flow.platform.domain.v1.ExecutedCmd;
import com.flow.platform.tree.Node;
import com.flow.platform.tree.NodePath;
import com.flow.platform.tree.NodeStatus;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;

/**
 * Test whole job - cmd communication
 *
 * @author yang
 */
public class JobIntegrationTest extends TestBase {

    @Autowired
    private JobService jobServiceV1;

    @Autowired
    private JobNodeManager jobNodeManager;

    @Autowired
    private CmdCallbackConsumer cmdCallbackConsumer;

    @Autowired
    private AgentManagerService agentManagerService;

    @Autowired
    private SpringContext springContext;

    @Autowired
    private JobHelper jobHelper;

    @Autowired
    private FlowHelper flowHelper;

    private Flow flow;

    private JobV1 job;

    private Agent agent;

    @Before
    public void init() throws Throwable {
        flow = flowHelper.createFlowWithYml("flow-job", "yml/job_integration.yml");
        agent = jobHelper.createAgent("default", "nice", AgentStatus.IDLE);

        CountDownLatch eventCountDown = new CountDownLatch(1);
        springContext.registerApplicationListener((ApplicationListener<JobStatusEvent>) event -> {
            eventCountDown.countDown();
        });

        job = jobServiceV1.create(flow, JobCategory.MANUAL, null);
        eventCountDown.await(10, TimeUnit.SECONDS);
    }

    @Test
    public void should_update_job_node_by_executed_cmd() throws InterruptedException {
        // when: mock step 1 callback
        CountDownLatch cmdCountDown = createCountDownForCmdEvent(NodePath.create("root", "step2"));
        ExecutedCmd executed = createExecutedCmd(0, CmdStatus.EXECUTED, NodePath.create("root", "step1"));
        cmdCallbackConsumer.handleMessage(executed);

        // then: check job and node status for step 1
        JobV1 loadedJob  = jobServiceV1.find(job.getKey());
        Assert.assertEquals(JobStatus.RUNNING, loadedJob.getStatus());

        Node root = jobNodeManager.root(job.getKey());
        Assert.assertEquals(NodeStatus.RUNNING, root.getStatus());

        Node node = jobNodeManager.get(job.getKey(), NodePath.create("root", "step1"));
        Assert.assertEquals(NodeStatus.SUCCESS, node.getStatus());

        cmdCountDown.await(10, TimeUnit.SECONDS);
        Assert.assertEquals(0, cmdCountDown.getCount());

        // when: mock step 2 callback with error code but allow failure
        cmdCountDown = createCountDownForCmdEvent(NodePath.create("root", "step3"));
        executed = createExecutedCmd(1, CmdStatus.EXECUTED, NodePath.create("root", "step2"));
        cmdCallbackConsumer.handleMessage(executed);

        // then: check job and node status for step 2
        loadedJob  = jobServiceV1.find(job.getKey());
        Assert.assertEquals(JobStatus.RUNNING, loadedJob.getStatus());

        root = jobNodeManager.root(job.getKey());
        Assert.assertEquals(NodeStatus.RUNNING, root.getStatus());

        node = jobNodeManager.get(job.getKey(), NodePath.create("root", "step2"));
        Assert.assertTrue(node.isAllowFailure());
        Assert.assertEquals(NodeStatus.SUCCESS, node.getStatus());

        cmdCountDown.await(10, TimeUnit.SECONDS);
        Assert.assertEquals(0, cmdCountDown.getCount());

        // when: mock step 3 callback
        executed = createExecutedCmd(0, CmdStatus.EXECUTED, NodePath.create("root", "step3"));
        cmdCallbackConsumer.handleMessage(executed);

        CountDownLatch jobFinishCountDown = new CountDownLatch(1);
        springContext.registerApplicationListener((ApplicationListener<JobStatusEvent>) event -> {
            jobFinishCountDown.countDown();
        });

        // then:
        jobFinishCountDown.await(10, TimeUnit.SECONDS);
        Assert.assertEquals(AgentStatus.IDLE, agentManagerService.find(agent.getToken()).getStatus());
        Assert.assertEquals(NodeStatus.SUCCESS, jobNodeManager.root(job.getKey()).getStatus());
        Assert.assertEquals(JobStatus.SUCCESS, jobServiceV1.find(job.getKey()).getStatus());
    }

    private ExecutedCmd createExecutedCmd(int code, CmdStatus status, NodePath nodePath) {
        ExecutedCmd executed = new ExecutedCmd();
        executed.setCode(code);
        executed.setDuration(10L);
        executed.setStatus(status);
        executed.getMeta().put(CmdManager.META_JOB_KEY, job.getKey().getId());
        executed.getMeta().put(CmdManager.META_JOB_NODE_PATH, nodePath.toString());
        executed.getMeta().put(CmdManager.META_AGENT_TOKEN, agent.getToken());
        return executed;
    }

    private CountDownLatch createCountDownForCmdEvent(NodePath targetPath) {
        CountDownLatch cmdCountDown = new CountDownLatch(1);
        springContext.registerApplicationListener((ApplicationListener<CmdSentEvent>) event -> {
            NodePath nodePath = NodePath.create(event.getCmd().getMeta().get(CmdManager.META_JOB_NODE_PATH));
            if (nodePath.equals(targetPath)) {
                cmdCountDown.countDown();
            }
        });
        return cmdCountDown;
    }
}
