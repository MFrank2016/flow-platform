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

import com.flow.platform.tree.Context;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @author yang
 */
@NoArgsConstructor
@ToString(of = {"name"})
@EqualsAndHashCode(of = {"name"}, callSuper = false)
public final class Flow {

    /**
     * Unique name of flow
     */
    @Getter
    @Setter
    private String name;

    @Setter
    @Getter
    private Map<String, String> context = new HashMap<>(10);

    @Getter
    @Setter
    private String createdBy;

    @Getter
    @Setter
    private ZonedDateTime createdAt;

    @Getter
    @Setter
    private ZonedDateTime updatedAt;

    public Flow(String name) {
        this.name = name;
    }

}
