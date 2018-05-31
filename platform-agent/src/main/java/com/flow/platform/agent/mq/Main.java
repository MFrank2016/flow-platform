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

package com.flow.platform.agent.mq;

import com.flow.platform.domain.CmdStatus;
import com.flow.platform.domain.v1.Cmd;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author yh@fir.im
 */
public class Main {

    public static void main(String[] args) {

        ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 100, TimeUnit.SECONDS, new LinkedBlockingQueue<>(100));

        Cmd cmd = new Cmd();
        cmd.setStatus(CmdStatus.RUNNING);
//        executor.execute(new CmdConsumer("127.0.0.1", "default==a"));

        Pusher.init("127.0.0.1", "cmd.callback.queue");

        Pusher.publish(cmd.toJson());
//        Pusher.publish("hello");
//        Pusher.publish("hello");
//        Pusher.publish("hello");

        System.out.println("finish send message");
    }
}

