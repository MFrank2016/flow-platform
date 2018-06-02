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

package com.flow.platform.agent.config;

import com.google.common.base.Strings;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author yang
 */
@ToString
public final class UpstreamUrlConfig {

    @Getter
    @Setter
    private String websocket;

    @Getter
    @Setter
    private String cmdLog;

    public boolean hasWebsocket() {
        return !Strings.isNullOrEmpty(websocket);
    }

    public boolean hasCmdLog() {
        return !Strings.isNullOrEmpty(cmdLog);
    }
}
