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

import com.flow.platform.api.domain.v1.JobV1;
import com.flow.platform.domain.AgentStatus;
import com.flow.platform.domain.v1.JobKey;
import com.flow.platform.domain.Agent;
import com.flow.platform.domain.AgentPath;
import com.flow.platform.domain.AgentSettings;
import com.flow.platform.tree.Cmd;
import com.flow.platform.tree.Node;
import java.util.List;

/**
 * @author yh@fir.im
 */
public interface AgentManagerService {

    Agent create(AgentPath agentPath);

    Agent find(AgentPath agentPath);

    List<Agent> list();

    Agent selectAgent();

    AgentSettings settings(String token);

    String agentQueue(Agent agent);

    void handleJob(JobKey key);

    Cmd buildCmdFromNode(Node node, JobKey jobKey, Agent agent);

    void resetAgentStatus(AgentStatus agentStatus, Agent agent);
}
