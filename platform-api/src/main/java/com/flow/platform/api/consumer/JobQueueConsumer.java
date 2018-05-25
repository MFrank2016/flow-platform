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

package com.flow.platform.api.consumer;

import com.flow.platform.api.domain.v1.JobV1;
import com.flow.platform.api.service.v1.AgentManagerService;
import com.flow.platform.api.service.v1.JobService;
import com.flow.platform.core.exception.NotFoundException;
import com.flow.platform.domain.v1.JobKey;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author yang
 */
@Component
@Log4j2
public class JobQueueConsumer {

    @Autowired
    private JobService jobServiceV1;

    @Autowired
    private AgentManagerService agentManagerService;

    public void handleMessage(JobKey key) {
        log.debug("Job received: {}", key);

        try {
            JobV1 job = jobServiceV1.find(key);
            if (job.isFinishStatus()) {
                log.warn("The job {} cannot start since its already on finish status", key);
                return;
            }
            agentManagerService.handleJob(job.getKey());
        } catch (NotFoundException e) {
            log.warn("The job '{}' from queue been deleted", key);
        }
    }
}
