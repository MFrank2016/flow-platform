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

package com.flow.platform.tree.yml;

import com.flow.platform.tree.Node;
import com.google.common.collect.Lists;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.DumperOptions.FlowStyle;
import org.yaml.snakeyaml.DumperOptions.LineBreak;
import org.yaml.snakeyaml.DumperOptions.ScalarStyle;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.representer.Representer;

/**
 * @author yang
 */
public class YmlHelper {

    private final static Constructor ROOT_YML_CONSTRUCTOR = new Constructor(RootYmlWrapper.class);

    private final static Representer ORDERED_SKIP_EMPTY_REPRESENTER = new OrderedSkipEmptyRepresenter();

    private final static DumperOptions DUMPER_OPTIONS = new DumperOptions();

    private final static LineBreak LINE_BREAK = LineBreak.getPlatformLineBreak();

    static {
        DUMPER_OPTIONS.setIndent(2);
        DUMPER_OPTIONS.setIndicatorIndent(0);
        DUMPER_OPTIONS.setExplicitStart(true);
        DUMPER_OPTIONS.setDefaultFlowStyle(FlowStyle.BLOCK);
        DUMPER_OPTIONS.setDefaultScalarStyle(ScalarStyle.PLAIN);
        DUMPER_OPTIONS.setLineBreak(LINE_BREAK);
    }

    /**
     * Create Node instance from yml
     */
    public static synchronized Node build(String yml) {
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

    public static synchronized String toYml(Node root) {
        RootNodeWrapper rootWrapper = RootNodeWrapper.fromNode(root);
        RootYmlWrapper ymlWrapper = new RootYmlWrapper(rootWrapper);

        Yaml yaml = new Yaml(ROOT_YML_CONSTRUCTOR, ORDERED_SKIP_EMPTY_REPRESENTER, DUMPER_OPTIONS);
        String dump = yaml.dump(ymlWrapper);
        dump = dump.substring(dump.indexOf(LINE_BREAK.getString()) + 1);
        return dump;
    }

    /**
     * Represent YML root flow
     */
    private static class RootYmlWrapper {

        public List<RootNodeWrapper> flow;

        public RootYmlWrapper() {
        }

        public RootYmlWrapper(RootNodeWrapper root) {
            this.flow = Lists.newArrayList(root);
        }
    }

    private static class RootNodeWrapper {

        private final static String DEFAULT_ROOT_NAME = "root";

        public static RootNodeWrapper fromNode(Node node) {
            RootNodeWrapper wrapper = new RootNodeWrapper();

            // set envs
            for (Map.Entry<String, String> entry : node.all()) {
                wrapper.envs.put(entry.getKey(), entry.getValue());
            }

            // set children
            for (Node child : node.getChildren()) {
                wrapper.steps.add(ChildNodeWrapper.fromNode(child));
            }

            return wrapper;
        }

        /**
         * Environment variables
         */
        public Map<String, String> envs = new LinkedHashMap<>();

        public List<ChildNodeWrapper> steps = new LinkedList<>();

        public Node toNode() {
            Node node = new Node(DEFAULT_ROOT_NAME);
            setEnvs(node);
            setChildren(node);
            return node;
        }

        protected void setEnvs(Node node) {
            for (Map.Entry<String, String> entry : envs.entrySet()) {
                node.put(entry.getKey(), entry.getValue());
            }
        }

        protected void setChildren(Node node) {
            for (ChildNodeWrapper child : steps) {
                node.getChildren().add(child.toNode());
            }
        }
    }

    private static class ChildNodeWrapper extends RootNodeWrapper {

        public static ChildNodeWrapper fromNode(Node node) {
            ChildNodeWrapper wrapper = new ChildNodeWrapper();

            // set envs
            for (Map.Entry<String, String> entry : node.all()) {
                wrapper.envs.put(entry.getKey(), entry.getValue());
            }

            wrapper.name = node.getName();
            wrapper.script = node.getContent();
            wrapper.plugin = node.getPlugin();
            wrapper.allowFailure = node.isAllowFailure() == Node.ALLOW_FAILURE_DEFAULT ? null : node.isAllowFailure();
            wrapper.isFinal = node.isFinal() == Node.IS_FINAL_DEFAULT ? null : node.isFinal();
            wrapper.condition = node.getCondition();

            for (Node child : node.getChildren()) {
                wrapper.steps.add(ChildNodeWrapper.fromNode(child));
            }

            return wrapper;
        }

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
