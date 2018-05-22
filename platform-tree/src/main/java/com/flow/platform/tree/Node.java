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

package com.flow.platform.tree;

import java.util.LinkedList;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author yang
 */
@ToString(of = {"path"})
@EqualsAndHashCode(of = {"path"}, callSuper = false)
public final class Node extends Context {

    /**
     * Name of node
     */
    @Getter
    private String name;

    /**
     * Path of current node
     */
    @Getter
    @Setter
    private NodePath path;

    /**
     * Status of node
     */
    @Getter
    @Setter
    private NodeStatus status = NodeStatus.PENDING;

    @Getter
    @Setter
    private String content;

    /**
     * Groovy condition script
     */
    @Getter
    @Setter
    private String condition;

    /**
     * Plugin name
     */
    @Getter
    @Setter
    private String plugin;

    /**
     * Allow failure on node
     */
    @Getter
    @Setter
    private boolean allowFailure;

    @Getter
    @Setter
    private boolean isFinal;

    @Getter
    @Setter
    private Node parent;

    /**
     * Children nodes
     */
    @Getter
    private List<Node> children = new LinkedList<>();

    /**
     * Create node with name
     */
    public Node(String name) {
        this.name = name;
        this.path = NodePath.create(name);
    }

    /**
     * Create node with path
     */
    public Node(NodePath path) {
        this.name = path.name();
        this.path = path;
    }
}
