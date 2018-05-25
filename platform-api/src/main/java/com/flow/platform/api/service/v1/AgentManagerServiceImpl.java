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

package com.flow.platform.api.service.v1;

import com.flow.platform.api.dao.v1.AgentDao;
import com.flow.platform.api.dao.v1.JobTreeDao;
import com.flow.platform.api.domain.v1.JobTree;
import com.flow.platform.api.domain.v1.JobV1;
import com.flow.platform.api.util.ZKHelper;
import com.flow.platform.core.exception.FlowException;
import com.flow.platform.core.service.ApplicationEventService;
import com.flow.platform.domain.Agent;
import com.flow.platform.domain.AgentPath;
import com.flow.platform.domain.AgentSettings;
import com.flow.platform.domain.AgentStatus;
import com.flow.platform.domain.v1.JobKey;
import com.flow.platform.tree.Cmd;
import com.flow.platform.tree.Node;
import com.flow.platform.tree.YmlEnvs;
import com.flow.platform.util.zk.ZKClient;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent.Type;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.zookeeper.data.Stat;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author yh@fir.im
 */
@Service
@Log4j2
public class AgentManagerServiceImpl extends ApplicationEventService implements AgentManagerService {

    @Autowired
    private JobService jobServiceV1;

    @Autowired
    private JobTreeDao jobTreeDao;

    @Autowired
    private AgentDao agentDao;

    @Autowired
    private ZKClient zkClient;

    @Autowired
    private RabbitTemplate commonTemplate;

    @Autowired
    private AmqpAdmin amqpAdmin;

    private final static String ZK_ROOT_NODE = "/flow-agents";

    private final static String SPLIT_CHARS = "===";

    @PostConstruct
    public void init() {
        createRoot();

        // detect node online or offline
        zkClient.watchChildren(ZK_ROOT_NODE, new ZkNodeWatcherEvent());
    }

    @Override
    public Agent create(AgentPath agentPath) {
        Agent agent = new Agent(agentPath);
        agent.setToken(UUID.randomUUID().toString());
        agentDao.save(agent);
        return agent;
    }

    @Override
    public Agent find(AgentPath agentPath) {
        return appendAgentStatus(agentDao.get(agentPath));
    }

    @Override
    public List<Agent> list() {
        return appendAgentsStatus(agentDao.list());
    }

    public void resetAgentStatus(AgentStatus agentStatus, Agent agent) {
        zkClient.setData(zkNodePath(agent), agentStatus.toString().getBytes());
    }

    public void handleJob(JobKey jobKey) {
        log.trace("Handle job to lock agent and send first node to queue , key is " + jobKey);

        JobV1 jobV1 = jobServiceV1.find(jobKey);
        JobTree jobTree = jobTreeDao.get(jobV1.getKey());
        Agent agent = selectAgent();
        Node node = jobTree.getTree().next(jobTree.getTree().getRoot().getPath());

        Cmd cmd = buildCmdFromNode(node, jobV1.getKey(), agent);

        log.trace("Send cmd to queue:  " + buildQueueName(agent.getPath()));
        commonTemplate.send(buildQueueName(agent.getPath()),
            new Message(cmd.toJson().getBytes(), new MessageProperties()));
    }

    @Override
    public Cmd buildCmdFromNode(Node node, JobKey jobKey, Agent agent) {
        // trans node to cmd
        Cmd cmd = new Cmd();
        cmd.setNodePath(node.getPath());
        cmd.setContent(node.getContent());
        cmd.setContext(node.getContext());
        cmd.put(YmlEnvs.TIMEOUT, "100");
        cmd.put(YmlEnvs.WORK_DIR, "/tmp"); //TODO: Working dir

        if (!Objects.isNull(agent)) {
            // lock agent
            cmd.put(YmlEnvs.AGENT_TOKEN, agent.getToken());
        }

        cmd.setJobKey(jobKey);

        return cmd;
    }

