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

import com.flow.platform.agent.mq.RabbitClient;
import com.flow.platform.cmd.ProcListener;
import com.flow.platform.domain.CmdStatus;
import com.flow.platform.domain.v1.Cmd;
import com.flow.platform.domain.v1.ExecutedCmd;
import com.flow.platform.util.DateUtil;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import lombok.extern.log4j.Log4j2;

/**
 * @author gy@fir.im
 */
@Log4j2
public class ProcEventHandler implements ProcListener {

    private final List<ProcListener> extra;

    private final RabbitClient pusher;

    private final ExecutedCmd result;

    private ZonedDateTime startAt;

    ProcEventHandler(RabbitClient pusher, Cmd cmd, List<ProcListener> extra) {
        this.pusher = pusher;
        this.extra = extra;
        this.result = ExecutedCmd.transfer(cmd);
    }

    @Override
    public void onStarted() {
        startAt = DateUtil.now();

        for (ProcListener listener : extra) {
            listener.onStarted();
        }
    }

    @Override
    public void onExecuted(int code) {
        result.setCode(code);
        result.setDuration(ChronoUnit.SECONDS.between(startAt, DateUtil.now()));
        result.setStatus(CmdStatus.EXECUTED);
        pusher.send(result.toJson());

        for (ProcListener listener : extra) {
            listener.onExecuted(code);
        }
    }

    @Override
    public void onLogged(Map<String, String> output) {
        result.setOutput(output);
        result.setStatus(CmdStatus.LOGGED);
        result.setDuration(ChronoUnit.SECONDS.between(startAt, DateUtil.now()));
        pusher.send(result.toJson());

        for (ProcListener listener : extra) {
            listener.onLogged(output);
        }
    }

    @Override
    public void onException(Throwable e) {
        result.setErrMsg(e.getMessage());
        result.setStatus(CmdStatus.EXCEPTION);
        result.setDuration(ChronoUnit.SECONDS.between(startAt, DateUtil.now()));
        pusher.send(result.toJson());

        for (ProcListener listener : extra) {
            listener.onException(e);
        }
    }
}
