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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.flow.platform.api.test.TestBase;
import com.flow.platform.domain.AgentPath;
import com.flow.platform.domain.AgentPathWithPassword;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * @author yh@firim
 */
public class AgentControllerTest extends TestBase {

    @Before
    public void before() {
        stubDemo();
    }

    @Test
    public void should_shutdown_success() throws Throwable {
        MockHttpServletRequestBuilder request = post("/agents/shutdown")
            .content(new AgentPathWithPassword("default", "machine", "123456").toJson())
            .contentType(MediaType.APPLICATION_JSON_VALUE);

        mockMvc.perform(request).andExpect(status().isOk()).andReturn();
    }

    @Test
    public void should_close_agent_success() throws Throwable {
        mockMvc.perform(post("/agents/close")
            .content(new AgentPath("default", "machine").toJson())
            .contentType(MediaType.APPLICATION_JSON_VALUE)).andExpect(status().isOk()).andReturn();
    }
}
