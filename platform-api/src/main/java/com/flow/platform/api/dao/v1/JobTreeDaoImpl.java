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

import com.flow.platform.api.domain.v1.JobKey;
import com.flow.platform.api.domain.v1.JobTree;
import com.flow.platform.core.dao.AbstractBaseDao;
import org.springframework.stereotype.Repository;

/**
 * @author yang
 */
@Repository
public class JobTreeDaoImpl extends AbstractBaseDao<JobKey, JobTree> implements JobTreeDao {

    @Override
    protected Class<JobTree> getEntityClass() {
        return JobTree.class;
    }

    @Override
    protected String getKeyName() {
        return "key";
    }
}
