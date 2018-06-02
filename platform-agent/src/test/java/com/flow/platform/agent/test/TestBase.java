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

import com.flow.platform.agent.config.AgentConfig;
import com.flow.platform.domain.AgentPath;
import com.flow.platform.domain.AgentSettings;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * @author gy@fir.im
 */
public abstract class TestBase {

    static final String TOKEN = "123123";

    @BeforeClass
    public static void stubForConfig() {
        AgentSettings settings = new AgentSettings();
        settings.setWebSocketUrl("ws://localhost:8080/logging");
        settings.setCmdLogUrl("http://localhost:8080/cmd/log/upload");
        settings.setAgentPath(new AgentPath("hello", "world"));
        settings.setCallbackQueueName("cmd.callback.queue");
        settings.setListeningQueueName("agent.hello.world");
        settings.setMqUri("amqp://127.0.0.1:5672");
        settings.setZookeeperUrl("127.0.0.1:2181");

        AgentConfig.load(settings, TOKEN);
    }

    @AfterClass
    public static void afterClassBase() {
        Path logDir = AgentConfig.getInstance().getLogDir();
        try {
            Files.list(logDir).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            Files.deleteIfExists(logDir);
        } catch (IOException e) { }
    }

    String getResourceContent(String path) throws IOException {
        ClassLoader classLoader = CmdManagerTest.class.getClassLoader();
        URL resource = classLoader.getResource(path);
        return org.apache.curator.shaded.com.google.common.io.Files.toString(
            new File(resource.getFile()), Charset.forName("UTF-8"));
    }
}
