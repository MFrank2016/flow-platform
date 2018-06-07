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

package com.flow.platform.api.consumer.v1;

import com.flow.platform.api.config.QueueConfig;
import com.flow.platform.api.domain.job.JobStatus;
import com.flow.platform.api.domain.v1.JobKey;
import com.flow.platform.api.domain.v1.JobV1;
import com.flow.platform.api.envs.EnvUtil;
import com.flow.platform.api.service.v1.AgentService;
import com.flow.platform.api.service.v1.JobNodeManager;
import com.flow.platform.api.service.v1.JobService;
import com.flow.platform.core.exception.IllegalStatusException;
import com.flow.platform.core.service.ApplicationEventService;
import com.flow.platform.domain.Agent;
import com.flow.platform.domain.v1.CmdMeta;
import com.flow.platform.domain.v1.ExecutedCmd;
import com.flow.platform.tree.Node;
import com.flow.platform.tree.NodePath;
import com.flow.platform.tree.NodeStatus;
import com.flow.platform.tree.Result;
import com.flow.platform.util.ObjectUtil;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import lombok.extern.log4j.Log4j2;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Receive cmd result from cmd callback queue
 *
 * @author yh@fir.im
 */
@Component
@Log4j2
public class CmdCallbackConsumer extends ApplicationEventService {

    private final static Map<NodeStatus, JobStatus> STATUS_MAP = new HashMap<>(5);

    static {
        STATUS_MAP.put(NodeStatus.PENDING, JobStatus.CREATED);
        STATUS_MAP.put(NodeStatus.SUCCESS, JobStatus.SUCCESS);
        STATUS_MAP.put(NodeStatus.FAILURE, JobStatus.FAILURE);
        STATUS_MAP.put(NodeStatus.KILLED, JobStatus.STOPPED);
        STATUS_MAP.put(NodeStatus.RUNNING, JobStatus.RUNNING);
    }

    @Autowired
    private JobService jobServiceV1;

    @Autowired
    private JobNodeManager jobNodeManager;

    @Autowired
    private AgentService agentService;

    @RabbitListener(queues = QueueConfig.CMD_CALLBACK_QUEUE_NAME)
    public void handleMessage(byte[] data) {
        ExecutedCmd cmd = (ExecutedCmd) ObjectUtil.fromBytes(data);
        if (Objects.isNull(cmd)) {
            log.warn("Cmd webhook consumer cannot parse the data: " + new String(data));
            return;
        }

        log.debug("Cmd Webhook Consumer received: {}", cmd);

        if (!cmd.isExecuted()) {
            return;
        }

        JobV1 job = null;

        try {
            JobKey jobKey = JobKey.create(cmd.getMeta().get(CmdMeta.META_JOB_KEY));
            NodePath nodePath = NodePath.create(cmd.getMeta().get(CmdMeta.META_JOB_NODE_PATH));
            String token = cmd.getMeta().get(CmdMeta.META_AGENT_TOKEN);
            job = jobServiceV1.find(jobKey);

            Agent agent = agentService.find(token);
            log.info("Cmd is " + nodePath + ", Cmd status is " + cmd.getStatus());

            // update nodes and parent for finish and get next node
            Node next = jobNodeManager.finish(job, nodePath, toResult(cmd, nodePath));

            // No more available node
            if (Objects.isNull(next)) {
                agentService.release(agent);
                Node root = jobNodeManager.root(job);
                JobStatus newStatus = toJobStatus(root);
                jobServiceV1.setStatus(job, newStatus);
                return;
            }

            // has node to execute
            jobNodeManager.execute(job, next.getPath(), agent);
            log.trace("Handle message finish!");

        } catch (Throwable throwable) {
            log.error("Handle message exception: " + throwable.getMessage());

            if (!Objects.isNull(job)) {
                jobServiceV1.setStatus(job, JobStatus.FAILURE);
            }
        }
    }

    private Result toResult(ExecutedCmd cmd, NodePath path) {
        Result result = new Result();
        result.setCode(cmd.getCode());
        result.setDuration(cmd.getDuration());
        result.setErrMsg(cmd.getErrMsg());
        result.setPath(path);

        EnvUtil.merge(cmd.getOutput(), result.getContext(), false);
        return result;
    }

    /**
     * Translate root node status to job status
     */
    private JobStatus toJobStatus(Node root) {
        JobStatus jobStatus = STATUS_MAP.get(root.getStatus());
        if (Objects.isNull(jobStatus)) {
            throw new IllegalStatusException("Root status cannot handle : " + root.getStatus());
        }
        return jobStatus;
    }
}