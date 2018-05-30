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

package com.flow.platform.api.test.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.flow.platform.api.domain.job.Job;
import com.flow.platform.api.domain.job.JobCategory;
import com.flow.platform.api.domain.v1.Flow;
import com.flow.platform.api.domain.v1.JobV1;
import com.flow.platform.api.envs.GitEnvs;
import com.flow.platform.api.service.v1.JobNodeManager;
import com.flow.platform.api.service.v1.JobService;
import com.flow.platform.api.test.FlowHelper;
import com.flow.platform.tree.NodeStatus;
import java.nio.file.Path;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/**
 * @author yh@firim
 */
public class JobControllerTest extends ControllerTestWithoutAuth {

    @Autowired
    private FlowHelper flowHelper;

    @Autowired
    private JobService jobServiceV1;

    @Autowired
    private JobNodeManager jobNodeManager;

    @Test
    public void should_show_job_success() throws Exception {
        stubDemo();
        Flow rootForFlow = flowHelper.createFlowWithYml("flow1", "yml/flow.yaml");
        JobV1 job = jobServiceV1.create(rootForFlow, JobCategory.MANUAL, null);

        job.putEnv(GitEnvs.FLOW_GIT_BRANCH, "master");
        jobDaoV1.update(job);

        JobV1 returnedJob = requestToShowJob(rootForFlow.getName(), job.buildNumber());
        Assert.assertEquals(job, returnedJob);

        // when: load yml
        String ymlForJob = requestToGetYml(rootForFlow.getName(), job.buildNumber());
        String originYml = getResourceContent("yml/flow.yaml");
        Assert.assertEquals(originYml, ymlForJob);
    }

    @Test
    public void should_stop_job_success() throws Exception {
        stubDemo();
        Flow rootForFlow = flowHelper.createFlowWithYml("flow1", "yml/flow.yaml");
        JobV1 job = jobServiceV1.create(rootForFlow, JobCategory.TAG, null);
        job.putEnv(GitEnvs.FLOW_GIT_BRANCH, "master");
        jobDaoV1.update(job);

        this.mockMvc.perform(post(String.format("/jobs/%s/%s/stop", rootForFlow.getName(), job.buildNumber()))
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk()).andReturn();

        JobV1 loadedJob = requestToShowJob(rootForFlow.getName(), job.buildNumber());
        Assert.assertEquals(NodeStatus.KILLED, jobNodeManager.root(loadedJob.getKey()).getStatus());
    }

    @Test
    public void should_get_step_log_success() throws Exception {
        stubDemo();
        Flow rootForFlow = flowHelper.createFlowWithYml("flow1", "yml/flow.yaml");
        JobV1 job = jobServiceV1.create(rootForFlow, JobCategory.TAG, null);
        job.putEnv(GitEnvs.FLOW_GIT_BRANCH, "master");
        jobDaoV1.update(job);

        String response = performRequestWith200Status(
            get(String.format("/jobs/%s/%s/1/log", rootForFlow.getName(), job.buildNumber())));
        Assert.assertNotNull(response);
    }

    @Test
    public void should_get_job_zip_error() throws Exception {
        stubDemo();
        Flow rootForFlow = flowHelper.createFlowWithYml("flow1", "yml/flow.yaml");
        Job job = jobService.create(rootForFlow, JobCategory.TAG, null, mockUser);

        job.putEnv(GitEnvs.FLOW_GIT_BRANCH, "master");
        jobDao.update(job);
        MvcResult mvcResult = this.mockMvc.perform(
            get(String.format("/jobs/%s/%s/log/download", job.getNodeName(), job.getNumber()))
        ).andExpect(status().isBadRequest()).andReturn();
        String response = mvcResult.getResponse().getContentAsString();
        Assert.assertNotNull(response);
    }

    private JobV1 requestToShowJob(String path, Long buildNumber) throws Exception {
        String response = performRequestWith200Status(get(String.format("/jobs/%s/%s", path, buildNumber))
            .contentType(MediaType.APPLICATION_JSON));

        return JobV1.parse(response, JobV1.class);
    }

    private String requestToGetYml(String path, Long buildNumber) throws Exception {
        return performRequestWith200Status(get(String.format("/jobs/%s/%s/yml", path, buildNumber)));
    }
}
