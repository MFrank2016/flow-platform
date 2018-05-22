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

import com.flow.platform.api.domain.Flow;
import com.flow.platform.api.domain.FlowYml;
import com.flow.platform.api.domain.v1.JobKey;
import com.flow.platform.api.domain.v1.JobV1;
import com.flow.platform.api.test.FlowHelper;
import com.flow.platform.api.test.TestBase;
import com.flow.platform.tree.NodeTree;
import com.flow.platform.tree.yml.YmlHelper;
import java.io.IOException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author yh@firim
 */
public class JobDaoTest extends TestBase {

    @Autowired
    private FlowHelper flowHelper;

    private Flow flow;

    private FlowYml flowYml;

    @Before
    public void init() throws IOException {
        flow = flowHelper.createFlowWithYml("job_dao_test", "yml/for_job_test.yml");
        Assert.assertNotNull(flow);

        flowYml = ymlDao.get(flow.getName());
        Assert.assertNotNull(flowYml);
    }

    @Test
    public void should_create_and_get_job() {
        NodeTree tree = NodeTree.create(YmlHelper.build(flowYml.getContent()));
        JobV1 job = new JobV1(flow.getName(), 0L);
        job.setTree(tree);
        job.setCreatedBy("admin@flow.ci");
        jobDaoV1.save(job);

        JobV1 loaded = jobDaoV1.get(new JobKey(flow.getName(), 0L));
        Assert.assertNotNull(loaded.getTree());
        Assert.assertEquals(job, loaded);
    }

}
