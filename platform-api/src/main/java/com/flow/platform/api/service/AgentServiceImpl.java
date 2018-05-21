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

package com.flow.platform.api.service;

import com.flow.platform.api.dao.job.JobDao;
import com.flow.platform.api.domain.agent.AgentItem;
import com.flow.platform.api.domain.agent.AgentSync;
import com.flow.platform.api.domain.job.Job;
import com.flow.platform.api.domain.job.JobStatus;
import com.flow.platform.api.domain.job.NodeStatus;
import com.flow.platform.api.domain.sync.SyncTask;
import com.flow.platform.api.events.AgentStatusChangeEvent;
import com.flow.platform.api.service.job.JobService;
import com.flow.platform.agent.manager.service.AgentCCService;
import com.flow.platform.core.service.ApplicationEventService;
import com.flow.platform.domain.Agent;
import com.flow.platform.domain.AgentStatus;
import com.flow.platform.util.CollectionUtil;
import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * @author yh@firim
 */

@Service
@Log4j2
public class AgentServiceImpl extends ApplicationEventService implements AgentService {

    @Value(value = "${api.zone.default}")
    private String zone;

    @Autowired
    private JobDao jobDao;

    @Autowired
    private JobService jobService;

    @Autowired
    private SyncService syncService;

    @Autowired
    private AgentCCService agentCCService;

    @Override
    public List<Agent> list() {
        return agentCCService.list(null);
    }

    @Override
    public List<AgentItem> listItems() {
        List<Agent> agents = agentCCService.list(null);

        // get all session id from agent collection
        List<String> sessionIds = CollectionUtil.toPropertyList("sessionId", agents);

        // get all running jobs from agent sessions
        List<Job> jobs = Collections.emptyList();
        if (!CollectionUtil.isNullOrEmpty(sessionIds)) {
            jobs = jobDao.list(sessionIds, NodeStatus.RUNNING);
        }

        // convert to session - job map
        Map<String, Job> sessionJobMap = CollectionUtil.toPropertyMap("sessionId", jobs);
        if (CollectionUtil.isNullOrEmpty(sessionJobMap)) {
            sessionJobMap = Collections.emptyMap();
        }

        // build agent item list
        List<AgentItem> list = new ArrayList<>(agents.size());

        for (Agent agent : agents) {
            // add offline agent
            if (agent.getStatus() == AgentStatus.OFFLINE) {
                list.add(new AgentItem(agent, null));
                continue;
            }

            // add agent without session id
            if (Strings.isNullOrEmpty(agent.getSessionId())) {
                list.add(new AgentItem(agent, null));
                continue;
            }

            // add agent which related a job by session id
            Job job = sessionJobMap.get(agent.getSessionId());
            if (job != null) {
                list.add(new AgentItem(agent, job));
                continue;
            }

            // add agent which related sync task by agent path
            SyncTask syncTask = syncService.getSyncTask(agent.getPath());
            if (syncTask != null) {
                AgentItem item = new AgentItem(agent, null);
                item.setSync(new AgentSync(syncTask.getTotal(), syncTask.getSyncQueue().size()));
                list.add(item);
            }

            list.add(new AgentItem(agent, null));
        }

        return list;
    }

    @Override
    public void onAgentStatusChange(Agent agent) {
        this.dispatchEvent(new AgentStatusChangeEvent(this, agent));
        handleAgentOnSyncService(agent);
        handleAgentOnJobService(agent);
    }

    private void handleAgentOnSyncService(final Agent agent) {
        if (agent.getStatus() == AgentStatus.IDLE) {
            syncService.register(agent.getPath());
        } else if (agent.getStatus() == AgentStatus.OFFLINE) {
            syncService.remove(agent.getPath());
        }
    }

    private void handleAgentOnJobService(final Agent agent) {
        // do not check related job if agent status not offline
        if (agent.getStatus() != AgentStatus.OFFLINE) {
            return;
        }

        // find related job and set job to failure
        String sessionId = agent.getSessionId();
        if (Strings.isNullOrEmpty(sessionId)) {
            return;
        }

        // find agent related job by session id
        Job job = jobService.find(sessionId);
        if (job == null) {
            return;
        }

        if (Job.RUNNING_STATUS.contains(job.getStatus())) {
            job.setFailureMessage(String.format("Agent %s is offline when job running", agent.getPath()));
            jobService.updateJobStatusAndSave(job, JobStatus.FAILURE);
        }
    }
}
