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

package com.flow.platform.api.events;

import com.flow.platform.api.domain.v1.JobNodeResult;
import com.flow.platform.api.domain.v1.JobV1;
import java.util.List;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Job node status and result change event
 *
 * @author yang
 */
public class JobNodeEvent extends ApplicationEvent {

    @Getter
    private final JobV1 job;

    @Getter
    private final List<JobNodeResult> details;

    public JobNodeEvent(Object source, JobV1 job, List<JobNodeResult> details) {
        super(source);
        this.job = job;
        this.details = details;
    }
}
