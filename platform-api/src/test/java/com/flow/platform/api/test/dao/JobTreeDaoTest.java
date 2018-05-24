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

package com.flow.platform.api.test.dao;

import com.flow.platform.api.domain.v1.JobKey;
import com.flow.platform.api.domain.v1.JobTree;
import com.flow.platform.api.test.FlowHelper;
import com.flow.platform.api.test.TestBase;
import com.flow.platform.tree.Node;
import com.flow.platform.tree.NodePath;
import com.flow.platform.tree.NodeStatus;
import com.flow.platform.tree.NodeTree;
import com.flow.platform.tree.yml.YmlHelper;
import java.io.IOException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author yang
 */
public class JobTreeDaoTest extends TestBase {

    @Autowired
    private FlowHelper flowHelper;

    private JobKey key = new JobKey(10L, 0L);

    @Before
    public void init() throws IOException {
        Node root = YmlHelper.build(flowHelper.getResourceContent("yml/for_job_test.yml"));
        JobTree jobTree = new JobTree(key, NodeTree.create(root));
        jobTreeDao.save(jobTree);
        Assert.assertNotNull(jobTreeDao.get(jobTree.getKey()));
    }

    @Test
    public void should_update_node_in_job_tree() {
        JobTree jobTree = jobTreeDao.get(key);
        NodeTree tree = jobTree.getTree();

        // when: set node status
        Node next = tree.next(tree.getRoot().getPath());
        next.setStatus(NodeStatus.RUNNING);
        jobTreeDao.update(jobTree);

        // then:
        NodeTree loaded = jobTreeDao.get(key).getTree();
        Assert.assertEquals(NodeStatus.RUNNING, loaded.next(NodePath.create("root")).getStatus());

        // when: set shared context
        loaded.getSharedContext().put("HELLO", "WORLD");
        jobTreeDao.update(new JobTree(key, loaded));

        // then:
        Assert.assertEquals("WORLD", jobTreeDao.get(key).getTree().getSharedContext().get("HELLO"));
    }

    @Test
    public void should_delete_by_flow_name() {
        jobTreeDao.deleteByFlow(key.getFlowId());
        Assert.assertEquals(0, jobTreeDao.list().size());
    }

}
