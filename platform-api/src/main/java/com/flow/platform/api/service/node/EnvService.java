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

package com.flow.platform.api.service.node;

import com.flow.platform.api.domain.Flow;
import com.flow.platform.api.domain.node.Node;
import java.util.Map;
import java.util.Set;

/**
 * @author yang
 */
public interface EnvService {

    /**
     * List all available env variable
     *
     * @param flow target node
     * @param editable is get editable or none editable env list
     */
    Map<String, String> list(Flow flow, boolean editable);

    /**
     * Add env variables to node
     * @param flow target node
     * @param envs env variables to add
     * @param verify is verify according to EnvKey properties
     */
    void save(Flow flow, Map<String, String> envs, boolean verify);

    /**
     * Del env variables from node
     * @param flow target node
     * @param keys env variables to delete
     * @param verify is verify according to EnvKey properties
     */
    void delete(Flow flow, Set<String> keys, boolean verify);

}
