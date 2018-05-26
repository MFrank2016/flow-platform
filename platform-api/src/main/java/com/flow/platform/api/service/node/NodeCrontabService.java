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

import com.flow.platform.api.domain.v1.Flow;
import com.flow.platform.core.context.ContextEvent;
import java.util.List;
import org.quartz.Trigger;

/**
 * @author yang
 */
public interface NodeCrontabService extends ContextEvent {

    String KEY_BRANCH = "branch";

    String KEY_NODE_PATH = "node_path";

    /**
     * Set crontab task for node
     */
    void set(Flow flow);

    /**
     * Delete crontab task for node
     */
    void delete(Flow flow);

    /**
     * List current triggers
     */
    List<Trigger> triggers();

    /**
     * Clean triggers
     */
    void cleanTriggers();
}
