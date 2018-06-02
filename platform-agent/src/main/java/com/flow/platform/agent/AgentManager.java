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

package com.flow.platform.agent;

import com.flow.platform.agent.config.AgentConfig;
import com.flow.platform.agent.config.QueueConfig;
import com.flow.platform.agent.config.ZookeeperConfig;
import com.flow.platform.agent.mq.Pusher;
import com.flow.platform.domain.AgentStatus;
import com.flow.platform.util.zk.ZKClient;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent.Type;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;

/**
 * @author gy@fir.im
 */
@Log4j2
public class AgentManager implements Runnable, TreeCacheListener, AutoCloseable {

    private final static Object STATUS_LOCKER = new Object();

    final static ThreadFactory DEFAULT_THREAD_FACTORY = r -> {
        Thread t = Executors.defaultThreadFactory().newThread(r);
        t.setDaemon(true);
        return t;
    };

    @Getter
    private final AgentConfig config;

    @Getter
    private final ZKClient zkClient;

    @Getter
    private final CmdQueueConsumer cmdConsumer;

    public AgentManager(AgentConfig config) {
        this.config = config;
        this.zkClient = initZookeeperClient(config.getZk());
        this.cmdConsumer = initCmdQueueConsumer(config.getQueue());
    }

    /**
     * Stop agent
     */
    public void stop() {
        synchronized (STATUS_LOCKER) {
            STATUS_LOCKER.notifyAll();
        }
    }

    @Override
    public void run() {
        // init zookeeper
        zkClient.start();

        registerZkNodeAndWatch();

        synchronized (STATUS_LOCKER) {
            try {
                STATUS_LOCKER.wait();
            } catch (InterruptedException e) {
                log.warn("InterruptedException : " + e.getMessage());
            }
        }
    }

    private void exit() {
        log.info("One Agent is running in other place. Please first to stop another agent, thx!");
        Runtime.getRuntime().exit(1);
    }

    @Override
    public void childEvent(CuratorFramework client, TreeCacheEvent event) {
        ChildData eventData = event.getData();
        log.trace("========= Event: {} =========", event.getType());

        if (event.getType() == Type.CONNECTION_RECONNECTED) {
            registerZkNodeAndWatch();
            return;
        }

        if (event.getType() == Type.NODE_UPDATED) {
            return;
        }

        if (event.getType() == Type.NODE_REMOVED) {
            close();
        }
    }

    @Override
    public void close() {
        closeZookeeper();
        closeRabbitQueue();
        stop();
    }

    /**
     * Register agent node to server
     * Monitor data changed event
     *
     * @return path of zookeeper or null if failure
     */
    private String registerZkNodeAndWatch() {
        String zookeeperPath = config.getPath().fullPath();
        return zkClient.createEphemeral(zookeeperPath, AgentStatus.IDLE.toString().getBytes());
    }

    private void closeZookeeper() {
        String zookeeperPath = config.getPath().fullPath();
        zkClient.deleteWithoutGuaranteed(zookeeperPath, false);
    }

    private void closeRabbitQueue() {
        cmdConsumer.close();
    }

    private ZKClient initZookeeperClient(ZookeeperConfig config) {
        final int ZK_RECONNECT_TIME = 1;
        final int ZK_RETRY_PERIOD = 500;
        return new ZKClient(config.getHost(), ZK_RETRY_PERIOD, ZK_RECONNECT_TIME);
    }

    private CmdQueueConsumer initCmdQueueConsumer(QueueConfig config) {
        String host = config.getHost();
        String cmdQueueName = config.getCmdQueueName();
        ExecutorService pool = Executors.newFixedThreadPool(1, DEFAULT_THREAD_FACTORY);
        return new CmdQueueConsumer(host, cmdQueueName, pool);
    }
}