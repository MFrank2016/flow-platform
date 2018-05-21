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
import com.flow.platform.tree.NodePath;
import com.google.gson.annotations.Expose;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
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
@ToString(of = {"name"})
@EqualsAndHashCode(of = {"name"}, callSuper = false)
public final class Flow extends Jsonable {

    /**
     * Unique name of flow
     */
    @Expose
    private String name;

    /**
     * User defined context
     */
    @Expose
    @Getter
    @Setter
    private Map<String, String> context = new HashMap<>(10);

    @Expose
    @Getter
    @Setter
    private String createdBy;

    @Expose
    @Getter
    @Setter
    private ZonedDateTime createdAt;

    @Expose
    @Getter
    @Setter
    private ZonedDateTime updatedAt;

    public Flow(String name) {
        this.name = NodePath.create(name).toString();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = NodePath.create(name).toString();
    }

    public String getContext(String key) {
        return context.get(key);
    }

    public String getContext(String key, String defaultVal) {
        String val = context.get(key);
        return Objects.isNull(val) ? defaultVal : val;
    }

    public void putContext(String key, String val) {
        context.put(key, val);
    }

    public void removeContext(String key) {
        context.remove(key);
    }
}
