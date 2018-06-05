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
import com.flow.platform.tree.NodeStatus;
import com.flow.platform.tree.Result;
import com.google.gson.annotations.Expose;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * @author yang
 */
@AllArgsConstructor
public final class JobNodeResult extends Jsonable {

    @Expose
    @Getter
    @Setter
    private Result detail;

    @Expose
    @Getter
    @Setter
    private NodeStatus status;
}
