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

import com.flow.platform.agent.mq.Pusher;
import com.flow.platform.cmd.ProcListener;
import com.flow.platform.domain.CmdResult;
import com.flow.platform.domain.CmdStatus;
import com.flow.platform.tree.Result;
import java.util.List;
import java.util.Objects;
import lombok.extern.log4j.Log4j2;

/**
 * @author gy@fir.im
 */
@Log4j2
public class ProcEventHandler implements ProcListener {

    private final com.flow.platform.tree.Cmd cmd;
    private final List<ProcListener> extraProcEventListeners;
    private final ReportManager reportManager = ReportManager.getInstance();

    public ProcEventHandler(com.flow.platform.tree.Cmd cmd,
                            List<ProcListener> extraProcEventListeners) {
        this.cmd = cmd;
        this.extraProcEventListeners = extraProcEventListeners;
    }

    @Override
    public void onStarted(CmdResult result) {

        // Send cmd Result to Queue
        // TODO: send cmd result to queue (Running)
        reportResult(CmdStatus.RUNNING, result);

        for (ProcListener listener : extraProcEventListeners) {
            listener.onStarted(result);
        }
    }

    @Override
    public void onExecuted(CmdResult result) {
        // report cmd sync since block current thread
//        reportManager.cmdReportSync(cmd.getId(), CmdStatus.EXECUTED, result);
        // TODO: send cmd result to queue (Executed)
        reportResult(CmdStatus.EXECUTED, result);

        for (ProcListener listener : extraProcEventListeners) {
            listener.onExecuted(result);
        }
    }

    @Override
    public void onLogged(CmdResult result) {
        log.debug("got result...");

        // report cmd sync since block current thread
        // TODO: send cmd result to queue (Logged)
        reportResult(CmdStatus.LOGGED, result);

        for (ProcListener listener : extraProcEventListeners) {
            listener.onLogged(result);
        }
    }

    @Override
    public void onException(CmdResult result) {

        // report cmd sync since block current thread
//        reportManager.cmdReportSync(cmd.getId(), CmdStatus.EXCEPTION, result);
        // TODO: send cmd result to queue (Exception)
        reportResult(CmdStatus.EXCEPTION, result);

        for (ProcListener listener : extraProcEventListeners) {
            listener.onException(result);
        }
    }

    private void reportResult(CmdStatus cmdStatus, CmdResult result) {
        cmd.setStatus(cmdStatus);
        cmd.setResult(resultAdaptor(result));
        Pusher.publish(cmd.toJson());
    }

    private Result resultAdaptor(CmdResult result) {

        Result res = new Result();
        res.setPath(cmd.getNodePath());

        if (!Objects.isNull(result)) {

            if(!Objects.isNull(result.getExitValue())) {
                res.setCode(result.getExitValue());
            }

            if(!Objects.isNull(result.getDuration())) {
                res.setDuration(result.getDuration());
            }

            if(!Objects.isNull(result.getOutput())) {
                result.getOutput().forEach((k, v) -> {
                    res.put(k, v);
                });
            }
        }
        return res;
    }
}
