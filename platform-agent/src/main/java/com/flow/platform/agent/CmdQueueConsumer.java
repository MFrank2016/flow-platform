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

package com.flow.platform.agent;

import com.flow.platform.agent.mq.Consumer;
import com.flow.platform.domain.Jsonable;
import com.flow.platform.domain.v1.Cmd;
import com.google.gson.JsonParseException;
import java.util.concurrent.ExecutorService;
import lombok.extern.log4j.Log4j2;

/**
 * @author yh@fir.im
 */
@Log4j2
public class CmdQueueConsumer extends Consumer {

    CmdQueueConsumer(String host, String queueName, ExecutorService executorService) {
        super(host, queueName, executorService);
    }

    @Override
    public void item(byte[] rawData) {
        log.trace("Received cmd : " + new String(rawData));

        try {
            Cmd cmd = Jsonable.parse(rawData, Cmd.class);
            log.trace("Cmd parsed : " + cmd.toString());
            CmdManager.getInstance().execute(cmd);
        } catch (JsonParseException e) {
            log.warn("Unable to parse cmd from zk node: " + new String(rawData));
        }
    }
}
