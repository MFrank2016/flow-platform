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

package com.flow.platform.cc.test.controller;

import static junit.framework.TestCase.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.fileUpload;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.flow.platform.agent.manager.service.AgentCCService;
import com.flow.platform.cc.service.CmdCCService;
import com.flow.platform.cc.service.ZoneService;
import com.flow.platform.cc.test.TestBase;
import com.flow.platform.domain.AgentPath;
import com.flow.platform.domain.AgentStatus;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdInfo;
import com.flow.platform.domain.CmdReport;
import com.flow.platform.domain.CmdResult;
import com.flow.platform.domain.CmdStatus;
import com.flow.platform.domain.CmdType;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

/**
 * @author gy@fir.im
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CmdControllerTest extends TestBase {

    @Autowired
    private CmdCCService cmdService;

    @Autowired
    private AgentCCService agentService;

    @Autowired
    private ZoneService zoneService;

    @Autowired
    private Path cmdLogDir;

    @Test
    public void should_list_cmd_types() throws Throwable {
        // when:
        MvcResult result = this.mockMvc.perform(get("/cmd/types"))
            .andDo(print())
            .andExpect(status().isOk())
            .andReturn();

        // then:
        String raw = result.getResponse().getContentAsString();
        CmdType[] types = gsonConfig.fromJson(raw, CmdType[].class);
        Assert.assertNotNull(types);
        Assert.assertTrue(types.length == 7);
    }

    @Test
    public void should_update_cmd_status() throws Throwable {
        // given:
        String zone = "test-mos-mac";
        String agent = "test-001";

        AgentPath path = new AgentPath(zone, agent);
        agentService.report(path, AgentStatus.IDLE);
        Thread.sleep(1000);

        CmdInfo base = new CmdInfo(zone, agent, CmdType.STOP, null);
        Cmd cmd = cmdService.create(base);

        // when:
        CmdReport postData = new CmdReport(cmd.getId(), CmdStatus.EXECUTED, new CmdResult());

        MockHttpServletRequestBuilder content = post("/cmd/report")
            .contentType(MediaType.APPLICATION_JSON)
            .content(postData.toJson());

        this.mockMvc.perform(content).andDo(print()).andExpect(status().isOk());

        // then: wait queue processing and check status
        Thread.sleep(2000);
        Cmd loaded = cmdService.find(cmd.getId());
        Assert.assertNotNull(loaded);
        Assert.assertTrue(loaded.getStatus().equals(CmdStatus.EXECUTED));
    }

    @Test
    public void should_upload_and_download_zipped_log() throws Throwable {
        // given:
        ClassLoader classLoader = CmdControllerTest.class.getClassLoader();
        URL resource = classLoader.getResource("test-cmd-id.out.zip");
        Path path = Paths.get(resource.getFile());
        byte[] data = Files.readAllBytes(path);

        CmdInfo cmdBase = new CmdInfo("test-zone-1", "test-agent-1", CmdType.RUN_SHELL, "~/hello.sh");
        Cmd cmd = cmdService.create(cmdBase);

        String originalFilename = cmd.getId() + ".out.zip";

        MockMultipartFile zippedCmdLogPart = new MockMultipartFile("file", originalFilename, "application/zip", data);
        MockMultipartFile cmdIdPart = new MockMultipartFile("cmdId", "", "text/plain", cmd.getId().getBytes());

        // when: upload zipped cmd log
        MockMultipartHttpServletRequestBuilder content = fileUpload("/cmd/log/upload")
            .file(zippedCmdLogPart)
            .file(cmdIdPart);

        this.mockMvc.perform(content)
            .andDo(print())
            .andExpect(status().isOk());

        // then: check upload file path and logging queue
        Path zippedLogPath = Paths.get(cmdLogDir.toString(), originalFilename);
        Assert.assertTrue(Files.exists(zippedLogPath));
        Assert.assertEquals(data.length, Files.size(zippedLogPath));

        // when: download uploaded zipped cmd log
        MvcResult result = this.mockMvc.perform(get("/cmd/log/download")
            .param("cmdId", cmd.getId()).param("index", Integer.toString(0)))
            .andDo(print())
            .andExpect(status().isOk())
            .andReturn();

        // then:
        MockHttpServletResponse response = result.getResponse();
        Assert.assertEquals("application/zip", response.getContentType());
        Assert.assertEquals(data.length, response.getContentLength());
        Assert.assertTrue(response.getHeader("Content-Disposition").contains(originalFilename));
    }
}
