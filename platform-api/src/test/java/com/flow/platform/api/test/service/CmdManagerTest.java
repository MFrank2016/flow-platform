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

package com.flow.platform.api.test.service;

import com.flow.platform.api.domain.v1.JobKey;
import com.flow.platform.api.service.v1.CmdManager;
import com.flow.platform.api.test.TestBase;
import com.flow.platform.tree.Node;
import java.io.UnsupportedEncodingException;
import java.util.Base64;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author yang
 */
public class CmdManagerTest extends TestBase {

    @Autowired
    private CmdManager cmdManager;

    @Test
    public void create_cmd_id_from_job_and_node() throws UnsupportedEncodingException {
        JobKey jobKey = new JobKey(10L, 1L);
        Node node = new Node("root/hello");

        String cmdId = cmdManager.getId(jobKey, node);
        Assert.assertNotNull(cmdId);

        String decoded = new String(Base64.getDecoder().decode(cmdId), "UTF-8");
        Assert.assertNotNull(decoded);

        String[] tokens = decoded.split("@");
        Assert.assertEquals(jobKey.getId(), tokens[0]);
        Assert.assertEquals(node.getPath().toString(), tokens[1]);
    }

}
