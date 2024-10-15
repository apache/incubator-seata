/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.server.controller;


import org.apache.seata.server.cluster.manager.ClusterWatcherManager;
import org.apache.seata.server.cluster.watch.Watcher;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.nio.channels.Channel;
import java.util.Map;

@RestController
@RequestMapping("/metadata/v2")
public class ClusterV2Controller {

    @Resource
    private ClusterWatcherManager clusterWatcherManager;

    @PostMapping("/watch")
    public void watchV2(Channel channel, @RequestParam Map<String, Object> groupTerms,
                        @RequestParam(defaultValue = "28000") int timeout) {
        groupTerms.forEach((group, term) -> {
            Watcher<Channel> watcher =
                    new Watcher<>(group, channel, timeout, Long.parseLong(String.valueOf(term)));
            clusterWatcherManager.registryWatcher(watcher);
        });
    }
}
