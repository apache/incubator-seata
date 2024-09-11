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
package org.apache.seata.server.raft;

import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.common.XID;
import org.apache.seata.config.ConfigurationCache;
import org.apache.seata.server.cluster.raft.RaftConfigServerManager;
import org.apache.seata.server.store.StoreConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

@SpringBootTest
public class RaftConfigServerTest {

    @BeforeAll
    public static void setUp(ApplicationContext context) {
        RaftConfigServerManager.destroy();
    }
    @BeforeEach
    public void init() {
        System.setProperty("config.type", "raft");
        System.setProperty("registry.preferredNetworks", "*");
        System.setProperty("config.raft.db.dir", "configStore");
        System.setProperty("config.raft.db.destroyOnShutdown", "true");
    }

    @AfterEach
    public void destroy() {
        System.setProperty("server.raftPort", "0");
        System.setProperty(ConfigurationKeys.SERVER_RAFT_SERVER_ADDR, "");
        ConfigurationCache.clear();
        RaftConfigServerManager.destroy();
    }

    @Test
    public void initRaftConfigServerStart() {
        System.setProperty("server.raftPort", "9091");
        System.setProperty(ConfigurationKeys.SERVER_RAFT_SERVER_ADDR,
                XID.getIpAddress() + ":9091" + "," + XID.getIpAddress() + ":9092" + "," + XID.getIpAddress() + ":9093");
        StoreConfig.setStartupParameter("raft", "raft", "raft");
        Assertions.assertDoesNotThrow(RaftConfigServerManager::init);
        Assertions.assertNotNull(RaftConfigServerManager.getRaftServer());
        Assertions.assertNotNull(RaftConfigServerManager.getGroup());
        Assertions.assertNotNull(RaftConfigServerManager.getCliServiceInstance());
        Assertions.assertNotNull(RaftConfigServerManager.getCliClientServiceInstance());
        Assertions.assertFalse(RaftConfigServerManager.isLeader());
        RaftConfigServerManager.start();
    }

    @Test
    public void initRaftConfigServerFail() {
        Assertions.assertThrows(IllegalArgumentException.class, RaftConfigServerManager::init);
    }

    @Test
    public void initRaftConfigServerFailByRaftPortNull() {
        System.setProperty(ConfigurationKeys.SERVER_RAFT_SERVER_ADDR,
                XID.getIpAddress() + ":9091" + "," + XID.getIpAddress() + ":9092" + "," + XID.getIpAddress() + ":9093");
        Assertions.assertThrows(IllegalArgumentException.class, RaftConfigServerManager::init);
    }

}
