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

import com.flow.platform.api.dao.FlowDao;
import com.flow.platform.api.dao.YmlDao;
import com.flow.platform.api.domain.Flow;
import com.flow.platform.api.domain.FlowYml;
import com.flow.platform.api.domain.user.User;
import com.flow.platform.api.envs.EnvUtil;
import com.flow.platform.api.exception.DuplicateExeption;
import com.flow.platform.api.service.CurrentUser;
import com.flow.platform.api.service.user.UserFlowService;
import com.flow.platform.core.exception.NotFoundException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author yang
 */
@Service
public class FlowServiceImpl extends CurrentUser implements FlowService {

    @Autowired
    private FlowDao flowDao;

    @Autowired
    private YmlDao ymlDao;

    @Autowired
    private UserFlowService userFlowService;

    @Override
    @Transactional
    public Flow save(String name) {
        Flow exist = flowDao.get(name);
        if (!Objects.isNull(exist)) {
            throw new DuplicateExeption("The flow name is duplicated");
        }

        User user = currentUser();
        exist = new Flow(name);
        exist.setCreatedBy(user.getEmail());
        userFlowService.assign(user, exist);

        ymlDao.save(new FlowYml(name));
        return flowDao.save(new Flow(name));
    }

    @Override
    public Flow find(String name) {
        Flow exist = flowDao.get(name);
        if (Objects.isNull(exist)) {
            throw new NotFoundException("The flow " + name + " is not found");
        }
        return exist;
    }

    @Override
    public FlowYml findYml(String name) {
        FlowYml yml = ymlDao.get(name);
        if (Objects.isNull(yml)) {
            throw new NotFoundException("The yml of flow " + name + " is not found");
        }
        return yml;
    }

    @Override
    public FlowYml updateYml(String name, String yml) {
        FlowYml flowYml = findYml(name);
        flowYml.setContent(yml);

        // TODO: verify yml
        ymlDao.update(flowYml);
        return flowYml;
    }

    @Override
    @Transactional
    public Flow delete(String name) {
        Flow flow = find(name);

        ymlDao.delete(new FlowYml(name));
        flowDao.delete(flow);

        userFlowService.unAssign(flow);

        return flow;
    }

    @Override
    public List<Flow> list(boolean isOnlyCurrentUser) {
        return null;
    }

    @Override
    public Flow merge(String flowName, Map<String, String> newContext) {
        Flow flow = find(flowName);
        EnvUtil.merge(newContext, flow.getContext(), true);

        flowDao.update(flow);
        return flow;
    }

    @Override
    public Flow remove(String flowName, Set<String> keys) {
        Flow flow = find(flowName);

        for (String key : keys) {
            flow.getContext().remove(key);
        }

        flowDao.update(flow);
        return flow;
    }
}
