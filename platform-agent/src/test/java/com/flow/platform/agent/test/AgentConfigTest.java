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

package com.flow.platform.agent.test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;

import com.flow.platform.agent.config.AgentConfig;
import com.flow.platform.domain.AgentPath;
import com.flow.platform.domain.AgentSettings;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author gy@fir.im
 */
public class AgentConfigTest extends TestBase {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8089);

    @Before
    public void init() {
        AgentSettings settings = new AgentSettings();
        settings.setWebSocketUrl("ws://localhost:8080/logging");
        settings.setCmdLogUrl("http://localhost:8080/cmd/log/upload");
        settings.setAgentPath(new AgentPath("hello", "world"));
        settings.setCallbackQueueName("cmd.callback.queue");
        settings.setListeningQueueName("agent.hello.world");
        settings.setMqUri("128.0.1.1");
        settings.setZookeeperUrl("123.123.123.123");

        stubFor(get("/agents/settings?token=" + TOKEN).willReturn(aResponse().withBody(settings.toJson())));
    }

    @Test
    public void should_load_agent_config() {
        // when: load config
        AgentConfig config = AgentConfig.load("http://localhost:8089", TOKEN);
        Assert.assertNotNull(config);

        // then:
        Assert.assertEquals("ws://localhost:8080/logging", config.getUrl().getWebsocket());
        Assert.assertEquals("http://localhost:8080/cmd/log/upload", config.getUrl().getCmdLog());

        Assert.assertEquals("cmd.callback.queue", config.getQueue().getCallbackQueueName());
        Assert.assertEquals("agent.hello.world", config.getQueue().getCmdQueueName());
        Assert.assertEquals("128.0.1.1", config.getQueue().getHost());

        Assert.assertEquals("123.123.123.123", config.getZk().getHost());

        Assert.assertEquals(TOKEN, config.getToken());

        Assert.assertEquals(new AgentPath("hello", "world"), config.getPath());
    }
}
