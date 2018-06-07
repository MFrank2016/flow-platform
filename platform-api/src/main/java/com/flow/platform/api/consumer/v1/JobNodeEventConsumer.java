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

import com.flow.platform.api.config.WebSocketConfig;
import com.flow.platform.api.domain.v1.JobNodeResult;
import com.flow.platform.api.domain.v1.JobV1;
import com.flow.platform.api.events.JobNodeEvent;
import com.flow.platform.api.message.PushHandler;
import com.flow.platform.domain.Jsonable;
import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.ApplicationListener;

/**
 * @author yang
 */
@Log4j2
public class JobNodeEventConsumer extends PushHandler implements ApplicationListener<JobNodeEvent> {

    class JobDetailMessage extends Jsonable {

        JobV1 job;

        List<JobNodeResult> details;

        JobDetailMessage(JobV1 job, List<JobNodeResult> details) {
            this.job = job;
            this.details = details;
        }
    }

    @Override
    public void onApplicationEvent(JobNodeEvent event) {
        JobDetailMessage message = new JobDetailMessage(event.getJob(), event.getDetails());
        this.push(WebSocketConfig.TOPIC_FOR_JOB_DETAIL, message);
    }
}
