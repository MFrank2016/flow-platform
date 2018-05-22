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

package com.flow.platform.api.domain.v1;

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
@EqualsAndHashCode(of = {"flow", "number"}, callSuper = false)
public class JobKey extends Jsonable {

    @Getter
    @Setter
    private String flow;

    @Getter
    @Setter
    private Long number;

    public JobKey(String flow, Long number) {
        Objects.requireNonNull(flow);
        Objects.requireNonNull(number);

        this.flow = flow;
        this.number = number;
    }
}
