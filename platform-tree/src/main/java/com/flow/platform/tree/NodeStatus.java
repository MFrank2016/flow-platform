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

/**
 * @author yang
 */
public enum NodeStatus {

    /**
     * Init status for node
     */
    PENDING(0),

    /**
     * Cannot execute the node
     */
    SKIP(1),

    /**
     * Running status for node
     */
    RUNNING(2),

    /**
     * Node executed successfully
     */
    SUCCESS(10),

    /**
     * Node executed with error
     */
    FAILURE(20),

    /**
     * Node stopped
     */
    KILLED(30);

    private int code;

    NodeStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
