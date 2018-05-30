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

package com.flow.platform.api.domain.v1;

import com.flow.platform.api.domain.EnvObject;
import com.flow.platform.api.domain.job.JobCategory;
import com.flow.platform.api.domain.job.JobStatus;
import com.flow.platform.domain.v1.JobKey;
import com.google.gson.annotations.Expose;
import java.time.ZonedDateTime;
import java.util.EnumSet;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @author yang
 */
@NoArgsConstructor
@ToString(of = {"key"})
@EqualsAndHashCode(of = {"key"}, callSuper = false)
public class JobV1 extends EnvObject {

    public final static EnumSet<JobStatus> FINISH_STATUS =
        EnumSet.of(JobStatus.SUCCESS, JobStatus.FAILURE, JobStatus.STOPPED, JobStatus.TIMEOUT);

    @Expose
    @Getter
    @Setter
    private JobKey key;

    @Expose
    @Getter
    @Setter
    private JobCategory category = JobCategory.MANUAL;

    @Expose
    @Getter
    @Setter
    private JobStatus status = JobStatus.CREATED;

    @Expose
    @Getter
    @Setter
    private ZonedDateTime createdAt;

    @Expose
    @Getter
    @Setter
    private ZonedDateTime updatedAt;

    @Expose
    @Getter
    @Setter
    private String createdBy;

    public JobV1(Long flowId, Long number) {
        this.key = new JobKey(flowId, number);
    }

    public boolean isFinishStatus() {
        return FINISH_STATUS.contains(status);
    }

    public Long buildNumber() {
        return key.getNumber();
    }
}
