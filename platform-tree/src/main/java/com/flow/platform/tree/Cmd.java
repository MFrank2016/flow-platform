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

package com.flow.platform.tree;

import com.flow.platform.domain.CmdStatus;
import com.flow.platform.domain.CmdType;
import com.flow.platform.domain.Jsonable;
import com.google.common.collect.Sets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @author yh@fir.im
 */
@NoArgsConstructor
@ToString
@EqualsAndHashCode
public class Cmd extends Jsonable {

    /**
     * Finish status set
     */
    public static final Set<CmdStatus> FINISH_STATUS =
        Sets.newHashSet(CmdStatus.LOGGED, CmdStatus.EXCEPTION, CmdStatus.KILLED, CmdStatus.REJECTED,
            CmdStatus.TIMEOUT_KILL, CmdStatus.STOPPED);

    public static final Set<CmdStatus> FAILURE_STATUS =
        Sets.newHashSet(CmdStatus.EXCEPTION, CmdStatus.KILLED, CmdStatus.REJECTED,
            CmdStatus.TIMEOUT_KILL, CmdStatus.STOPPED);

    @Getter
    @Setter
    private NodePath nodePath;

    @Getter
    @Setter
    private String jobKey;

    @Getter
    @Setter
    private String content;

    @Getter
    @Setter
    private Map<String, String> context = new HashMap<>();

    /**
     * Command type (Required)
     */
    @Setter
    @Getter
    private CmdType type = CmdType.RUN_SHELL;

    /**
     * record current status
     */
    @Setter
    @Getter
    private CmdStatus status = CmdStatus.PENDING;

    @Setter
    @Getter
    private Result result;

    public String get(YmlEnvs ymlEnvs) {
        return context.get(ymlEnvs.name());
    }

    public String put(YmlEnvs ymlEnvs, String content) {
        return context.put(ymlEnvs.name(), content);
    }
}
