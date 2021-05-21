/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.spring.boot.autoconfigure.properties.server;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


import static io.seata.spring.boot.autoconfigure.StarterConstants.SERVER_RAFT_PREFIX;

/**
 * @author funkye
 */
@Component
@ConfigurationProperties(prefix = SERVER_RAFT_PREFIX)
public class ServerRaftProperties {

    private String cluster;

    private Integer snapshotInterval;

    public String getCluster() {
        return cluster;
    }

    public ServerRaftProperties setCluster(String cluster) {
        this.cluster = cluster;
        return this;
    }

    public Integer getSnapshotInterval() {
        return snapshotInterval;
    }

    public ServerRaftProperties setSnapshotInterval(Integer snapshotInterval) {
        this.snapshotInterval = snapshotInterval;
        return this;
    }

}
