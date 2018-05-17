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

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author yang
 */
@NoArgsConstructor
public class Result extends Context {

    @Getter
    @Setter
    private NodePath path;

    /**
     * Exit code for script content
     */
    @Getter
    @Setter
    private int code;

    /**
     * The script execute duration
     */
    @Getter
    @Setter
    private Long duration = 0L;

    /**
     * Extra message for error
     */
    @Getter
    @Setter
    private String errMsg;

    public Result(NodePath path, int code) {
        this.path = path;
        this.code = code;
    }
}
