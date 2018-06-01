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

import com.flow.platform.api.consumer.v1.CmdCallbackConsumer;
import com.flow.platform.api.domain.job.JobCategory;
import com.flow.platform.api.domain.job.JobStatus;
import com.flow.platform.api.domain.v1.Flow;
import com.flow.platform.api.domain.v1.JobV1;
import com.flow.platform.api.events.JobStatusEvent;
import com.flow.platform.api.service.v1.JobNodeManager;
import com.flow.platform.api.service.v1.JobService;
import com.flow.platform.api.test.FlowHelper;
import com.flow.platform.api.test.JobHelper;
import com.flow.platform.api.test.TestBase;
import com.flow.platform.core.context.SpringContext;
import com.flow.platform.core.domain.Page;
import com.flow.platform.core.domain.Pageable;
import com.flow.platform.domain.AgentStatus;
import com.flow.platform.tree.Node;
import com.flow.platform.tree.NodeStatus;
import com.google.common.collect.Lists;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * @author yang
 */
public class JobServiceV1Test extends TestBase {

    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;

    @Autowired
    private JobService jobServiceV1;

    @Autowired
    private JobNodeManager jobNodeManager;

    @Autowired
    private CmdCallbackConsumer cmdCallbackQueueConsumer;

    @Autowired
    private SpringContext springContext;

    @Autowired
    private FlowHelper flowHelper;

    @Autowired
    private JobHelper jobHelper;

    @Before
    public void init() {
        setCurrentUser(mockUser);
    }

    @Test
    public void should_create_job_with_running_status() throws Throwable {
        // init: create idle agent for job
        jobHelper.createAgent("default", "hello", AgentStatus.IDLE);

        CountDownLatch eventCountDown = new CountDownLatch(1);
        springContext.registerApplicationListener((ApplicationListener<JobStatusEvent>) event -> {
            eventCountDown.countDown();
        });

        // when: create job for mock flow
        Flow flow = flowHelper.createFlowWithYml("flow-job", "yml/demo_flow2.yaml");
        JobV1 job = jobServiceV1.create(flow, JobCategory.MANUAL, null);
        eventCountDown.await(10, TimeUnit.SECONDS);

        // then: job node tree root and first node status should be running
        Node root = jobNodeManager.root(job.getKey());
        Assert.assertEquals(NodeStatus.RUNNING, root.getStatus());

        Node firstExecNode = jobNodeManager.next(job.getKey(), root.getPath());
        Assert.assertEquals(NodeStatus.RUNNING, firstExecNode.getStatus());

        // then: job status should be running
        JobV1 loaded = jobServiceV1.find(job.getKey());
        Assert.assertEquals(JobStatus.RUNNING, loaded.getStatus());
    }

    @Test
    public void should_create_job_with_unique_build_number() throws Throwable {
        // given:
        final Flow flow = flowHelper.createFlowWithYml("flow-job-number", "yml/demo_flow2.yaml");
        final int numOfJob = 5;
        final CountDownLatch countDown = new CountDownLatch(numOfJob);

        // when:
        for (int i = 0; i < numOfJob; i++) {
            taskExecutor.execute(() -> {
                // set current user in thread since get it from ThreadLocal
                setCurrentUser(mockUser);

                jobServiceV1.create(flow, JobCategory.MANUAL, null);
                countDown.countDown();
            });
        }

        // then:
        countDown.await(30, TimeUnit.SECONDS);
        Page<JobV1> jobs = jobDaoV1.listByFlow(Lists.newArrayList(flow.getId()), Pageable.DEFAULT);
        Assert.assertEquals(numOfJob, jobs.getPageSize());
    }
}
