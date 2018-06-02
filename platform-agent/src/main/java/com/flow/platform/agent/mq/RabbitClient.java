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

package com.flow.platform.agent.mq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

/**
 * @author yh@fir.im
 */
@Log4j2
public abstract class RabbitClient {

    @Getter
    private Channel channel;

    @Getter
    private Connection connection;

    @Getter
    private final String queueName;

    public RabbitClient(String host, String queueName, ExecutorService executorService) {
        this.queueName = queueName;

        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(host);

        try {
            if (Objects.isNull(executorService)) {
                connection = connectionFactory.newConnection();
            } else{
                connection = connectionFactory.newConnection(executorService);
            }
            channel = connection.createChannel();
            channel.queueDeclare(queueName, true, false, false, null);
        } catch (Throwable throwable) {
            log.error("Unable to connect queue : " + host + " - " + queueName);
        }
    }

    public void close() {
        try {
            this.channel.close();
            this.connection.close();
        } catch (Throwable e) {
            log.error("Error when close queue: " + e.getMessage());
        }
    }
}
