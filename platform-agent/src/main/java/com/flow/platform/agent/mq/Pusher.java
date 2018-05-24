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

import com.google.common.base.Charsets;

/**
 * @author yh@fir.im
 */
public class Pusher extends MqSettings {

    private static Pusher instance;

    public Pusher(String host, String queueName) {
        super(host, queueName);
    }

    public void send(String message) {
        try {
            this.channel.basicPublish("", this.queueName, null, message.getBytes(Charsets.UTF_8));
        } catch (Throwable throwable) {
            System.out.println(throwable.getMessage());
        }
    }

    public static void init(String host, String queueName) {
        instance = new Pusher(host, queueName);
    }

    public static void publish(String message) {
        instance.send(message);
    }

}
