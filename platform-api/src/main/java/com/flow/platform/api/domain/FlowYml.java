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

package com.flow.platform.api.domain;

import com.flow.platform.util.StringUtil;
import java.util.Objects;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @author yang
 */
@NoArgsConstructor
@EqualsAndHashCode(of = {"name"})
@ToString(of = {"name"})
public class FlowYml {

    @Getter
    @Setter
    private String name;

    @Getter
    @Setter
    private String content = StringUtil.EMPTY;

    public FlowYml(String name) {
        this.name = name;
    }

    public FlowYml(String name, String content) {
        this.name = name;
        this.content = content;
    }

    public boolean isEmpty() {
        return Objects.isNull(content) || content.isEmpty();
    }
}
