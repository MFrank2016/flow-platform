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

package com.flow.platform.api.test.service;

import static com.flow.platform.api.envs.FlowEnvs.FLOW_ENV_OUTPUT_PREFIX;
import static com.flow.platform.api.envs.FlowEnvs.FLOW_STATUS;
import static com.flow.platform.api.envs.FlowEnvs.FLOW_YML_STATUS;
import static com.flow.platform.api.envs.GitEnvs.FLOW_GIT_BRANCH;
import static com.flow.platform.api.envs.GitEnvs.FLOW_GIT_WEBHOOK;
import static junit.framework.TestCase.fail;

import com.flow.platform.api.domain.Flow;
import com.flow.platform.api.envs.EnvUtil;
import com.flow.platform.api.envs.FlowEnvs;
import com.flow.platform.api.test.TestBase;
import com.flow.platform.core.exception.IllegalOperationException;
import com.flow.platform.core.exception.IllegalParameterException;
import com.google.common.collect.Sets;
import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author yang
 */
public class EnvServiceTest extends TestBase {

    @Test(expected = IllegalOperationException.class)
    public void should_raise_exception_when_save_readonly_env() {
        Assert.assertTrue(FlowEnvs.FLOW_STATUS.isReadonly());

        Flow mock = new Flow("flow");
        envService.save(mock, EnvUtil.build(FlowEnvs.FLOW_STATUS.name(), "Hello"), true);
    }

    @Test(expected = IllegalOperationException.class)
    public void should_raise_exception_when_delete_readonly_env() {
        Assert.assertTrue(FlowEnvs.FLOW_STATUS.isReadonly());

        Flow mock = new Flow("flow");
        envService.delete(mock, Sets.newHashSet(FlowEnvs.FLOW_STATUS.name()), true);
    }

    @Test
    public void should_env_not_changed_when_env_handler_has_exception() {
        // given:
        Flow node = flowService.save("flow");

        // when: save env variable with error
        Map<String, String> envs = new HashMap<>(2);
        envs.put(FlowEnvs.FLOW_TASK_CRONTAB_CONTENT.name(), "111");
        envs.put(FlowEnvs.FLOW_TASK_CRONTAB_BRANCH.name(), "master");

        try {
            envService.save(node, envs, false);
            fail();

        } catch (Throwable e) {
            Assert.assertTrue(e instanceof IllegalParameterException);

            // then: the new env not been recorded
            Assert.assertNull(node.getEnv(FlowEnvs.FLOW_TASK_CRONTAB_CONTENT));
            Assert.assertNull(node.getEnv(FlowEnvs.FLOW_TASK_CRONTAB_BRANCH));
        }
    }

    @Test
    public void should_list_env() {
        // given:
        Flow flow = flowService.save("flow");
        flow.putEnv(FLOW_ENV_OUTPUT_PREFIX, "hello");
        flow.putEnv(FLOW_GIT_BRANCH, "master");
        envService.save(flow, flow.getEnvs(), false);

        // when: list editable only which from copied node envs
        Map<String, String> editable = envService.list(flow, true);
        Assert.assertEquals(2, editable.size());
        Assert.assertFalse(editable.containsKey(FLOW_STATUS.name()));

        // then: check the flow actual envs size
        flow = flowService.find(flow.getName());
        Assert.assertEquals(11, flow.getEnvs().size());

        // when: list none editable only which from copied node envs
        Map<String, String> noneEditable = envService.list(flow, false);
        Assert.assertEquals(9, noneEditable.size());
        Assert.assertTrue(noneEditable.containsKey(FLOW_STATUS.name()));
        Assert.assertTrue(noneEditable.containsKey(FLOW_YML_STATUS.name()));
        Assert.assertTrue(noneEditable.containsKey(FLOW_GIT_WEBHOOK.name()));

        // then: check the actual flow envs size not changed
        Assert.assertEquals(11, flow.getEnvs().size());
    }
}
