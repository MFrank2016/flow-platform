/*
 * Copyright 2017 flow.ci
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

package com.flow.platform.api.dao.job;

import com.flow.platform.api.domain.job.JobNumber;
import com.flow.platform.core.dao.AbstractBaseDao;
import org.springframework.stereotype.Repository;

/**
 * @author yang
 */
@Repository
public class JobNumberDaoImpl extends AbstractBaseDao<Long, JobNumber> implements JobNumberDao {

    @Override
    protected Class<JobNumber> getEntityClass() {
        return JobNumber.class;
    }

    @Override
    protected String getKeyName() {
        return "flowId";
    }

    @Override
    public JobNumber increase(final Long flowId) {
        return execute(session -> {
            final String sql = "update JobNumber set number = (number + 1) where flowId = :flowId";
            session.createQuery(sql).setParameter("flowId", flowId).executeUpdate();

            JobNumber number = session.get(getEntityClass(), flowId);
            session.refresh(number);
            return number;
        });
    }
}
