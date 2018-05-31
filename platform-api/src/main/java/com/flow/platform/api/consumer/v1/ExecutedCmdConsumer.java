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
import com.flow.platform.api.service.v1.AgentManagerService;
import com.flow.platform.api.service.v1.CmdManager;
import com.flow.platform.api.service.v1.JobNodeManager;
import com.flow.platform.api.service.v1.JobService;
import com.flow.platform.domain.Agent;
import com.flow.platform.domain.CmdStatus;
import com.flow.platform.domain.v1.Cmd;
import com.flow.platform.domain.v1.ExecutedCmd;
import com.flow.platform.tree.Node;
import com.flow.platform.tree.NodePath;
import com.flow.platform.tree.Result;
import com.google.common.base.Strings;
import java.util.Objects;
import lombok.extern.log4j.Log4j2;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author yh@fir.im
 */
@Component
@Log4j2
public class ExecutedCmdConsumer {

    @Autowired
    private JobService jobServiceV1;

    @Autowired
    private JobNodeManager jobNodeManager;

    @Autowired
    private AgentManagerService agentManagerService;

    @Autowired
    private CmdManager cmdManager;

    @Autowired
    private AmqpTemplate jobCmdTemplate;

    @RabbitListener(queues = QueueConfig.CMD_CALLBACK_QUEUE_NAME)
    public void handleMessage(ExecutedCmd cmd) {
        log.debug("Cmd Webhook Consumer received: {}", cmd);

        JobKey jobKey = JobKey.create(cmd.getMeta().get(CmdManager.META_JOB_KEY));
        NodePath nodePath = NodePath.create(cmd.getMeta().get(CmdManager.META_JOB_NODE_PATH));
        String token = cmd.getMeta().get(CmdManager.META_AGENT_TOKEN);

        try {

            JobV1 job = jobServiceV1.find(jobKey);
            if (Objects.isNull(job)) {
                log.trace("Not found Job");
                return;
            }

            if (Strings.isNullOrEmpty(token)) {
                log.trace("Not found agent token");
                return;
            }

            Agent agent = agentManagerService.find(token);

            log.info("Cmd is " + nodePath + ", Cmd status is " + cmd.getStatus());

            if (cmd.getStatus() == CmdStatus.RUNNING) {
                // do not handle it
                return;
            }

            if (Cmd.FINISH_STATUS.contains(cmd.getStatus())) {

                // update nodes and parent for finish
                jobNodeManager.finish(jobKey, nodePath, toResult(cmd));

                // find next available node
                Node next = jobNodeManager.next(jobKey, nodePath);

                // No more available node
                if (Objects.isNull(next)) {
                    agentManagerService.release(agent);
                    Node root = jobNodeManager.root(jobKey);
                    jobServiceV1.setStatus(job.getKey(), toJobStatus(root));
                    return;
                }

                // has node to execute
                Cmd nextCmd = cmdManager.create(jobKey, next, agent.getToken());
                String agentQueueName = agentManagerService.getQueueName(agent);
                jobCmdTemplate.send(agentQueueName, new Message(nextCmd.toBytes(), new MessageProperties()));
            }

        } catch (Throwable throwable) {
            throwable.printStackTrace();
            log.error("Handle message exception: " + throwable.getMessage());
        }

        log.trace("Handle message finish!");
    }

    private Result toResult(Cmd cmd) {
        return null;
    }

    /**
     * Translate root node status to job status
     */
    private JobStatus toJobStatus(Node root) {
        return null;
    }
}