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

package com.flow.platform.api.consumer.v1;

import com.flow.platform.api.domain.CmdCallbackQueueItem;
import com.flow.platform.api.domain.v1.JobTree;
import com.flow.platform.core.queue.PriorityMessage;
import com.flow.platform.queue.PlatformQueue;
import com.flow.platform.queue.QueueListener;
import java.util.Objects;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author yh@fir.im
 */
@Component
public class JobConsumer implements QueueListener<PriorityMessage> {

    @Autowired
    private PlatformQueue<PriorityMessage> jobQueue;

    @PostConstruct
    public void init() {
        jobQueue.register(this);
    }

    @Override
    public void onQueueItem(PriorityMessage message) {
        if (Objects.isNull(message)) {
            return;
        }

        JobTree item = JobTree.parse(message.getBody(), JobTree.class);


    }
}
