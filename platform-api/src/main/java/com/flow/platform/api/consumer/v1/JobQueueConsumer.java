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

package com.flow.platform.api.consumer.v1;

import com.flow.platform.api.config.QueueConfig;
import com.flow.platform.api.domain.job.JobStatus;
import com.flow.platform.api.domain.v1.JobKey;
import com.flow.platform.api.domain.v1.JobV1;
import com.flow.platform.api.events.JobStatusEvent;
import com.flow.platform.api.exception.AgentNotAvailableException;
import com.flow.platform.api.service.v1.AgentManagerService;
import com.flow.platform.api.service.v1.JobNodeManager;
import com.flow.platform.api.service.v1.JobService;
import com.flow.platform.core.exception.IllegalStatusException;
import com.flow.platform.core.exception.NotFoundException;
import com.flow.platform.core.service.ApplicationEventService;
import com.flow.platform.domain.Agent;
import com.flow.platform.tree.Node;
import lombok.extern.log4j.Log4j2;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Receive JobKey item from job queue and dispatch to related agent
 *
 * @author yang
 */
@Component
@Log4j2
public class JobQueueConsumer extends ApplicationEventService {

    @Autowired
    private JobService jobServiceV1;

    @Autowired
    private JobNodeManager jobNodeManager;

    @Autowired
    private AgentManagerService agentManagerService;

    /**
     * Receive message from job queue and send related cmd to agent
     */
    @RabbitListener(queues = QueueConfig.JOB_QUEUE_NAME)
    public void handleMessage(JobKey key) {
        log.debug("Job received: {}", key);

        try {
            JobV1 job = getJob(key);
            Agent agent = getAgent();

            Node root = jobNodeManager.root(job.getKey());
            Node next = jobNodeManager.next(key, root.getPath());

            // send cmd to agent queue
            jobNodeManager.execute(key, next.getPath(), agent);

            // set job status to running
            jobServiceV1.setStatus(key, JobStatus.RUNNING);
            this.dispatchEvent(new JobStatusEvent(this, JobStatus.RUNNING));

        } catch (AgentNotAvailableException e) {
            log.warn("Cannot find available agent for job: " + key);
            jobServiceV1.enqueue(key);
        } catch (Throwable e) {
            log.error(e.getMessage());
            jobServiceV1.setStatus(key, JobStatus.FAILURE);
        }
    }

    private JobV1 getJob(JobKey key) throws NotFoundException {
        JobV1 job = jobServiceV1.find(key);
        if (job.isFinishStatus()) {
            throw new IllegalStatusException("The job " + key + " cannot start since its already on finish status");
        }
        return job;
    }

    private Agent getAgent() throws AgentNotAvailableException {
        return agentManagerService.acquire();
    }
}
