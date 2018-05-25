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

import com.flow.platform.domain.Jsonable;
import java.util.Objects;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @author yang
 */
@ToString
@NoArgsConstructor
@EqualsAndHashCode(of = {"flowId", "number"}, callSuper = false)
public class JobKey extends Jsonable {

    @Getter
    @Setter
    private Long flowId;

    @Getter
    @Setter
    private Long number;

    public JobKey(Long flowId, Long number) {
        Objects.requireNonNull(flowId);
        Objects.requireNonNull(number);

        this.flowId = flowId;
        this.number = number;
    }
}
