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

import com.google.common.base.Strings;
import groovy.util.ScriptException;
import java.util.Map;
import java.util.Objects;

/**
 * @author yang
 */
public class TreeManager {

    private final static int DEFAULT_GROOVY_SCRIPT_TIMEOUT = 10;

    private final NodeTree tree;

    public TreeManager(NodeTree tree) {
        this.tree = tree;
    }

    /**
     * Execute tree from node
     * @param startNode
     * @param consumer real node executing method
     * @exception ScriptException Groovy script exception
     */
    public void execute(Node startNode, NodeConsumer consumer) {
        if (Objects.isNull(startNode)) {
            throw new IllegalArgumentException("Start node cannot be null");
        }

        try {
            if (!executeGroovyScript(startNode)) {
                startNode.setStatus(NodeStatus.SKIP);
            }

            startNode.setStatus(NodeStatus.RUNNING);

            if (!Objects.isNull(consumer)) {
                consumer.accept(startNode, tree.getSharedContext());
            }

        } catch (ScriptException e) {
            throw new IllegalStateException("The groovy script error: " + e.getMessage());
        }
    }

    /**
     * Tell node tree the node been executed
     *
     * @param finishNode
     * @return next node
     */
    public Node onFinish(Node finishNode) {
        // 1. change finish node status
        // 2. update parent node status
        // 3. update shared context
        // 4. return next node
        throw new UnsupportedOperationException();
    }

    /**
     * Tell node tree the node been executed with error
     *
     * @param errorNode
     * @return next node
     */
    public Node onError(Node errorNode) {
        // 1. change finish node status
        // 2. update parent node status
        // 3. update shared context
        // 4. return next node
        throw new UnsupportedOperationException();
    }

    /**
     *  Update parent node status and context
     */
    private void updateParent(Node currentNode) {
        throw new UnsupportedOperationException();
    }

    /**
     * Execute groovy script of node
     */
    private boolean executeGroovyScript(Node node) throws ScriptException {
        String conditionScript = node.getCondition();
        if (Strings.isNullOrEmpty(conditionScript)) {
            return true;
        }

        GroovyRunner<Boolean> runner = GroovyRunner.create(DEFAULT_GROOVY_SCRIPT_TIMEOUT);
        for (Map.Entry<String, String> entry : node.all()) {
            runner.putVariable(entry.getKey(), entry.getValue());
        }

        return runner.setScript(node.getCondition()).run();
    }
}
