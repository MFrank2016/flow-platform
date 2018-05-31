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
import com.flow.platform.api.domain.job.JobCategory;
import com.flow.platform.api.domain.job.JobNumber;
import com.flow.platform.api.domain.v1.Flow;
import com.flow.platform.api.domain.v1.FlowStatus;
import com.flow.platform.api.domain.v1.FlowYml;
import com.flow.platform.api.domain.v1.JobTree;
import com.flow.platform.api.domain.v1.JobV1;
import com.flow.platform.api.envs.EnvUtil;
import com.flow.platform.api.envs.FlowEnvs;
import com.flow.platform.api.envs.JobEnvs;
import com.flow.platform.api.service.CurrentUser;
import com.flow.platform.core.domain.Page;
import com.flow.platform.core.domain.Pageable;
import com.flow.platform.core.exception.IllegalStatusException;
import com.flow.platform.core.exception.NotFoundException;
import com.flow.platform.domain.v1.JobKey;
import com.flow.platform.tree.NodeTree;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author yang
 */
@Service(value = "jobServiceV1")
public class JobServiceImpl extends CurrentUser implements JobService {

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

    @Autowired
    private RabbitTemplate jobQueueTemplate;

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
    public String jobYml(JobKey key) {
        Objects.requireNonNull(key, "JobKey is required");
        JobTree jobTree = jobTreeDao.get(key);
        return jobTree.getTree().toYml();
    }

    @Override
    public Page<JobV1> list(List<String> flows, Pageable pageable) {
        List<Long> ids = flowService.list(flows);
        return jobDaoV1.listByFlow(ids, pageable);
    }

    @Override
    public List<JobV1> listForLatest(List<String> flows) {
        List<Long> ids = flowService.list(flows);
        return jobDaoV1.listLatestByFlows(ids);
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public JobV1 create(Flow flow, JobCategory eventType, Map<String, String> envs) {
        Objects.requireNonNull(flow, "Flow must be defined");
        Objects.requireNonNull(eventType, "Event type must be defined");

        if (flow.getStatus() == FlowStatus.PENDING) {
            throw new IllegalStatusException("Cannot start job since flow not ready");
        }

        // parse yml content to NodeTree
        FlowYml flowYml = flowService.findYml(flow);
        NodeTree tree = NodeTree.create(flowYml.getContent());

        // create job with job number
        JobNumber jobNumber = jobNumberDao.increase(flow.getId());
        JobV1 job = new JobV1(flow.getId(), jobNumber.getNumber());
        job.setCategory(eventType);
        job.setCreatedBy(currentUser().getEmail());
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

        enqueue(job.getKey());
        return job;
    }

    @Override
    public void enqueue(JobKey key) {
        jobQueueTemplate.convertAndSend(key);
    }

    @Override
    @Transactional
    public void delete(Flow flow) {
        Objects.requireNonNull(flow, "Flow must be defined");
        jobDaoV1.deleteByFlow(flow.getId());
        jobTreeDao.deleteByFlow(flow.getId());
    }
}
