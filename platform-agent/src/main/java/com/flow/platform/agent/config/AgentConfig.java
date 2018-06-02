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

package com.flow.platform.agent.config;

import com.flow.platform.domain.AgentPath;
import com.flow.platform.domain.AgentSettings;
import com.flow.platform.domain.Jsonable;
import com.flow.platform.util.http.HttpClient;
import com.flow.platform.util.http.HttpResponse;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author yang
 */
@ToString
public final class AgentConfig {

    private static final String USER_HOME = System.getProperty("user.home");

    private static AgentConfig Instance;

    public static AgentConfig load(String baseUrl, String token) {
        final String url = baseUrl + "/agents/settings?token=" + token;
        HttpResponse<String> response = HttpClient.build(url).get().retry(5).bodyAsString();

        if (!response.hasSuccess()) {
            String err = "Unable to load agent setting with http status " + response.getStatusCode();
            throw new IllegalStateException(err);
        }

        AgentSettings settings = Jsonable.parse(response.getBody(), AgentSettings.class);
        Instance = new AgentConfig(token, settings);
        return Instance;
    }

    public static AgentConfig load(AgentSettings settings, String token) {
        Instance = new AgentConfig(token, settings);
        return Instance;
    }

    public static AgentConfig getInstance() {
        Objects.requireNonNull(Instance, "AgentConfig should be loaded at beginning");
        return Instance;
    }

    @Getter
    private final String token;

    @Getter
    private final AgentPath path;

    @Getter
    private final String cmdQueueName;

    @Getter
    private final String callbackQueueName;

    @Getter
    private final QueueConfig queue = new QueueConfig();

    @Getter
    private final ZookeeperConfig zk = new ZookeeperConfig();

    @Getter
    private final UpstreamUrlConfig url = new UpstreamUrlConfig();

    @Getter
    @Setter
    private Integer numOfConcurrentCmd = 1;

    @Getter
    @Setter
    private Path logDir = Paths.get(USER_HOME, ".flow-agent", "run-log");

    private AgentConfig(String token, AgentSettings settings) {
        this.token = token;
        path = settings.getAgentPath();
        cmdQueueName = settings.getListeningQueueName();
        callbackQueueName = settings.getCallbackQueueName();

        zk.setHost(settings.getZookeeperUrl());
        zk.setTimeout(1000);

        url.setWebsocket(settings.getWebSocketUrl());
        url.setCmdLog(settings.getCmdLogUrl());

        queue.setHost(settings.getMqUri());
        queue.setCmdQueueName(settings.getListeningQueueName());
        queue.setCallbackQueueName(settings.getCallbackQueueName());
    }
}
