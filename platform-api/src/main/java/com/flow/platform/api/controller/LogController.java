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

package com.flow.platform.api.controller;

import com.flow.platform.core.exception.IllegalParameterException;
import com.flow.platform.domain.v1.Cmd;
import java.util.Objects;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author yh@fir.im
 */

@RestController
@RequestMapping("log")
public class LogController {

    /**
     * Upload zipped cmd log with multipart
     *
     * @param cmdJson
     * @param file zipped cmd log with application/zip
     */
    @PostMapping(path = "/upload")
    public void uploadFullLog(@RequestPart String cmdJson, @RequestPart MultipartFile file) {
        if (!Objects.equals(file.getContentType(), "application/zip")) {
            throw new IllegalParameterException("Illegal zipped log file format");
        }

        Cmd cmd = Cmd.parse(cmdJson, Cmd.class);

    }
}
