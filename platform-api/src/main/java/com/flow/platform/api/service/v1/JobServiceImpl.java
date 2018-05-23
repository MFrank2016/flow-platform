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

import com.flow.platform.api.dao.v1.JobDao;
import com.flow.platform.api.domain.Flow;
import com.flow.platform.api.domain.job.JobCategory;
import com.flow.platform.api.domain.user.User;
import com.flow.platform.api.domain.v1.JobKey;
import com.flow.platform.api.domain.v1.JobV1;
import com.flow.platform.core.exception.NotFoundException;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author yang
 */
@Service(value = "jobServiceV1")
public class JobServiceImpl implements JobService {

    @Autowired
    private JobDao jobDaoV1;

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
    public JobV1 create(Flow flow, JobCategory eventType, Map<String, String> envs, User creator) {
        return null;
    }
}
