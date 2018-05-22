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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.error.YAMLException;

/**
 * @author yang
 */
public class YmlHelper {

    private final static Constructor ROOT_YML_CONSTRUCTOR = new Constructor(RootYmlWrapper.class);

    /**
     * Create Node instance from yml
     */
    public static Node buildFromYml(String yml) {
        Yaml yaml = new Yaml(ROOT_YML_CONSTRUCTOR);
        RootYmlWrapper node = yaml.load(yml);

        // verify flow node
        if (Objects.isNull(node.flow)) {
            throw new YAMLException("The 'flow' content must be defined");
        }

        // current version only support single flow
        if (node.flow.size() > 1) {
            throw new YAMLException("Unsupported multiple flows definition");
        }

        // steps must be provided
        RootNodeWrapper flow = node.flow.get(0);
        List<ChildNodeWrapper> steps = flow.steps;

        if (Objects.isNull(steps) || steps.isEmpty()) {
            throw new YAMLException("The 'step' must be defined");
        }

        return flow.toNode();
    }

    /**
     * Represent YML root flow
     */
    private static class RootYmlWrapper {

        public List<RootNodeWrapper> flow;
    }

    private static class RootNodeWrapper {

        private final static String DEFAULT_ROOT_NAME = "root";

        /**
         * Environment variables
         */
        public Map<String, String> envs;

        public List<ChildNodeWrapper> steps;

        public Node toNode() {
            Node node = new Node(DEFAULT_ROOT_NAME);
            setEnvs(node);
            setChildren(node);
            return node;
        }

        protected void setEnvs(Node node) {
            if (Objects.isNull(envs)) {
                return;
            }

            for (Map.Entry<String, String> entry : envs.entrySet()) {
                node.put(entry.getKey(), entry.getValue());
            }
        }

        protected void setChildren(Node node) {
            if (Objects.isNull(steps)) {
                return;
            }

            for (ChildNodeWrapper child : steps) {
                node.getChildren().add(child.toNode());
            }
        }
    }

    private static class ChildNodeWrapper extends RootNodeWrapper {

        public String name;

        public String script;

        public String plugin;

        public Boolean allowFailure = false;

        public Boolean isFinal = false;

        public String condition;

        @Override
        public Node toNode() {
            Node node = new Node(name);
            node.setContent(script);
            node.setPlugin(plugin);
            node.setAllowFailure(allowFailure);
            node.setCondition(condition);
            node.setFinal(isFinal);
            setEnvs(node);
            setChildren(node);
            return node;
        }
    }
}
