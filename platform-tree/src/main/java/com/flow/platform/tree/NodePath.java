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
import com.google.common.collect.Lists;
import com.google.common.collect.Range;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.EqualsAndHashCode;

/**
 * @author yang
 */
@EqualsAndHashCode(of = {"pathInStr"})
public class NodePath {

    private final static String PATH_SEPARATOR = "/";

    private final static int MAX_DEPTH = 10;

    private final static Range<Integer> NAME_LENGTH_RANGE = Range.closed(1, 100);

    private final List<String> paths = new ArrayList<>(MAX_DEPTH);

    private final String pathInStr;

    public static NodePath create(String... nameOrPaths) {
        return new NodePath(Arrays.asList(nameOrPaths));
    }

    public static NodePath create(NodePath parentPath, String... nameOrPaths) {
        ArrayList<String> paths = new ArrayList<>(parentPath.paths.size() + nameOrPaths.length);
        paths.addAll(parentPath.paths);
        paths.addAll(Arrays.asList(nameOrPaths));
        return new NodePath(paths);
    }

    private NodePath(List<String> nameOrPaths) {
        for (String nameOrPath : nameOrPaths) {
            if (nameOrPath == null) {
                continue;
            }

            nameOrPath = nameOrPath.trim();

            if (nameOrPath.startsWith(PATH_SEPARATOR)) {
                nameOrPath = nameOrPath.substring(1);
            }

            // name include path separator
            String[] names = nameOrPath.split(PATH_SEPARATOR);
            if (names.length > 0) {
                for (String name : names) {
                    if (Strings.isNullOrEmpty(name.trim())) {
                        continue;
                    }
                    validateNodeName(name);
                    paths.add(name);
                }
                continue;
            }

            String name = nameOrPath;
            if (Strings.isNullOrEmpty(name)) {
                continue;
            }

            validateNodeName(name);
            paths.add(name);
        }

        if (paths.isEmpty()) {
            throw new IllegalArgumentException("Empty node path is not allowed");
        }

        if (paths.size() > MAX_DEPTH) {
            throw new IllegalArgumentException("Node path over the depth limit");
        }

        StringBuilder builder = new StringBuilder();
        for (String name : paths) {
            builder.append(name).append(PATH_SEPARATOR);
        }
        pathInStr = builder.deleteCharAt(builder.length() - 1).toString();
    }

    public NodePath parent() {
        if ((paths.size() - 1) >= 0) {
            paths.remove(paths.size() - 1);
            return new NodePath(paths);
        }
        return null;
    }

    public NodePath root() {
        return new NodePath(Lists.newArrayList(paths.get(0)));
    }

    public String name() {
        return paths.get(paths.size() - 1);
    }

    @Override
    public String toString() {
        return pathInStr;
    }

    private void validateNodeName(String name) throws IllegalArgumentException {
        String errMsg = "Illegal node name: " + name;

        name = name.trim();
        if (Strings.isNullOrEmpty(name) || name.startsWith(PATH_SEPARATOR)) {
            throw new IllegalArgumentException(errMsg);
        }

        if (!NAME_LENGTH_RANGE.contains(name.length())) {
            throw new IllegalArgumentException(errMsg);
        }

        if (name.contains("*")) {
            throw new IllegalArgumentException(errMsg);
        }
    }
}
