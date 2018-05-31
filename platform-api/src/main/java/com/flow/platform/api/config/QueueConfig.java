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

import com.flow.platform.api.consumer.v1.JobQueueConsumer;
import com.flow.platform.api.consumer.v1.CmdWebhookConsumer;
import com.flow.platform.api.service.SyncService;
import com.flow.platform.core.queue.MemoryQueue;
import com.flow.platform.core.queue.PriorityMessage;
import com.flow.platform.queue.PlatformQueue;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * RabbitMQ configuration
 *
 * @author yang
 */
@Configuration
public class QueueConfig {

    public final static long DEFAULT_CMD_CALLBACK_QUEUE_PRIORITY = 1L;

    private final static String JOB_QUEUE_NAME = "job.queue";

    private final static String CMD_WEBHOOK_QUEUE_NAME = "cmd.callback.queue";

    @Value("${api.queue.hosts}")
    private String hosts;

    @Value("${api.queue.username}")
    private String username;

    @Value("${api.queue.password}")
    private String password;

    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;

    @Autowired
    private JobQueueConsumer jobQueueConsumer;

    @Autowired
    private CmdWebhookConsumer cmdWebhookConsumer;

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
    public RabbitTemplate jobCmdTemplate() {
        return new RabbitTemplate(connectionFactory());
    }

    @Bean
    public AmqpAdmin amqpAdmin() {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory());
        admin.declareQueue(new Queue(JOB_QUEUE_NAME));
        admin.declareQueue(new Queue(CMD_WEBHOOK_QUEUE_NAME));
        return admin;
    }

    @Bean
    public RabbitTemplate jobQueueTemplate() {
        RabbitTemplate template = new RabbitTemplate(connectionFactory());
        template.setRoutingKey(JOB_QUEUE_NAME);
        template.setQueue(JOB_QUEUE_NAME);
        return template;
    }

    @Bean
    public SimpleMessageListenerContainer jobQueueListener() {
        SimpleMessageListenerContainer container = buildContainer();
        container.setQueueNames(JOB_QUEUE_NAME);
        container.setConcurrentConsumers(1);
        container.setMessageListener(new MessageListenerAdapter(jobQueueConsumer));
        return container;
    }

    @Bean
    public SimpleMessageListenerContainer cmdWebhookQueueListener() {
        SimpleMessageListenerContainer container = buildContainer();
        container.setQueueNames(CMD_WEBHOOK_QUEUE_NAME);
        container.setConcurrentConsumers(1);
        container.setMessageListener(new MessageListenerAdapter(cmdWebhookConsumer));
        return container;
    }

    private SimpleMessageListenerContainer buildContainer() {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory());
        return container;
    }

    private CachingConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory("127.0.0.1");
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(password);
        return connectionFactory;
    }
}
