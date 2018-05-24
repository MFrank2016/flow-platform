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
import com.flow.platform.api.dao.job.JobNumberDao;
import com.flow.platform.api.domain.Flow;
import com.flow.platform.api.domain.FlowYml;
import com.flow.platform.api.domain.job.JobNumber;
import com.flow.platform.api.domain.user.Role;
import com.flow.platform.api.domain.user.SysRole;
import com.flow.platform.api.domain.user.User;
import com.flow.platform.api.envs.EnvUtil;
import com.flow.platform.api.envs.GitEnvs;
import com.flow.platform.api.exception.DuplicateExeption;
import com.flow.platform.api.service.CurrentUser;
import com.flow.platform.api.service.user.RoleService;
import com.flow.platform.api.service.user.UserFlowService;
import com.flow.platform.core.exception.NotFoundException;
import com.flow.platform.util.http.HttpURL;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author yang
 */
@Service
public class FlowServiceImpl extends CurrentUser implements FlowService {

    @Value(value = "${domain.api}")
    private String apiDomain;

    @Autowired
    private FlowDao flowDao;

    @Autowired
    private YmlDao ymlDao;

    @Autowired
    private JobNumberDao jobNumberDao;

    @Autowired
    private UserFlowService userFlowService;

    @Autowired
    private RoleService roleService;

    @Override
    @Transactional
    public Flow save(String name) {
        Flow flow = flowDao.get(name);
        if (!Objects.isNull(flow)) {
            throw new DuplicateExeption("The flow name is duplicated");
        }

        // save flow get auto increased flow id
        flow = flowDao.save(new Flow(name));
        flow.putEnv(GitEnvs.FLOW_GIT_WEBHOOK, hooksUrl(flow));
        flow.putEnv(GitEnvs.FLOW_GIT_BRANCH, GitEnvs.DEFAULT_BRANCH);

        User user = currentUser();
        flow.setCreatedBy(user.getEmail());
        userFlowService.assign(user, flow);

        ymlDao.save(new FlowYml(flow));
        jobNumberDao.save(new JobNumber(flow));

        return flow;
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
    public FlowYml findYml(Flow flow) {
        FlowYml yml = ymlDao.get(flow.getId());
        if (Objects.isNull(yml)) {
            throw new NotFoundException("The yml of flow " + flow.getName() + " is not found");
        }
        return yml;
    }

    @Override
    public FlowYml updateYml(Flow flow, String yml) {
        FlowYml flowYml = findYml(flow);
        flowYml.setContent(yml);

        // TODO: verify yml
        ymlDao.update(flowYml);
        return flowYml;
    }

    @Override
    @Transactional
    public Flow delete(String name) {
        Flow flow = find(name);

        ymlDao.delete(new FlowYml(flow));
        flowDao.delete(flow);
        jobNumberDao.delete(new JobNumber(flow));

        userFlowService.unAssign(flow);
        return flow;
    }

    @Override
    public List<Flow> list(boolean isOnlyCurrentUser) {
        if (!isOnlyCurrentUser) {
            return flowDao.list();
        }

        List<Role> roles = roleService.list(currentUser());

        if (roles.contains(roleService.find(SysRole.ADMIN.name()))) {
            return flowDao.list();
        }

        return userFlowService.list(currentUser());
    }

    @Override
    public List<Long> list(Collection<String> names) {
        return flowDao.listByNames(names);
    }

    @Override
    public Flow merge(String flowName, Map<String, String> newContext) {
        Flow flow = find(flowName);
        EnvUtil.merge(newContext, flow.getEnvs(), true);

        flowDao.update(flow);
        return flow;
    }

    @Override
    public Flow remove(String flowName, Set<String> keys) {
        Flow flow = find(flowName);

        for (String key : keys) {
            flow.removeEnv(key);
        }

        flowDao.update(flow);
        return flow;
    }

    private String hooksUrl(final Flow flow) {
        return HttpURL.build(apiDomain).append("/hooks/git/").append(flow.getName()).toString();
    }
}
