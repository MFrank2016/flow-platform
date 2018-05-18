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

package com.flow.platform.api.service;

import com.flow.platform.api.domain.Flow;
import java.util.List;

/**
 * @author yang
 */
public interface FlowService {

    /**
     * Create a flow with name
     */
    Flow save(String name);

    /**
     * Get flow instance
     */
    Flow find(String name);

    /**
     * Delete flow
     */
    Flow delete(String name);

    /**
     * List flows for current user only or all
     */
    List<Flow> list(boolean isOnlyCurrentUser);

}