    @Override
    public Agent selectAgent() {

        List<Agent> availableAgents = findAvailableAgents();

        if (availableAgents.size() == 0) {
            throw new FlowException("Not found available agents");
        }

        Agent selectedAgent = availableAgents.get(0);
        lockAgent(selectedAgent);
        return selectedAgent;
    }

    @Override
    public AgentSettings settings(String token) {

        Agent agent = agentDao.getByToken(token);

        if (Objects.isNull(agent)) {
            throw new FlowException("Agent not found this token");
        }

        AgentSettings agentSettings = new AgentSettings();
        agentSettings.setAgentPath(agent.getPath());
        agentSettings.setRabbitmqHost("127.0.0.1");
        agentSettings.setZookeeperUrl("127.0.0.1:2181");
        agentSettings.setCallbackQueueName("cmd.callback.queue");
        agentSettings.setListeningQueueName(buildQueueName(agent.getPath()));

        return agentSettings;
    }

    @Override
    public String agentQueue(Agent agent) {
        return buildQueueName(agent.getPath());
    }

    private void report(AgentPath agentPath, AgentStatus agentStatus) {

        if (agentStatus == AgentStatus.IDLE) {

            declareQueue(buildQueueName(agentPath));
        }

        if (agentStatus == AgentStatus.OFFLINE) {

        }
    }

    private Queue declareQueue(String name) {
        Map<String, Object> cmdQueueArgs = new HashMap<>();
        cmdQueueArgs.put("x-max-length", Integer.MAX_VALUE);
        cmdQueueArgs.put("x-max-priority", 255);
        Queue queue = new Queue(name, true, false, false, cmdQueueArgs);
        amqpAdmin.declareQueue(queue);
        return queue;
    }

    private void lockAgent(Agent agent) {
        try {
            zkClient.getClient().inTransaction()
                .check()
                .forPath(zkNodePath(agent))
                .and()
                .setData().withVersion(agent.getVersion())
                .forPath(zkNodePath(agent), AgentStatus.BUSY.toString().getBytes())
                .and()
                .commit();
        } catch (Throwable e) {
            throw new FlowException("Lock agent failure: " + agent.toString());
        }
    }

    private String zkNodePath(Agent agent) {
        return ZK_ROOT_NODE + "/" + agent.getZone() + SPLIT_CHARS + agent.getName();
    }

    private List<Agent> findAvailableAgents() {
        List<Agent> agents = list();
        List<Agent> availableList = new ArrayList<>(agents.size());

        for (Agent agent : agents) {
            if (Objects.equals(agent.getStatus(), AgentStatus.IDLE)) {
                availableList.add(agent);
            }
        }

        return availableList;
    }

    private void createRoot() {
        zkClient.create(ZK_ROOT_NODE, null);
    }

    private Agent appendAgentStatus(Agent agent) {
        if (Objects.isNull(agent)) {
            return agent;
        }

        try {
            Stat stat = new Stat();
            agent.setStatus(AgentStatus.valueOf(new String(zkClient.getData(zkNodePath(agent), stat))));
            agent.setVersion(stat.getVersion());
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

    private String buildQueueName(AgentPath agentPath) {
        return agentPath.getZone() + SPLIT_CHARS + agentPath.getName();
    }

    class ZkNodeWatcherEvent implements PathChildrenCacheListener {

        @Override
        public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
            final Type eventType = event.getType();
            final String path = event.getData().getPath();
            final String name = ZKHelper.getNameFromPath(path);
            log.debug("Receive zookeeper event {} {}", eventType, path);

            if (eventType == Type.CHILD_ADDED) {

                report(new AgentPath(name.split(SPLIT_CHARS)[0], name.split(SPLIT_CHARS)[1]), AgentStatus.IDLE);
                // report online
                return;
            }

            if (eventType == Type.CHILD_REMOVED) {
                // report offline

                report(new AgentPath(name.split(SPLIT_CHARS)[0], name.split(SPLIT_CHARS)[1]), AgentStatus.OFFLINE);
                return;
            }
        }
    }


}
