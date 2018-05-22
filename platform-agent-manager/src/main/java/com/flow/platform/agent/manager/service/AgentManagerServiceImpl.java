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
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.extern.log4j.Log4j2;
import org.apache.zookeeper.data.Stat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
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
            saveWithStatus(exist, AgentStatus.OFFLINE);
            return;
        }

        // create new agent with idle status
        if (exist == null) {
            try {
                exist = create(path, null);
                log.trace("Create agent {} from report", path);
            } catch (DataIntegrityViolationException ignore) {
                // agent been created at some other threads
                return;
            }
        }

        // update exist offline agent to idle status
        if (exist.getStatus() == AgentStatus.OFFLINE) {
            exist.setSessionId(null);
            saveWithStatus(exist, AgentStatus.IDLE);
        }

        // do not update agent status when its busy
        if (exist.getStatus() == AgentStatus.BUSY) {
            // do nothing
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Agent find(AgentPath key) {
        return agentDao.get(key);
    }

    @Override
    @Transactional(readOnly = true)
    public Agent find(String sessionId) {
        return agentDao.get(sessionId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Agent> findAvailable(String zone) {
        return agentDao.list(zone, "updatedDate", AgentStatus.IDLE);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Agent> listForOnline(String zone) {
        return agentDao.list(zone, "createdDate", AgentStatus.IDLE, AgentStatus.BUSY);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Agent> list(String zone) {
        if (Strings.isNullOrEmpty(zone)) {
            return agentDao.list();
        }
        return agentDao.list(zone, "createdDate");
    }

    @Override
    public void saveWithStatus(Agent agent, AgentStatus status) {
        if (agent == null || status == null) {
            return;
        }

        if (!agentDao.exist(agent.getPath())) {
            throw new AgentErr.NotFoundException(agent.getName());
        }

        boolean statusIsChanged = !agent.getStatus().equals(status);

        agent.setStatus(status);
        agentDao.update(agent);
        log.trace("Agent status been updated to '{}'", status);

        // send webhook if status changed
        if (statusIsChanged) {
            this.webhookCallback(agent);
        }

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
        agent.setStatus(AgentStatus.OFFLINE);
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

    public List<Agent> agentsFromZookeeper() {

        String statusPath = statusNode();
        List<Agent> agents = agentDao.list();
        List<String> children = zkClient.getChildren(statusPath);

        for (String child : children) {
            String childPath = statusPath + "/" + child;

            AgentPath agentPath = new AgentPath(child.split(SPLITCHARS)[0], child.split(SPLITCHARS)[1]);
            Agent agent = findByPath(agents, agentPath);
            if (!Objects.isNull(agent)) {
                Stat stat = new Stat();
                agent.setStatus(AgentStatus.valueOf(new String(zkClient.getData(childPath, stat))));
            }
        }
        return agents;
    }

    private String statusNode() {
        return "/" + rootNodeName + "/" + STATUS;
    }

    private Agent findByPath(List<Agent> agents, AgentPath agentPath) {
        for (Agent agent : agents) {
            if (Objects.equals(agent.getPath(), agentPath)) {
                return agent;
            }
        }
        return null;
    }
}
