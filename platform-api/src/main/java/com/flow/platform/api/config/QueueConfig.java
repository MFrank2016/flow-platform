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

package com.flow.platform.api.config;

import com.flow.platform.api.service.SyncService;
import com.flow.platform.core.queue.MemoryQueue;
import com.flow.platform.core.queue.PriorityMessage;
import com.flow.platform.core.queue.RabbitQueue;
import com.flow.platform.queue.PlatformQueue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * @author yang
 */
@Configurable
public class QueueConfig {

    public final static long DEFAULT_CMD_CALLBACK_QUEUE_PRIORITY = 1L;

    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;

    /**
     * Queue to process cmd callback task
     */
    @Bean
    public PlatformQueue<PriorityMessage> cmdCallbackQueue() {
        return new MemoryQueue(taskExecutor, 50, "CmdCallbackQueue");
    }

    @Bean
    public SyncService.QueueCreator syncQueueCreator() {
        return name -> new MemoryQueue(taskExecutor, 50, name);
    }

    @Bean
    public PlatformQueue<PriorityMessage> jobQueue() {
        return new RabbitQueue(taskExecutor, "127.0.0.1", 50, 100, "JobQueue");
    }

    @Bean
    public PlatformQueue<PriorityMessage> jobResultQueue() {
        return new RabbitQueue(taskExecutor, "127.0.0.1", 50, 100, "JobResultQueue");
    }

    @Bean
    public RabbitTemplate rabbit() {
        RabbitTemplate template = new RabbitTemplate(connectionFactory());
        return template;
    }

    @Bean
    public RabbitAdmin rabbitAdmin() {
        RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory());
        return rabbitAdmin;
    }

    private CachingConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory("127.0.0.1");
        return connectionFactory;
    }
}
