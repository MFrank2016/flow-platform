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

package com.flow.platform.domain.v1;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

/**
 * @author yang
 */
public final class ExecutedCmd extends Cmd {

    public static ExecutedCmd transfer(Cmd cmd) {
        ExecutedCmd r = new ExecutedCmd();
        r.setId(cmd.getId());
        r.setMeta(cmd.getMeta());
        r.setStatus(cmd.getStatus());
        r.setContent(cmd.getContent());
        r.setTimeout(cmd.getTimeout());
        r.setContext(cmd.getContext());
        r.setType(cmd.getType());
        r.setWorkDir(cmd.getWorkDir());
        r.setOutputFilter(cmd.getOutputFilter());
        return r;
    }

    /**
     * Exit code for script content
     */
    @Getter
    @Setter
    private Integer code;

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

    /**
     * Start time
     */
    @Getter
    @Setter
    private ZonedDateTime startAt;

    /**
     * Output environment variable
     */
    @Getter
    @Setter
    private Map<String, String> output = new HashMap<>();

}
