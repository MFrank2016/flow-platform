/*
 * Copyright 2018 fir.im
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

import com.flow.platform.api.domain.v1.JobNodeResult;
import com.flow.platform.api.domain.v1.JobV1;
import com.flow.platform.domain.Agent;
import com.flow.platform.tree.Node;
import com.flow.platform.tree.NodePath;
import com.flow.platform.tree.Result;
import java.util.List;

/**
 * To handle method of node tree for job
 *
 * @author yang
 */
public interface JobNodeManager {

    /**
     * Get root job node
     */
    Node root(JobV1 job);

    /**
     * Get job node by path
     */
    Node get(JobV1 job, NodePath path);

    /**
     * Get next node by path
     */
    Node next(JobV1 job, NodePath path);

    /**
     * Execute node for job, update node status on tree
     */
    void execute(JobV1 job, NodePath path, Agent agent);

    /**
     * Finish node for job and return next Node
     */
    Node finish(JobV1 job, NodePath path, Result result);

    /**
     * Get result list by job
     */
    List<JobNodeResult> resultList(JobV1 job);
}
