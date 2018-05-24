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

package com.flow.platform.core.domain;

import com.flow.platform.domain.Jsonable;
import com.google.gson.annotations.Expose;
import java.util.Collections;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author gyfirim
 */
@NoArgsConstructor
public class Page<T> extends Jsonable {

    @Expose
    @Getter
    @Setter
    private List<T> content = Collections.emptyList();

    @Expose
    @Getter
    @Setter
    private long totalSize;

    // page index
    @Expose
    @Getter
    @Setter
    private int pageNumber;

    @Expose
    @Getter
    @Setter
    private int pageSize;

    @Expose
    @Getter
    @Setter
    private int pageCount;

    public Page(List<T> content, int pageSize, int number, long totalSize) {
        this.content = content;
        this.totalSize = totalSize;
        this.pageSize = pageSize;
        this.pageNumber = number;
        this.pageCount = pageSize == 0 ? 1 : (int) Math.ceil((double) this.totalSize / (double)pageSize);
    }

    public Page(List<T> content, Pageable pageable, long totalSize) {
        this(content, pageable.getSize(), pageable.getNumber(), totalSize);
    }
}
