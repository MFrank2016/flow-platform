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

import com.flow.platform.util.StringUtil;
import com.google.common.base.Charsets;
import lombok.extern.log4j.Log4j2;

/**
 * @author yh@fir.im
 */
@Log4j2
public class Pusher extends RabbitClient {

    private final static String DEFAULT_EXCHANGE = StringUtil.EMPTY;

    public Pusher(String host, String queueName) {
        super(host, queueName, null);
    }

    public void send(String message) {
        try {
            getChannel().basicPublish(DEFAULT_EXCHANGE, getQueueName(), null, message.getBytes(Charsets.UTF_8));
        } catch (Throwable throwable) {
            log.error(throwable.getMessage());
        }
    }
}
