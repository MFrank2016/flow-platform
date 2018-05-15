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

import com.flow.platform.tree.Node;
import com.flow.platform.tree.NodePath;
import com.flow.platform.tree.NodeTree;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author yang
 */
public class NodeTreeTest {

    private NodeTree tree;

    @Before
    public void init() {
        Node root = new Node(NodePath.create("root"));
        Assert.assertNotNull(root);
        Assert.assertEquals("root", root.getName());
        Assert.assertEquals("root", root.getPath().toString());

        Node childFirst = new Node("child-1");
        childFirst.getChildren().add(new Node("child-1-1"));

        Node childSecond = new Node("child-2");

        root.getChildren().add(childFirst);
        root.getChildren().add(childSecond);

        tree = NodeTree.create(root);

        Assert.assertEquals(root, childFirst.getParent());
        Assert.assertEquals(root, childSecond.getParent());
    }

    @Test
    public void should_find_node_relation_for_parent() {
        Assert.assertNull(tree.parent(tree.getRoot().getPath()));

        Assert.assertEquals(
            tree.get(NodePath.create("root/child-1")),
            tree.parent(NodePath.create("root/child-1/child-1-1")));

        Assert.assertEquals(tree.getRoot(), tree.parent(NodePath.create("root/child-2")));
    }

    @Test
    public void should_find_node_relation_for_next() {
        Node nextOfRoot = tree.next(tree.getRoot().getPath());
        Assert.assertEquals(tree.get(NodePath.create("root/child-1/child-1-1")), nextOfRoot);

        Node nextOfFirstChild = tree.next(NodePath.create("root", "child-1"));
        Assert.assertEquals(tree.get(NodePath.create("root/child-2")), nextOfFirstChild);

        Node nextOfSecondChild = tree.next(NodePath.create("root/child-2"));
        Assert.assertNull(nextOfSecondChild);
    }

    @Test
    public void should_find_node_relation_for_prev() {
        Node prevOfRoot = tree.prev(tree.getRoot().getPath());
        Assert.assertNull(prevOfRoot);

        Node prevOfFirstChild = tree.prev(NodePath.create("root", "child-1"));
        Assert.assertEquals(tree.get(NodePath.create("root/child-1/child-1-1")), prevOfFirstChild);

        Node prevOfSecondChild = tree.prev(NodePath.create("root/child-2"));
        Assert.assertEquals(tree.get(NodePath.create("root/child-1")), prevOfSecondChild);
    }
}
