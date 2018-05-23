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

package com.flow.platform.api.service.v1;

import com.flow.platform.api.dao.job.JobNumberDao;
import com.flow.platform.api.dao.v1.JobDao;
import com.flow.platform.api.dao.v1.JobTreeDao;
import com.flow.platform.api.domain.Flow;
import com.flow.platform.api.domain.FlowYml;
import com.flow.platform.api.domain.job.JobCategory;
import com.flow.platform.api.domain.job.JobNumber;
import com.flow.platform.api.domain.user.User;
import com.flow.platform.api.domain.v1.JobKey;
import com.flow.platform.api.domain.v1.JobTree;
import com.flow.platform.api.domain.v1.JobV1;
import com.flow.platform.api.envs.EnvUtil;
import com.flow.platform.api.envs.FlowEnvs;
import com.flow.platform.api.envs.JobEnvs;
import com.flow.platform.core.exception.NotFoundException;
import com.flow.platform.tree.NodeTree;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author yang
 */
@Service(value = "jobServiceV1")
public class JobServiceImpl implements JobService {

    @Value(value = "${domain.api}")
    private String apiDomain;

    @Autowired
    private JobDao jobDaoV1;

    @Autowired
    private JobTreeDao jobTreeDao;

    @Autowired
    private JobNumberDao jobNumberDao;

    @Autowired
    private FlowService flowService;

    @Override
    public JobV1 find(JobKey key) {
        Objects.requireNonNull(key, "JobKey is required");
        JobV1 job = jobDaoV1.get(key);

        if (Objects.isNull(job)) {
            throw new NotFoundException("Job not found for: " + key.toString());
        }

        return job;
    }

    @Override
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public JobV1 create(Flow flow, JobCategory eventType, Map<String, String> envs, User creator) {
        Objects.requireNonNull(flow, "Flow must be defined");
        Objects.requireNonNull(eventType, "Event type must be defined");
        Objects.requireNonNull(creator, "User must be defined while create job");

        // parse yml content to NodeTree
        FlowYml flowYml = flowService.findYml(flow.getName());
        NodeTree tree = NodeTree.create(flowYml.getContent());

        // create job with job number
        JobNumber jobNumber = jobNumberDao.increase(flow.getName());
        JobV1 job = new JobV1(flow.getName(), jobNumber.getNumber());
        job.setCategory(eventType);
        job.setCreatedBy(creator.getEmail());
        job.setCreatedAt(ZonedDateTime.now());
        job.setUpdatedAt(ZonedDateTime.now());

        // init default environment variables
        job.putEnv(FlowEnvs.FLOW_NAME, flow.getName());
        job.putEnv(JobEnvs.FLOW_JOB_BUILD_CATEGORY, eventType.name());
        job.putEnv(JobEnvs.FLOW_JOB_BUILD_NUMBER, job.getKey().getNumber().toString());
        job.putEnv(JobEnvs.FLOW_API_DOMAIN, apiDomain);

        // merge flow and customized env variables to job env variable
        EnvUtil.merge(flow.getEnvs(), job.getEnvs(), true);
        EnvUtil.merge(envs, job.getEnvs(), true);

        // persistent job and job tree
        jobDaoV1.save(job);
        jobTreeDao.save(new JobTree(job.getKey(), tree));

        // TODO: send job request to queue

        return job;
    }
}
