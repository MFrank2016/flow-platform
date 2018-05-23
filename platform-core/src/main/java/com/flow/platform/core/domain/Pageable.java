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

import com.flow.platform.util.StringUtil;
import java.util.Objects;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author gyfirim
 */
@NoArgsConstructor
public class Pageable {

    public final static Pageable DEFAULT = new Pageable(1, 20);

    @Getter
    @Setter
    private int number;

    @Getter
    @Setter
    private int size;

    public Pageable(int number, int size) {
        this.number = number;
        this.size = size;
    }

    public int getOffset() {
        return (number - 1) * size;
    }

    public boolean isEmpty() {
        return StringUtil.isNullOrEmptyForItems(
            String.valueOf(getNumber()), String.valueOf(getSize()))
            || (getSize() == 0 || getNumber() == 0);
    }

    public static boolean isEmpty(Pageable pageable) {
        return Objects.isNull(pageable) || pageable.isEmpty();
    }

}
