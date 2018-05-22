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

package com.flow.platform.tree;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Tree context
 *
 * @author yang
 */
public class Context implements Serializable {

    protected final Map<String, String> context = new LinkedHashMap<>();

    public Set<Entry<String, String>> all() {
        return context.entrySet();
    }

    public Context put(String key, String value) {
        context.put(key, value);
        return this;
    }

    public String get(String key) {
        return context.get(key);
    }

    public Context remove(String key) {
        context.remove(key);
        return this;
    }

}
