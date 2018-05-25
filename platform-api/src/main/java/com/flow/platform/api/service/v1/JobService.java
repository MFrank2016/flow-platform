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

import com.flow.platform.api.domain.Flow;
import com.flow.platform.api.domain.job.JobCategory;
import com.flow.platform.domain.v1.JobKey;
import com.flow.platform.api.domain.v1.JobV1;
import com.flow.platform.core.domain.Page;
import com.flow.platform.core.domain.Pageable;
import java.util.List;
import java.util.Map;

/**
 * @author yang
 */
public interface JobService {

    /**
     * Find job by job key
     */
    JobV1 find(JobKey key);

    /**
     * Get yml content for current job
     */
    String jobYml(JobKey key);

    /**
     * List jobs by flows
     */
    Page<JobV1> list(List<String> flows, Pageable pageable);

    /**
     * List latest job for flows
     */
    List<JobV1> listForLatest(List<String> flows);

    /**
     * Create a new job
     */
    JobV1 create(Flow flow, JobCategory eventType, Map<String, String> envs);

    /**
     * Delete job by flow
     */
    void delete(Flow flow);
}
