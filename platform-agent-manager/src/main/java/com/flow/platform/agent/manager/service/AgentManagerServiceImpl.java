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

package com.flow.platform.agent.manager.service;

import com.flow.platform.agent.manager.dao.AgentDao;
import com.flow.platform.agent.manager.event.AgentResourceEvent;
import com.flow.platform.agent.manager.event.AgentResourceEvent.Category;
import com.flow.platform.agent.manager.exception.AgentErr;
import com.flow.platform.core.exception.IllegalParameterException;
import com.flow.platform.core.service.WebhookServiceImplBase;
import com.flow.platform.domain.Agent;
import com.flow.platform.domain.AgentPath;
import com.flow.platform.domain.AgentSettings;
import com.flow.platform.domain.AgentStatus;
import com.flow.platform.util.DateUtil;
import com.flow.platform.util.zk.ZKClient;
import com.google.common.base.Strings;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.extern.log4j.Log4j2;
import org.apache.zookeeper.data.Stat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author gy@fir.im
 */
@Log4j2
@Service
@Transactional
public class AgentManagerServiceImpl extends WebhookServiceImplBase implements AgentManagerService {

    @Autowired
    private AgentDao agentDao;

    @Autowired
    protected ZKClient zkClient;

    @Autowired
    private AgentSettings agentSettings;

    @Value("${zk.node.root}")
    private String rootNodeName;

    private final static String STATUS = "STATUS";

    private final static String SPLITCHARS = "---";

    @Override
    public void report(AgentPath path, AgentStatus status) {
        Agent exist = find(path);

        // For agent offline status
        if (status == AgentStatus.OFFLINE) {
            exist.setSessionId(null);
        }

        saveWithStatus(exist, AgentStatus.IDLE);
        return;
    }

    @Override
    @Transactional(readOnly = true)
    public Agent find(AgentPath key) {
        return appendStatusToAgent(agentDao.get(key));
    }

    @Override
    @Transactional(readOnly = true)
    public Agent find(String sessionId) {
        return appendStatusToAgent(agentDao.get(sessionId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Agent> findAvailable(String zone) {
        return agentsFromZookeeper(AgentStatus.IDLE);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Agent> list(String zone) {
        if (Strings.isNullOrEmpty(zone)) {
            return appendStatusToAgents(agentDao.list());
        }
        return appendStatusToAgents(agentDao.list(zone, "createdDate"));
    }

    @Override
    public void saveWithStatus(Agent agent, AgentStatus status) {
        if (agent == null || status == null) {
            return;
        }

        if (!agentDao.exist(agent.getPath())) {
            throw new AgentErr.NotFoundException(agent.getName());
        }

        agent.setStatus(status);
        log.trace("Agent status been updated to '{}'", status);

        this.webhookCallback(agent);

        // boardcast AgentResourceEvent for release
        if (agent.getStatus() == AgentStatus.IDLE) {
            this.dispatchEvent(new AgentResourceEvent(this, agent.getZone(), Category.RELEASED));
        }
    }

    @Override
    public boolean isSessionTimeout(Agent agent, ZonedDateTime compareDate, long timeoutInSeconds) {
        if (agent.getSessionId() == null) {
            throw new UnsupportedOperationException("Target agent is not enable session");
        }

        long sessionAlive = ChronoUnit.SECONDS.between(agent.getSessionDate(), compareDate);
        return sessionAlive >= timeoutInSeconds;
    }

    @Override
    public Agent create(AgentPath agentPath, String webhook) {
        Agent agent = agentDao.get(agentPath);
        if (agent != null) {
            throw new IllegalParameterException(String.format("The agent '%s' has already exsited", agentPath));
        }

        agent = new Agent(agentPath);
        agent.setCreatedDate(DateUtil.now());
        agent.setUpdatedDate(DateUtil.now());
        agent.setWebhook(webhook);

        //random token
        agent.setToken(UUID.randomUUID().toString());
        agentDao.save(agent);

        return agent;
    }

    @Override
    public String refreshToken(AgentPath agentPath) {
        Agent agent = agentDao.get(agentPath);
        if (agent != null) {
            throw new IllegalParameterException(String.format("The agent '%s' has already exsited", agentPath));
        }

        //random token
        agent.setToken(UUID.randomUUID().toString());
        agentDao.save(agent);

        return agent.getToken();
    }

    @Override
    public AgentSettings settings(String token) {
        Agent agent = agentDao.getByToken(token);

        // validate token
        if (agent == null) {
            throw new IllegalParameterException("Illegal agent token");
        }

        agentSettings.setAgentPath(agent.getPath());
        return agentSettings;
    }

    @Override
    public void delete(Agent agent) {
        try {
            agentDao.delete(agent);
        } catch (Throwable e) {
            throw new UnsupportedOperationException("delete agent failure " + e.getMessage());
        }
    }

    public List<Agent> agentsFromZookeeper(AgentStatus status) {

        List<Agent> agents = agentDao.list();
        List<Agent> result = new ArrayList<>(agents.size());

        appendStatusToAgents(agents);

        if (!Objects.isNull(status)) {
            for (Agent agent : agents) {
                if (Objects.equals(agent.getStatus(), status)) {
                    result.add(agent);
                }
            }

            return result;
        }

        return agents;
    }

    @Override
    public String statusNode(Agent agent) {
        return zkStatusNode() + "/" + agent.getPath().getZone() + SPLITCHARS + agent.getPath().getName();
    }

    private List<Agent> appendStatusToAgents(List<Agent> agents) {
        for (Agent agent : agents) {
            appendStatusToAgent(agent);
        }
        return agents;
    }

    private Agent appendStatusToAgent(Agent agent) {

        if (Objects.isNull(agent)) {
            return agent;
        }

        String childPath = statusNode(agent);
        if (!Objects.isNull(agent)) {
            Stat stat = new Stat();
            String data;
            try {
                data = new String(zkClient.getData(childPath, stat));
                agent.setVersion(stat.getVersion());
            } catch (Throwable throwable) {
                data = AgentStatus.OFFLINE.toString();
            }
            agent.setStatus(AgentStatus.valueOf(data));
        }

        return agent;
    }

    private String zkStatusNode() {
        return "/" + rootNodeName + "/" + STATUS;
    }
}
