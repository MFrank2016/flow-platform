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

import com.flow.platform.api.domain.v1.Flow;
import com.flow.platform.api.domain.job.JobCategory;
import com.flow.platform.api.domain.v1.JobV1;
import com.flow.platform.api.service.v1.JobNodeManager;
import com.flow.platform.api.service.v1.JobService;
import com.flow.platform.api.test.FlowHelper;
import com.flow.platform.api.test.TestBase;
import com.flow.platform.core.domain.Page;
import com.flow.platform.core.domain.Pageable;
import com.flow.platform.tree.Node;
import com.flow.platform.tree.NodeStatus;
import com.google.common.collect.Lists;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
    private FlowHelper flowHelper;

    @Before
    public void init() {
        setCurrentUser(mockUser);
    }

    @Test
    public void should_create_job() throws Throwable {
        Flow flow = flowHelper.createFlowWithYml("flow-job", "yml/demo_flow2.yaml");
        JobV1 job = jobServiceV1.create(flow, JobCategory.MANUAL, null);
        Assert.assertNotNull(jobDaoV1.get(job.getKey()));
        Assert.assertNotNull(jobTreeDao.get(job.getKey()));

        Node root = jobNodeManager.root(job.getKey());
        Assert.assertEquals(NodeStatus.RUNNING, root.getStatus());
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
