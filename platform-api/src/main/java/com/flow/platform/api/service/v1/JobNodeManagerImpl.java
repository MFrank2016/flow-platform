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

package com.flow.platform.api.service.v1;

import com.flow.platform.api.dao.v1.JobTreeDao;
import com.flow.platform.api.domain.v1.JobKey;
import com.flow.platform.tree.Node;
import com.flow.platform.tree.NodePath;
import com.flow.platform.tree.NodeTree;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author yang
 */
@Component
public class JobNodeManagerImpl implements JobNodeManager {

    @Autowired
    private JobTreeDao jobTreeDao;

    @Override
    public Node root(JobKey key) {
        return getTree(key).getRoot();
    }

    @Override
    public Node get(JobKey key, NodePath path) {
        return getTree(key).get(path);
    }

    @Override
    public Node next(JobKey key, NodePath path) {
        return getTree(key).next(path);
    }

    private NodeTree getTree(JobKey key) {
        return jobTreeDao.get(key).getTree();
    }
}
