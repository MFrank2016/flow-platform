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
package com.flow.platform.api.service.user;

import com.flow.platform.api.dao.FlowDao;
import com.flow.platform.api.dao.user.UserDao;
import com.flow.platform.api.dao.user.UserFlowDao;
import com.flow.platform.api.domain.Flow;
import com.flow.platform.api.domain.user.User;
import com.flow.platform.api.domain.user.UserFlow;
import com.flow.platform.api.domain.user.UserFlowKey;
import com.flow.platform.api.service.CurrentUser;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author lhl
 */
@Service
@Transactional
public class UserFlowServiceImpl extends CurrentUser implements UserFlowService {

    @Autowired
    private UserFlowDao userFlowDao;

    @Autowired
    private UserDao userDao;

    @Autowired
    private FlowDao flowDao;

    @Override
    public List<User> list(String flowName) {
        Flow flow = flowDao.get(flowName);
        List<String> emails = userFlowDao.listByFlow(flow.getId());
        return emails.isEmpty() ? new ArrayList<>(0) : userDao.list(emails);
    }

    @Override
    public List<Flow> list(User user) {
        List<Long> ids = userFlowDao.listByEmail(user.getEmail());
        return ids.isEmpty() ? new ArrayList<>(0) : flowDao.list(ids);
    }

    @Override
    public void assign(User user, Flow flow) {
        UserFlow userFlow = new UserFlow(flow.getId(), user.getEmail());
        userFlow.setCreatedBy(currentUser().getEmail());
        userFlowDao.save(userFlow);
    }

    @Override
    public void unAssign(User user) {
        userFlowDao.deleteByEmail(user.getEmail());
    }

    @Override
    public void unAssign(Flow flow) {
        userFlowDao.deleteByFlow(flow.getId());
    }

    @Override
    public void unAssign(User user, Flow flow) {
        UserFlow userFlow = userFlowDao.get(new UserFlowKey(flow.getId(), user.getEmail()));
        if (userFlow != null) {
            userFlowDao.delete(userFlow);
        }
    }
}
