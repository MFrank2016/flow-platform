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

package com.flow.platform.domain.v1;

import com.flow.platform.domain.CmdStatus;
import com.flow.platform.domain.CmdType;
import com.flow.platform.domain.Jsonable;
import com.google.common.collect.Sets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author yh@fir.im
 */
@ToString(of = {"type", "status"})
public class Cmd extends Jsonable {

    public static final Set<CmdStatus> FAILURE_STATUS = Sets.newHashSet(
        CmdStatus.EXCEPTION,
        CmdStatus.KILLED,
        CmdStatus.REJECTED,
        CmdStatus.TIMEOUT_KILL,
        CmdStatus.STOPPED
    );

    /**
     * Timeout in seconds
     */
    @Getter
    @Setter
    private Long timeout = 1800L;

    /**
     * Work directory
     */
    @Getter
    @Setter
    private String workDir;

    /**
     * Content will be executed
     */
    @Getter
    @Setter
    private String content;

    /**
     * Command type (Required)
     */
    @Getter
    @Setter
    private CmdType type = CmdType.RUN_SHELL;

    @Getter
    @Setter
    private CmdStatus status = CmdStatus.PENDING;

    /**
     * Meta data for extra information
     */
    @Getter
    @Setter
    private Map<String, String> meta = new HashMap<>();

    /**
     * Indicate cmd is executed >= level 2
     */
    public boolean isExecuted() {
        return this.status.getLevel() >= CmdStatus.EXECUTED.getLevel();
    }
}
