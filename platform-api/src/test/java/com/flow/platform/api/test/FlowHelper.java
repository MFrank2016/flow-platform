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

package com.flow.platform.api.test;

import com.flow.platform.api.config.AppConfig;
import com.flow.platform.api.domain.Flow;
import com.flow.platform.api.service.v1.FlowService;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author yang
 */
@Component
public class FlowHelper {

    @Autowired
    private FlowService flowService;

    public Flow createFlowWithYml(String flowName, String ymlResourceName) throws IOException {
        Flow flow = flowService.save(flowName);
        String yml = getResourceContent(ymlResourceName);
        flowService.updateYml(flow.getName(), yml);
        return flow;
    }

    private String getResourceContent(String fileName) throws IOException {
        ClassLoader classLoader = TestBase.class.getClassLoader();
        URL resource = classLoader.getResource(fileName);
        File path = new File(resource.getFile());
        return Files.toString(path, AppConfig.DEFAULT_CHARSET);
    }
}
