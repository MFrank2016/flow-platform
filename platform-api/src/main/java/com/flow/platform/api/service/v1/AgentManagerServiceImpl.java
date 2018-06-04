/*
 * Copyright 2018 flow.ci
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

package com.flow.platform.api.service.v1;

import com.flow.platform.api.config.QueueConfig;
import com.flow.platform.api.dao.v1.AgentDao;
import com.flow.platform.api.exception.AgentNotAvailableException;
import com.flow.platform.api.util.ZKHelper;
import com.flow.platform.core.exception.NotFoundException;
import com.flow.platform.core.service.ApplicationEventService;
import com.flow.platform.domain.Agent;
import com.flow.platform.domain.AgentPath;
import com.flow.platform.domain.AgentSettings;
import com.flow.platform.domain.AgentStatus;
import com.flow.platform.util.zk.ZKClient;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent.Type;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.data.Stat;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author yh@fir.im
 */
@Service
@Log4j2
public class AgentManagerServiceImpl extends ApplicationEventService implements AgentManagerService {

    /**
     * Zookeeper node path which used for lock of acquire agent
     */
    private final static String LOCKER_PATH = ZKPaths.makePath(AgentPath.ROOT, "locker");

    @Autowired
    private AgentDao agentDao;

    @Autowired
    private ZKClient zkClient;

    @Autowired
    private AmqpAdmin amqpAdmin;

    @PostConstruct
    public void init() {
        zkClient.create(AgentPath.ROOT, null);
        zkClient.create(LOCKER_PATH, null);

        // detect node online or offline
        zkClient.watchChildren(AgentPath.ROOT, new ZkNodeWatcherEvent());
    }

    @Override
    public Agent create(AgentPath agentPath) {
        Agent agent = new Agent(agentPath);
        agent.setToken(UUID.randomUUID().toString());
        agentDao.save(agent);

        declareQueue(agent);
        return agent;
    }

    @Override
    public Agent find(AgentPath agentPath) {
        return appendAgentStatus(agentDao.get(agentPath));
    }

    @Override
    public Agent find(String token) {
        Agent agent = agentDao.getByToken(token);

        if (Objects.isNull(agent)) {
            throw new NotFoundException("Agent not found for token: " + token);
        }

        appendAgentStatus(agent);
        return agent;
    }

    @Override
    public List<Agent> list() {
        List<Agent> agents = agentDao.list();
        return appendAgentsStatus(agents);
    }

    @Override
    public void release(Agent agent) {
        zkClient.setData(agent.fullPath(), AgentStatus.IDLE.getBytes());
    }

    @Override
    public Agent acquire() throws AgentNotAvailableException {
        List<Agent> availableAgents = findAvailableAgents();

        if (availableAgents.isEmpty()) {
            throw new AgentNotAvailableException("No available agent");
        }

        Agent selectedAgent = availableAgents.get(0);

        // create lock try to set agent status
        try {
            zkClient.lock(LOCKER_PATH, (path) -> {
                zkClient.setData(selectedAgent.fullPath(), AgentStatus.BUSY.getBytes());
                selectedAgent.setStatus(AgentStatus.BUSY);
            });
        } catch (Throwable e) {
            throw new AgentNotAvailableException(e.getMessage());
        }

        return selectedAgent;
    }

    @Override
    public AgentSettings settings(String token) {
        Agent agent = find(token);

        AgentSettings agentSettings = new AgentSettings();
        agentSettings.setAgentPath(agent.getPath());
        agentSettings.setMqUri("amqp://127.0.0.1:5672");
        agentSettings.setZookeeperUrl("127.0.0.1:2181");
        agentSettings.setCallbackQueueName(QueueConfig.CMD_CALLBACK_QUEUE_NAME);
        agentSettings.setListeningQueueName(agent.queueName());

        return agentSettings;
    }

    /**
     * Declare queue with agent name
     */
    private Queue declareQueue(Agent agent) {
        Queue queue = new Queue(agent.queueName(), true, false, false, QueueConfig.DEFAULT_QUEUE_ARGS);
        amqpAdmin.declareQueue(queue);
        return queue;
    }

    private List<Agent> findAvailableAgents() {
        List<Agent> agents = list();
        agents.removeIf(agent -> agent.getStatus() == AgentStatus.BUSY);
        return agents;
    }

    private void createRoot() {
        zkClient.create(AgentPath.ROOT, null);
    }

    private Agent appendAgentStatus(Agent agent) {
        Objects.requireNonNull(agent, "Agent cannot be null");

        try {
            Stat stat = new Stat();
            agent.setStatus(AgentStatus.valueOf(new String(zkClient.getData(agent.fullPath(), stat))));
        } catch (Throwable throwable) {
            agent.setStatus(AgentStatus.OFFLINE);
        }
        return agent;
    }

    private List<Agent> appendAgentsStatus(List<Agent> agents) {
        for (Agent agent : agents) {
            appendAgentStatus(agent);
        }
        return agents;
    }

    private class ZkNodeWatcherEvent implements PathChildrenCacheListener {

        @Override
        public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) {
            final Type eventType = event.getType();
            final String path = event.getData().getPath();
            final String name = ZKHelper.getNameFromPath(path);

            log.debug("Receive zookeeper event {} {}", eventType, path);

            AgentPath agentPath = AgentPath.parse(name);

            if (eventType == Type.CHILD_ADDED) {
                return;
            }

            if (eventType == Type.CHILD_REMOVED) {
                return;
            }

            if (eventType == Type.CHILD_UPDATED) {
                return;
            }
        }
    }
}
