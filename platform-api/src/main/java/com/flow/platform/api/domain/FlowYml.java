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

import com.flow.platform.domain.Jsonable;
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
@EqualsAndHashCode(of = {"flowId"}, callSuper = false)
@ToString(of = {"flowId"})
public final class FlowYml extends Jsonable {

    @Getter
    @Setter
    private Long flowId;

    @Getter
    @Setter
    private String content = StringUtil.EMPTY;

    public FlowYml(Flow flow) {
        this.flowId = flow.getId();
    }

    public FlowYml(Flow flow, String content) {
        this(flow);
        this.content = content;
    }

    public boolean isEmpty() {
        return Objects.isNull(content) || content.isEmpty();
    }
}
