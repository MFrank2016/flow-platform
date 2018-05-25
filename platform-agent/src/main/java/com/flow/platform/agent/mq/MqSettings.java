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

/**
 * @author yh@fir.im
 */
public abstract class MqSettings {

    protected Channel channel;

    protected Connection connection;

    protected String queueName;

    public MqSettings(String host, String queueName) {
        this.queueName = queueName;

        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(host);

        try {
            connection = connectionFactory.newConnection();
            channel = connection.createChannel();
            channel.queueDeclare(queueName, true, false, false, null);

        } catch (Throwable throwable) {
            System.out.println(throwable.getMessage());
        }
    }

    public void close() {
        try {
            this.channel.close();
            this.connection.close();
        } catch (Throwable throwable) {
            System.out.println(throwable.getMessage());
        }
    }
}
