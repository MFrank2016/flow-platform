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

import com.flow.platform.api.dao.v1.AgentDao;
import com.flow.platform.api.dao.v1.JobDao;
import com.flow.platform.api.dao.v1.JobTreeDao;
import com.flow.platform.api.domain.job.JobStatus;
import com.flow.platform.api.domain.v1.JobTree;
import com.flow.platform.api.domain.v1.JobV1;
import com.flow.platform.api.service.v1.AgentManagerService;
import com.flow.platform.api.service.v1.CmdManager;
import com.flow.platform.api.service.v1.JobService;
import com.flow.platform.domain.Agent;
import com.flow.platform.domain.CmdStatus;
import com.flow.platform.tree.Cmd;
import com.flow.platform.tree.Node;
import com.flow.platform.tree.TreeManager;
import com.flow.platform.tree.YmlEnvs;
import com.google.common.base.Strings;
import java.util.Objects;
import lombok.extern.log4j.Log4j2;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author yh@fir.im
 */
@Component
@Log4j2
public class CmdWebhookConsumer implements MessageListener {

    @Autowired
    private JobService jobServiceV1;

    @Autowired
    private JobTreeDao jobTreeDao;

    @Autowired
    private AgentManagerService agentManagerService;

    @Autowired
    private CmdManager cmdManager;

    @Autowired
    private AmqpTemplate jobCmdTemplate;

    @Autowired
    private AgentDao agentDao;

    @Autowired
    private JobDao jobDaoV1;

    @Override
    public void onMessage(Message message) {

        if (Objects.isNull(message)) {
            return;
        }
        String context = new String(message.getBody());

        if (Strings.isNullOrEmpty(context)) {
            return;
        }
        log.debug("Cmd Webhook Consumer received: {}", context);

        try {
            Cmd cmd = Cmd.parse(context, Cmd.class);

            JobV1 job = jobServiceV1.find(cmd.getJobKey());
            if (Objects.isNull(job)) {
                log.trace("Not found Job");
                return;
            }

            JobTree jobTree = jobTreeDao.get(cmd.getJobKey());
            if (Objects.isNull(jobTree)) {
                log.trace("Not found jobTree");
                return;
            }

            String token = cmd.get(YmlEnvs.AGENT_TOKEN);
            if (Strings.isNullOrEmpty(token)) {
                log.trace("Not found agent token");
                return;
            }

            Agent agent = agentDao.getByToken(token);
            if (Objects.isNull(agent)) {
                log.trace("Not found agent， token is " + token);
                return;
            }

            log.info("Cmd is " + cmd.getNodePath() + ", Cmd status is " + cmd.getStatus());

            TreeManager treeManager = new TreeManager(jobTree.getTree());
            if (cmd.getStatus() == CmdStatus.RUNNING) {
                job.setStatus(JobStatus.RUNNING);
                treeManager.execute(cmd.getNodePath(), null);
            }

            if (Cmd.FINISH_STATUS.contains(cmd.getStatus())) {
                Node nextNode = treeManager.onFinish(cmd.getResult());

                if (Objects.isNull(nextNode)) {
                    job.setStatus(JobStatus.SUCCESS);
                    agentManagerService.release(agent);
                }

                if (!Objects.isNull(nextNode)) {
                    Cmd nextCmd = cmdManager.create(cmd.getJobKey(), nextNode, agent.getToken());
                    jobCmdTemplate.send(agentManagerService.getQueueName(agent),
                        new Message(nextCmd.toBytes(), new MessageProperties()));
                }
            }

            // update job and tree
            jobTreeDao.update(jobTree);
            jobDaoV1.update(job);

        } catch (Throwable throwable) {
            throwable.printStackTrace();
            log.error("Handle message exception: " + throwable.getMessage());
        }

        log.trace("Handle message finish!");
    }
}