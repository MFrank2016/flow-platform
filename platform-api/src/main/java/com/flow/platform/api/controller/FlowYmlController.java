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

import com.flow.platform.api.config.AppConfig;
import com.flow.platform.api.domain.v1.Flow;
import com.flow.platform.api.domain.v1.FlowYml;
import com.flow.platform.api.domain.permission.Actions;
import com.flow.platform.api.security.WebSecurity;
import com.flow.platform.api.service.v1.FlowService;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author yang
 */
@RestController
@RequestMapping(path = "/flows/{root}/yml")
public class FlowYmlController extends NodeController {

    @Autowired
    private FlowService flowService;

    /**
     * @api {get} /flows/:root/yml Get
     * @apiParam {String} root flow node name of yml
     * @apiGroup Flow YML
     * @apiDescription Get flow node related yml content,
     * response empty yml content if it is loading from git repo
     *
     * @apiSuccessExample {yaml} Success-Response
     *  - flows
     *      - name: xx
     *      - steps:
     *          - name: xxx
     */
    @GetMapping
    @WebSecurity(action = Actions.FLOW_SHOW)
    public String getYml() {
        Flow flow = flowService.find(flowName.get());
        FlowYml root = flowService.findYml(flow);
        return root.getContent();
    }

    /**
     * @api {post} /flows/:root/yml/download YML Download
     * @apiParam {String} root flow node name to set yml content
     * @apiGroup Flow YML
     * @apiDescription download yml for flow,
     * the flow name must be matched with flow name defined in yml
     *
     * @apiSuccessExample {json} Success-Response
     *
     *  yml file
     */
    @GetMapping("/download")
    @WebSecurity(action = Actions.FLOW_CREATE)
    public Resource downloadYml(HttpServletResponse httpResponse) throws IOException {
        Flow flow = flowService.find(flowName.get());
        FlowYml flowYml = flowService.findYml(flow);

        httpResponse.setHeader(
            "Content-Disposition",
            String.format("attachment; filename=%s", flow.getName() + ".yml"));

        try (InputStream is = new ByteArrayInputStream(flowYml.getContent().getBytes(AppConfig.DEFAULT_CHARSET))) {
            return new InputStreamResource(is);
        }
    }

    /**
     * @api {post} /flows/:root/yml YML Update
     * @apiParam {String} root flow node name to update yml content
     * @apiParam Request-Body
     *  - flows:
     *      - name: xxx
     *      - steps:
     *          - name: xxx
     *
     * @apiGroup Flow YML
     * @apiDescription Update yml for flow,
     * the flow name must be matched with flow name defined in yml
     *
     * @apiSuccessExample {json} Success-Response
     *
     *  yml body
     */
    @PostMapping
    @WebSecurity(action = Actions.FLOW_CREATE)
    public String updateYml(@RequestBody String yml) {
        Flow flow = flowService.find(flowName.get());
        flowService.updateYml(flow, yml);
        return yml;
    }
}
