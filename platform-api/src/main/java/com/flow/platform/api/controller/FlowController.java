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

package com.flow.platform.api.controller;

import com.flow.platform.api.domain.permission.Actions;
import com.flow.platform.api.domain.request.TriggerParam;
import com.flow.platform.api.domain.v1.Flow;
import com.flow.platform.api.domain.v1.FlowStatus;
import com.flow.platform.api.security.WebSecurity;
import com.flow.platform.api.service.GitService;
import com.flow.platform.api.service.v1.FlowService;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author yh@firim
 */

@RestController
@RequestMapping(path = "/flows")
public class FlowController extends NodeController {

    @Autowired
    private GitService gitService;

    @Autowired
    private FlowService flowService;

    @GetMapping
    @WebSecurity(action = Actions.FLOW_SHOW)
    public List<Flow> index() {
        return flowService.list(true);
    }

    @GetMapping(path = {"/{root}", "/{root}/show"})
    @WebSecurity(action = Actions.FLOW_SHOW)
    public Flow show() {
        return flowService.find(flowName.get());
    }

    @PostMapping(path = {"/{root}", "/{root}/create"})
    @WebSecurity(action = Actions.FLOW_CREATE)
    public Flow create() {
        Flow created = flowService.save(flowName.get());
        return created;
    }

    @PatchMapping(path = "/{root}/status/{status}")
    public Flow changeStatus(@PathVariable FlowStatus status) {
        Flow flow = flowService.find(flowName.get());
        return flowService.changeStatus(flow, status);
    }

    @DeleteMapping(path = "/{root}")
    @WebSecurity(action = Actions.FLOW_DELETE)
    public Flow delete() {
        return flowService.delete(flowName.get());
    }

    @PatchMapping("/{root}/env")
    @WebSecurity(action = Actions.FLOW_SET_ENV)
    public Flow putContext(@RequestBody Map<String, String> envs) {
        return flowService.merge(flowName.get(), envs);
    }

    @DeleteMapping("/{root}/env")
    @WebSecurity(action = Actions.FLOW_SET_ENV)
    public Flow removeContext(@RequestBody Set<String> envKeys) {
        return flowService.remove(flowName.get(), envKeys);
    }

    @GetMapping("/{root}/env")
    public Map<String, String> getContext() {
        return flowService.find(flowName.get()).getEnvs();
    }

    @GetMapping("/{root}/branches")
    public List<String> listBranches(@RequestParam(required = false, defaultValue = "false") Boolean refresh) {
        Flow flow = flowService.find(flowName.get());
        return gitService.branches(flow, refresh);
    }

    @GetMapping("/{root}/tags")
    public List<String> listTags() {
        Flow flow = flowService.find(flowName.get());
        return gitService.tags(flow, false);
    }

    @PatchMapping("/{root}/trigger")
    public Flow trigger(@RequestBody TriggerParam triggerParam) {
        return flowService.merge(flowName.get(), triggerParam.toEnv());
    }

    @PostMapping("/{root}/git/try")
    public void tryToConnectGit(@RequestBody(required = false) Map<String, String> envs) {

    }
}
