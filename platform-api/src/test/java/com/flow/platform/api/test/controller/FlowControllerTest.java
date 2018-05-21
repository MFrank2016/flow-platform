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

package com.flow.platform.api.test.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.flow.platform.api.domain.Flow;
import com.flow.platform.api.domain.response.BooleanValue;
import com.flow.platform.api.service.v1.FlowService;
import com.flow.platform.api.test.FlowHelper;
import com.flow.platform.core.exception.NotFoundException;
import com.flow.platform.core.response.ResponseError;
import com.flow.platform.util.StringUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * @author yang
 */
public class FlowControllerTest extends ControllerTestWithoutAuth {

    private final String flowName = "flow_default";

    @Autowired
    private FlowHelper flowHelper;

    @Autowired
    private FlowService flowService;

    @Before
    public void init() throws Throwable {
        stubDemo();
        createEmptyFlow(flowName);
    }

    @Test
    public void should_get_flow_node_detail() throws Throwable {
        Flow flowNode = Flow.parse(performRequestWith200Status(get("/flows/" + flowName + "/show")), Flow.class);
        Assert.assertNotNull(flowNode);
        Assert.assertEquals(flowName, flowNode.getName());
    }

    @Test(expected = NotFoundException.class)
    public void should_delete_flow_success() throws Exception {
        // given:
        String flowName = "flow1";
        setCurrentUser(mockUser);
        Flow flow = flowHelper.createFlowWithYml(flowName, "yml/demo_flow2.yaml");

        // when: perform http delete
        Flow deleted = Flow.parse(performRequestWith200Status(delete("/flows/" + flowName)), Flow.class);

        // then: flow and flow yml been deleted
        Assert.assertEquals(flow, deleted);
        Assert.assertNull(flowDao.get(flowName));
        Assert.assertNull(ymlDao.get(flowName));
        flowService.find(flowName);
    }

    @Test
    public void should_get_env_value() throws Throwable {
        // given: add env to context
        mockMvc.perform(post("/flows/" + flowName + "/env")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"FLOW_STATUS\": \"PENDING\"}")).andExpect(status().isOk());

        // when: get env value from flow object
        Flow flow = Flow.parse(performRequestWith200Status(get("/flows/" + flowName)), Flow.class);

        // then: env value existed
        Assert.assertEquals("PENDING", flow.getContext().get("FLOW_STATUS"));
    }

    @Test
    public void should_response_4xx_if_flow_name_format_invalid_when_create_flow() throws Throwable {
        String flowName = "hello*gmail";

        MvcResult result = this.mockMvc.perform(post("/flows/" + flowName)
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().is4xxClientError())
            .andReturn();

        String body = result.getResponse().getContentAsString();
        ResponseError error = ResponseError.parse(body, ResponseError.class);
        Assert.assertNotNull(error);
        Assert.assertEquals(error.getMessage(), "Illegal node name: hello*gmail");
    }

    @Test
    public void should_response_false_if_flow_name_not_exist() throws Throwable {
        // given:
        String flowName = "not-exit";

        // when:
        String response = performRequestWith200Status(
            get("/flows/" + flowName + "/exist").contentType(MediaType.APPLICATION_JSON));

        // then:
        BooleanValue existed = BooleanValue.parse(response, BooleanValue.class);
        Assert.assertNotNull(existed);
        Assert.assertFalse(existed.getValue());
    }

    @Test
    public void should_get_yml_file_content() throws Throwable {
        // given:
        String yml = getResourceContent("yml/demo_flow.yaml");
        flowService.updateYml(flowName, yml);

        // when:
        String content = performRequestWith200Status(get("/flows/" + flowName + "/yml"));

        // then:
        Assert.assertEquals(yml, content);
    }

    @Test
    public void should_return_empty_string_if_no_yml_content() throws Throwable {
        // when:
        MockHttpServletRequestBuilder request = get("/flows/" + flowName + "/yml");
        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andReturn();
        String content = result.getResponse().getContentAsString();

        // then:
        Assert.assertEquals(StringUtil.EMPTY, content);
    }

    @Test
    public void should_download_yml_success() throws Exception {
        // given:
        String yml = getResourceContent("yml/demo_flow.yaml");
        performRequestWith200Status(post("/flows/" + flowName + "/yml").content(yml));

        // when: download yml
        MvcResult result = mockMvc.perform(get("/flows/" + flowName + "/yml/download").contentType(MediaType.ALL))
            .andExpect(status().isOk())
            .andReturn();
        Assert.assertNotNull(result.getResponse());

        // then:
        Assert.assertEquals(yml, result.getResponse().getContentAsString());
    }
}
