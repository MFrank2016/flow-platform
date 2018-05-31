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

import com.flow.platform.api.domain.v1.JobTree;
import com.flow.platform.api.exception.AgentNotAvailableException;
import com.flow.platform.domain.Agent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author yh@fir.im
 */

@Service
public class JobTreeDispatchServiceImpl implements JobTreeDispatchService {

    @Autowired
    private AgentManagerService agentManagerService;

    @Override
    public void dispatch(JobTree jobTree) {

        // selected one agent to run job
        try {
            Agent agent = agentManagerService.acquire();
        } catch (AgentNotAvailableException e) {
            e.printStackTrace();
        }

    }
}
