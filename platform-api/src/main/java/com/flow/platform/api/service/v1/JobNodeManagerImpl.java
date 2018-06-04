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

import com.flow.platform.api.dao.v1.JobTreeDao;
import com.flow.platform.api.domain.v1.JobKey;
import com.flow.platform.api.domain.v1.JobTree;
import com.flow.platform.api.events.CmdSentEvent;
import com.flow.platform.core.service.ApplicationEventService;
import com.flow.platform.domain.Agent;
import com.flow.platform.domain.v1.Cmd;
import com.flow.platform.domain.v1.CmdMeta;
import com.flow.platform.tree.Context;
import com.flow.platform.tree.Node;
import com.flow.platform.tree.NodePath;
import com.flow.platform.tree.NodeTree;
import com.flow.platform.tree.Result;
import com.flow.platform.tree.TreeManager;
import com.flow.platform.util.ObjectUtil;
import java.util.Base64;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author yang
 */
@Component
public class JobNodeManagerImpl extends ApplicationEventService implements JobNodeManager {

    private final static String CMD_ID_SPLITTER = "@";

    @Autowired
    private JobTreeDao jobTreeDao;

    @Autowired
    private AmqpTemplate jobCmdTemplate;

    @Override
    public Node root(JobKey key) {
        return getTree(key).getRoot();
    }

    @Override
    public Node get(JobKey key, NodePath path) {
        return getTree(key).get(path);
    }

    @Override
    public Node next(JobKey key, NodePath path) {
        return getTree(key).next(path);
    }

    @Override
    public void execute(JobKey key, NodePath path, Agent agent) {
        JobTree jobTree = jobTreeDao.get(key);

        // TODO: should be cached
        TreeManager treeManager = new TreeManager(jobTree.getTree());
        treeManager.execute(path, null);

        Node node = jobTree.getTree().get(path);

        // create cmd and send it to agent cmd queue
        Cmd nextCmd = createCmd(key, node, jobTree.getTree().getSharedContext(), agent.getToken());
        jobCmdTemplate.send(agent.queueName(), new Message(ObjectUtil.toBytes(nextCmd), new MessageProperties()));
        this.dispatchEvent(new CmdSentEvent(this, nextCmd));

        jobTreeDao.update(jobTree);
    }

    @Override
    public Node finish(JobKey key, NodePath path, Result result) {
        JobTree jobTree = jobTreeDao.get(key);

        // TODO: should be cached
        TreeManager treeManager = new TreeManager(jobTree.getTree());
        treeManager.onFinish(result);

        jobTreeDao.update(jobTree);

        return next(key, path);
    }

    private NodeTree getTree(JobKey key) {
        return jobTreeDao.get(key).getTree();
    }

    private Cmd createCmd(JobKey key, Node node, Context sharedContext, String token) {
        // trans node to cmd
        Cmd cmd = new Cmd();
        cmd.setId(getId(key, node));
        cmd.setTimeout(1800);
        cmd.setContent(node.getContent());
        cmd.setWorkDir("/tmp");

        // set meta data
        cmd.getMeta().put(CmdMeta.META_JOB_KEY, key.getId());
        cmd.getMeta().put(CmdMeta.META_JOB_NODE_PATH, node.getPath().toString());
        cmd.getMeta().put(CmdMeta.META_AGENT_TOKEN, token);

        // set cmd context from shared context and node private context
        cmd.getContext().putAll(sharedContext.getContext());
        cmd.getContext().putAll(node.getContext());

        return cmd;
    }

    public String getId(JobKey key, Node node) {
        String source = key.getId() + CMD_ID_SPLITTER + node.getPath().toString();
        return Base64.getEncoder().encodeToString(source.getBytes());
    }
}
