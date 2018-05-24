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

package com.flow.platform.api.dao;

import com.flow.platform.api.domain.Flow;
import com.flow.platform.core.dao.AbstractBaseDao;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * @author yh@firim
 */
@Repository(value = "flowDao")
public class FlowDaoImpl extends AbstractBaseDao<Long, Flow> implements FlowDao {

    @Override
    protected Class<Flow> getEntityClass() {
        return Flow.class;
    }

    @Override
    protected String getKeyName() {
        return "path";
    }

    @Override
    public Flow get(String name) {
        return execute(session -> session
            .createQuery("from Flow where name = :name", Flow.class)
            .setParameter("name", name)
            .uniqueResult());
    }

    @Override
    public List<Flow> listByCreatedBy(Collection<String> createdBy) {
        return execute(session -> session
            .createQuery("from Flow where createdBy in :createdByList", Flow.class)
            .setParameterList("createdByList", createdBy)
            .list());
    }

    @Override
    public List<Long> listByNames(Collection<String> names) {
        return execute(session -> session
            .createQuery("select id from Flow where name in :names", Long.class)
            .setParameterList("names", names)
            .list());
    }
}
