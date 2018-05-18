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

package com.flow.platform.tree.test;

import com.flow.platform.tree.Context;
import com.flow.platform.tree.Node;
import com.flow.platform.tree.NodePath;
import com.flow.platform.tree.Result;
import com.flow.platform.tree.NodeStatus;
import com.flow.platform.tree.NodeTree;
import com.flow.platform.tree.TreeManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/**
 * @author yang
 */
@FixMethodOrder(value = MethodSorters.JVM)
public class TreeManagerTest {

    private TreeManager manager;

    private NodeTree tree;

    @Before
    public void init() {
        Node root = new Node("root");

        Node childOne = new Node("child-1");
        childOne.getChildren().add(new Node("child-1-1"));
        root.getChildren().add(childOne);

        Node childTwo = new Node("child-2");
        root.getChildren().add(childTwo);

        Node childThree = new Node("child-3");
        root.getChildren().add(childThree);

        tree = NodeTree.create(root);
        manager = new TreeManager(tree);
    }

    @Test
    public void should_exec_node_tree_from_root_with_success_status() {
        NodePath pathToTest = NodePath.create("root/child-1/child-1-1");

        // the first node of tree status should be running and all parent node status should be running
        manager.execute(NodePath.create("root"), null);
        Assert.assertEquals(NodeStatus.RUNNING, tree.get(pathToTest).getStatus());
        Assert.assertEquals(NodeStatus.RUNNING, tree.get(pathToTest.parent()).getStatus());
        Assert.assertEquals(NodeStatus.RUNNING, tree.getRoot().getStatus());

        // when: tell tree manager node been executed
        Result nodeResult = new Result(pathToTest, 0);
        nodeResult.put("OUTPUT_1", "111").put("OUTPUT_2", "222");
        manager.onFinish(nodeResult);

        // then: verify shared context value
        Context sharedContext = tree.getSharedContext();
        Assert.assertEquals("111", sharedContext.get("OUTPUT_1"));
        Assert.assertEquals("222", sharedContext.get("OUTPUT_2"));

        // then: verify node status
        Assert.assertEquals(NodeStatus.SUCCESS, tree.get(pathToTest).getStatus());
        Assert.assertEquals(NodeStatus.SUCCESS, tree.get(NodePath.create("root/child-1")).getStatus());
        Assert.assertEquals(NodeStatus.RUNNING, tree.getRoot().getStatus());

        // then: mock to finish all children node for root and test their status
        NodePath childTwoPath = NodePath.create("root/child-2");
        manager.onFinish(new Result(childTwoPath, 0));
        Assert.assertEquals(NodeStatus.SUCCESS, tree.get(childTwoPath).getStatus());
        Assert.assertEquals(NodeStatus.RUNNING, tree.getRoot().getStatus());

        NodePath childThreePath = NodePath.create("root/child-3");
        manager.onFinish(new Result(childThreePath, 0));
        Assert.assertEquals(NodeStatus.SUCCESS, tree.get(childThreePath).getStatus());
        Assert.assertEquals(NodeStatus.SUCCESS, tree.getRoot().getStatus());
    }

    @Test
    public void should_get_failure_status_for_node() {
        Result mockResult = new Result(NodePath.create("root/child-1/child-1-1"), 1);
        Node nextNode = manager.onFinish(mockResult);

        Assert.assertEquals(NodeStatus.FAILURE, tree.get(NodePath.create("root/child-1/child-1-1")).getStatus());
        Assert.assertEquals(NodeStatus.FAILURE, tree.get(NodePath.create("root/child-1")).getStatus());
        Assert.assertEquals(NodeStatus.FAILURE, tree.get(NodePath.create("root")).getStatus());
        Assert.assertEquals(tree.get(NodePath.create("root/child-1")), nextNode);
    }

    @Test
    public void should_get_killed_status_for_node() {
        Result mockResult = new Result(NodePath.create("root/child-1/child-1-1"), 130);
        Node nextNode = manager.onFinish(mockResult);

        Assert.assertEquals(NodeStatus.KILLED, tree.get(NodePath.create("root/child-1/child-1-1")).getStatus());
        Assert.assertEquals(NodeStatus.KILLED, tree.get(NodePath.create("root/child-1")).getStatus());
        Assert.assertEquals(NodeStatus.KILLED, tree.get(NodePath.create("root")).getStatus());
        Assert.assertEquals(tree.get(NodePath.create("root/child-1")), nextNode);
    }
}
