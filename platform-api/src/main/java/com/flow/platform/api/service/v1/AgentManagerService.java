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

import com.flow.platform.api.exception.AgentNotAvailableException;
import com.flow.platform.domain.Agent;
import com.flow.platform.domain.AgentPath;
import com.flow.platform.domain.AgentSettings;
import java.util.List;

/**
 * @author yh@fir.im
 */
public interface AgentManagerService {

    /**
     * Create agent into database and create unique token
     */
    Agent create(AgentPath agentPath);

    /**
     * Find agent by agent path
     */
    Agent find(AgentPath agentPath);

    /**
     * Find agent by token
     */
    Agent find(String token);

    /**
     * List all agent
     */
    List<Agent> list();

    /**
     * Acquire available agent, throw exception when
     *
     * @throws AgentNotAvailableException
     */
    Agent acquire() throws AgentNotAvailableException;

    /**
     * Release agent, set status to idle
     */
    void release(Agent agent);

    /**
     * Get agent settings by token
     */
    AgentSettings settings(String token);

    /**
     * Get queue name for agent
     */
    String getQueueName(Agent agent);
}
