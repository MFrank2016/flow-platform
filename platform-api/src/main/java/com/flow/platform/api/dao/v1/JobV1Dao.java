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

package com.flow.platform.api.dao.v1;

import com.flow.platform.api.domain.job.JobKeyV1;
import com.flow.platform.api.domain.job.JobV1;
import com.flow.platform.core.dao.BaseDao;
import com.flow.platform.core.domain.Pageable;
import java.util.Collection;
import java.util.List;

/**
 * @author yang
 */
public interface JobV1Dao extends BaseDao<JobKeyV1, JobV1> {

    List<JobV1> listLatestByFlows(Collection<String> flows);

    List<JobV1> listByFlow(String flow, Pageable pageable);

    void deleteByFlow(String flow);
}
