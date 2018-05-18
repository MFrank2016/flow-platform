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

package com.flow.platform.api.test.dao;

import com.flow.platform.api.config.AppConfig;
import com.flow.platform.api.domain.Flow;
import com.flow.platform.api.domain.FlowYml;
import com.flow.platform.api.domain.node.Yml;
import com.flow.platform.api.test.TestBase;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author yh@firim
 */
public class FlowYmlDaoTest extends TestBase {

    @Test
    public void should_save_and_get_yml_success() throws IOException {
        ClassLoader classLoader = FlowYmlDaoTest.class.getClassLoader();
        URL resource = classLoader.getResource("yml/flow.yaml");

        File path = new File(resource.getFile());
        String ymlContent = Files.toString(path, AppConfig.DEFAULT_CHARSET);
        FlowYml storage = new FlowYml("flow", ymlContent);
        ymlDao.save(storage);

        FlowYml yml = ymlDao.get("flow");
        Assert.assertNotNull(yml);
        Assert.assertEquals(ymlContent, yml.getContent());
    }

    @Test
    public void should_delete_success() {
        FlowYml storage = new FlowYml("flow", "Yml Body");
        ymlDao.save(storage);
        Assert.assertNotNull(ymlDao.get("flow"));

        ymlDao.delete(new FlowYml("flow", null));
        Assert.assertNull(ymlDao.get("flow"));
    }
}
