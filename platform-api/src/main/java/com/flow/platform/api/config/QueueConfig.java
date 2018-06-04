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

import java.util.HashMap;
import java.util.Map;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
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
@EnableRabbit
@Configuration
public class QueueConfig {

    public final static long DEFAULT_CMD_CALLBACK_QUEUE_PRIORITY = 1L;

    public final static String JOB_QUEUE_NAME = "job.queue";

    public final static String CMD_CALLBACK_QUEUE_NAME = "cmd.callback.queue";

    public final static Map<String, Object> DEFAULT_QUEUE_ARGS = new HashMap<>(2);

    static {
        DEFAULT_QUEUE_ARGS.put("x-max-length", Integer.MAX_VALUE);
        DEFAULT_QUEUE_ARGS.put("x-max-priority", 255);
    }

    @Value("${api.queue.hosts}")
    private String hosts;

    @Value("${api.queue.username}")
    private String username;

    @Value("${api.queue.password}")
    private String password;

    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;

//    @Bean
//    public SyncService.QueueCreator syncQueueCreator() {
//        return name -> new MemoryQueue(taskExecutor, 50, name);
//    }
    
    @Bean
    public RabbitTemplate jobCmdTemplate() {
        return new RabbitTemplate(connectionFactory());
    }

    @Bean
    public AmqpAdmin amqpAdmin() {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory());
        admin.declareQueue(new Queue(JOB_QUEUE_NAME, true, false, false, DEFAULT_QUEUE_ARGS));
        admin.declareQueue(new Queue(CMD_CALLBACK_QUEUE_NAME, true, false, false, DEFAULT_QUEUE_ARGS));
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
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory() {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory());
        factory.setConcurrentConsumers(1);
        factory.setMaxConcurrentConsumers(1);
        return factory;
    }

    private CachingConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory("127.0.0.1");
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(password);
        return connectionFactory;
    }
}
