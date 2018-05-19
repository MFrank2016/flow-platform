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

import com.flow.platform.api.domain.Flow;
import com.flow.platform.api.domain.FlowYml;
import com.flow.platform.api.domain.node.Node;
import com.flow.platform.api.domain.node.Yml;
import com.flow.platform.api.domain.permission.Actions;
import com.flow.platform.api.domain.request.ListParam;
import com.flow.platform.api.domain.request.TriggerParam;
import com.flow.platform.api.domain.response.BooleanValue;
import com.flow.platform.api.domain.user.User;
import com.flow.platform.api.envs.EnvUtil;
import com.flow.platform.api.security.WebSecurity;
import com.flow.platform.api.service.GitService;
import com.flow.platform.api.service.node.YmlService;
import com.flow.platform.api.service.v1.FlowService;
import com.flow.platform.core.exception.IllegalParameterException;
import com.flow.platform.util.StringUtil;
import com.google.common.base.Strings;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
    private YmlService ymlService;

    @Autowired
    private GitService gitService;

    @Autowired
    private FlowService flowService;

    @GetMapping
    @WebSecurity(action = Actions.FLOW_SHOW)
    public List<Flow> index() {
        return flowService.list(true);
    }

    /**
     * @api {get} /flows/:root Show
     * @apiParam {String} root flow node name
     * @apiGroup Flows
     *
     * @apiSuccessExample {json} Success-Response
     *  {
     *      path: /flow-name,
     *      name: flow-name,
     *      createdAt: 15123123
     *      updatedAt: 15123123
     *      envs: {
     *          FLOW_ENV_1: xxxx,
     *          FLOW_ENV_2: xxxx
     *      }
     *  }
     */
    @GetMapping(path = {"/{root}", "/{root}/show"})
    @WebSecurity(action = Actions.FLOW_SHOW)
    public Flow show() {
        return flowService.find(flowName.get());
    }

    /**
     * @api {post} /flows/:root Create
     * @apiParam {String} root flow node name will be created
     * @apiDescription Create empty flow node with default env variables
     * @apiGroup Flows
     *
     * @apiSuccessExample {json} Success-Response
     *  {
     *      path: /flow-name,
     *      name: flow-name,
     *      createdAt: 15123123
     *      updatedAt: 15123123
     *      envs: {
     *      }
     *  }
     */
    @PostMapping(path = {"/{root}", "/{root}/create"})
    @WebSecurity(action = Actions.FLOW_CREATE)
    public Flow create() {
        Flow created = flowService.save(flowName.get());
        return created;
    }

    /**
     * @api {delete} /flows/:root Delete
     * @apiParam {String} root flow node name will be deleted
     * @apiDescription Delete flow node by name and return flow node object
     * @apiGroup Flows
     *
     * @apiSuccessExample {json} Success-Response
     *  {
     *      path: /flow-name,
     *      name: flow-name,
     *      createdAt: 15123123
     *      updatedAt: 15123123
     *      envs: {
     *          FLOW_ENV_VAR_1: xxx,
     *          FLOW_ENV_VAR_2: xxx
     *      }
     *  }
     */
    @DeleteMapping(path = "/{root}")
    @WebSecurity(action = Actions.FLOW_DELETE)
    public Flow delete() {
        return flowService.delete(flowName.get());
    }

    /**
     * @api {post} /flows/:root/env Add Env Variables
     * @apiParam {String} root flow node name will be set env variables
     * @apiParam {Boolean} [verify=false] enable to verify env varaible
     * @apiParamExample {json} Request-Body:
     *  {
     *      FLOW_ENV_VAR_2: xxx,
     *      FLOW_ENV_VAR_1: xxx
     *  }
     * @apiGroup Flows
     * @apiDescription Add env variables to flow env variables, overwrite if env existed
     *
     * @apiSuccessExample {json} Success-Response
     *  {
     *      path: /flow-name,
     *      name: flow-name,
     *      createdAt: 15123123
     *      updatedAt: 15123123
     *      envs: {
     *          FLOW_ENV_VAR_1: xxx,
     *          FLOW_ENV_VAR_2: xxx
     *      }
     *  }
     */
    @PostMapping("/{root}/env")
    @WebSecurity(action = Actions.FLOW_SET_ENV)
    public Flow putContext(@RequestBody Map<String, String> envs) {
        return flowService.merge(flowName.get(), envs);
    }

    /**
     * @api {delete} /flows/:root/env Del Env Variables
     * @apiParam {String} root flow node name will be set env variables
     * @apiParam {Boolean} [verify=false] enable to verify env varaible
     * @apiParamExample {json} Request-Body:
     *  [
     *      FLOW_ENV_VAR_2,
     *      FLOW_ENV_VAR_1
     *  ]
     * @apiGroup Flows
     * @apiDescription Delete env variables to flow env variables
     *
     * @apiSuccessExample {json} Success-Response
     *  {
     *      path: /flow-name,
     *      name: flow-name,
     *      createdAt: 15123123
     *      updatedAt: 15123123
     *      envs: {
     *          FLOW_ENV_VAR_3: xxx,
     *          FLOW_ENV_VAR_4: xxx
     *      }
     *  }
     */
    @DeleteMapping("/{root}/env")
    @WebSecurity(action = Actions.FLOW_SET_ENV)
    public Flow removeContext(@RequestBody Set<String> envKeys) {
        return flowService.remove(flowName.get(), envKeys);
    }

    /**
     * @api {get} /flows/:root/branches List Branches
     * @apiParam {String} root flow node name
     * @apiParam {Boolean} [refresh] true or false, the default is false
     * @apiGroup Flows
     *
     * @apiSuccessExample {json} Success-Response
     *
     *  [
     *      master,
     *      develop,
     *      feature/xxx/xxx
     *  ]
     */
    @GetMapping("/{root}/branches")
    public List<String> listBranches(@RequestParam(required = false) Boolean refresh) {
        if (refresh == null) {
            refresh = false;
        }

        Node root = nodeService.find(flowName.get()).root();
        return gitService.branches(root, refresh);
    }

    /**
     * @api {get} /flows/:root/tags List Tags
     * @apiParam {String} root flow node name
     * @apiGroup Flows
     *
     * @apiSuccessExample {json} Success-Response
     *
     *  [
     *      v1.0,
     *      v2.0
     *  ]
     */
    @GetMapping("/{root}/tags")
    public List<String> listTags() {
        Node root = nodeService.find(flowName.get()).root();
        return gitService.tags(root, false);
    }

    /**
     * @api {post} /flows/:root/users/auth
     * @apiParam {String} root flow node name
     * @apiParamExample {json} Request-Body:
     *     {
     *         	"arrays" : ["test1@fir.im", "hl@fir.im"]
     *     }
     * @apiGroup Flows
     *
     * @apiSuccessExample {list} Success-Response
     *  [
     *    {
     *      email: "xxxx",
     *      username: "xxxx",
     *      flows: [
     *        "aaa"
     *      ]
     *      createdAt: 15123123
     *      updatedAt: 15123123
     *    },
     *    {}
     *  ]
     */
    @PostMapping("/{root}/users/auth")
    @WebSecurity(action = Actions.FLOW_AUTH)
    public List<User> flowAuthUsers(@RequestBody ListParam<String> listParam) {
        return nodeService.authUsers(listParam.getArrays(), flowName.get());
    }

    /**
     * @api {post} /flows/:root/trigger
     * @apiParam {String} root
     * @apiParamExample {json} Request-Body:
     *     {
     *         	"branchFilter" : ["master", "dev"]
     *         	"tagFilter" : ["v01", "v02"]
     *         	"tagEnabled": true
     *         	"pushEnabled": false
     *         	"prEnabled": true
     *     }
     * @apiGroup Flows
     *
     * @apiSuccessExample {list} Success-Response
     *  {
     *      "branchFilter": [
     *          "master",
     *          "develop"
     *     ],
     *     "tagFilter": [
     *          "aa"
     *     ]
     *     "tagEnable": false,
     *     "pushEnable": true,
     *     "prEnable": false,
     *      path: /flow-name,
     *      name: flow-name,
     *      createdAt: 15123123
     *      updatedAt: 15123123
     *      branchFilter: []
     *      envs: {
     *          FLOW_ENV_VAR_1: xxx,
     *          FLOW_ENV_VAR_2: xxx
     *      }
     */
    @PostMapping("/{root}/trigger")
    public Node trigger(@RequestBody TriggerParam triggerParam) {
        String path = flowName.get();
        Node flow = nodeService.find(path).root();
        envService.save(flow, triggerParam.toEnv(), true);
        return flow;
    }
}
