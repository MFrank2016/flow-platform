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

package com.flow.platform.api.test.dao;

import com.flow.platform.api.domain.v1.Flow;
import com.flow.platform.api.domain.v1.FlowYml;
import com.flow.platform.api.domain.v1.JobKey;
import com.flow.platform.api.domain.v1.JobV1;
import com.flow.platform.api.test.FlowHelper;
import com.flow.platform.api.test.TestBase;
import com.flow.platform.core.domain.Page;
import com.flow.platform.core.domain.Pageable;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author yh@firim
 */
public class JobDaoTest extends TestBase {

    @Autowired
    private FlowHelper flowHelper;

    private Flow flow;

    @Before
    public void init() throws IOException {
        flow = flowHelper.createFlowWithYml("job_dao_test", "yml/for_job_test.yml");
        Assert.assertNotNull(flow);

        FlowYml flowYml = ymlDao.get(flow.getId());
        Assert.assertNotNull(flowYml);
    }

    @Test
    public void should_create_and_get_job() {
        JobV1 job = createJobs(flow.getId(), 1).get(0);
        JobV1 loaded = jobDaoV1.get(new JobKey(flow.getId(), 1L));
        Assert.assertEquals(job, loaded);
    }

    @Test
    public void should_list_job_by_flow_name() {
        List<JobV1> inits = createJobs(flow.getId(), 2);
        JobV1 first = inits.get(0);
        JobV1 second = inits.get(1);

        Page<JobV1> jobs = jobDaoV1.listByFlow(Lists.newArrayList(flow.getId()), new Pageable(1, 1));
        Assert.assertNotNull(jobs);
        Assert.assertEquals(1, jobs.getPageSize());
        Assert.assertEquals(2, jobs.getTotalSize());
        Assert.assertEquals(first, jobs.getContent().get(0));

        jobs = jobDaoV1.listByFlow(Lists.newArrayList(flow.getId()), new Pageable(2, 1));
        Assert.assertNotNull(jobs);
        Assert.assertEquals(1, jobs.getPageSize());
        Assert.assertEquals(2, jobs.getTotalSize());
        Assert.assertEquals(second, jobs.getContent().get(0));
    }

    @Test
    public void should_list_latest_job_for_flow_names() {
        List<JobV1> jobs = createJobs(flow.getId(), 2);
        Assert.assertEquals(1L, jobs.get(0).getKey().getNumber().longValue());
        Assert.assertEquals(2L, jobs.get(1).getKey().getNumber().longValue());

        List<JobV1> latestJobs = jobDaoV1.listLatestByFlows(Lists.newArrayList(flow.getId()));
        Assert.assertEquals(1, latestJobs.size());
        Assert.assertEquals(2L, latestJobs.get(0).getKey().getNumber().longValue());
    }

    @Test
    public void should_delete_jobs_by_flow_name() {
        createJobs(flow.getId(), 10);
        Assert.assertEquals(10, jobDaoV1.list().size());

        jobDaoV1.deleteByFlow(flow.getId());
        Assert.assertEquals(0, jobDaoV1.list().size());
    }

    private List<JobV1> createJobs(Long flowId, int size) {
        List<JobV1> jobs = new ArrayList<>(size);
        for (int i = 1; i <= size; i++) {
            jobs.add(jobDaoV1.save(new JobV1(flowId, (long) i)));
        }
        return jobs;
    }

}
