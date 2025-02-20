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
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.server.cluster.raft.RaftServerManager;
import org.apache.seata.server.lock.LockerManagerFactory;
import org.apache.seata.server.session.SessionHolder;
import org.apache.seata.server.store.StoreConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;


import static org.apache.seata.common.ConfigurationKeys.SERVER_RAFT_SSL_CLIENT_KEYSTORE_PATH;
import static org.apache.seata.common.ConfigurationKeys.SERVER_RAFT_SSL_ENABLED;
import static org.apache.seata.common.ConfigurationKeys.SERVER_RAFT_SSL_KMF_ALGORITHM;
import static org.apache.seata.common.ConfigurationKeys.SERVER_RAFT_SSL_SERVER_KEYSTORE_PATH;
import static org.apache.seata.common.ConfigurationKeys.SERVER_RAFT_SSL_TMF_ALGORITHM;

@SpringBootTest
public class RaftServerTest {

    @BeforeAll
    public static void setUp(ApplicationContext context) {
        LockerManagerFactory.destroy();
        SessionHolder.destroy();
    }

    @AfterEach
    public void destroy() {
        System.setProperty("server.raftPort", "0");
        System.setProperty(ConfigurationKeys.SERVER_RAFT_SERVER_ADDR, "");
        ConfigurationCache.clear();
        StoreConfig.setStartupParameter("file", "file", "file");
        LockerManagerFactory.destroy();
        SessionHolder.destroy();
    }

    @Test
    public void initRaftServerStart() {
       Assertions.assertDoesNotThrow(()-> ConfigurationFactory.getInstance().getConfig(SERVER_RAFT_SSL_ENABLED));
       Assertions.assertDoesNotThrow(()-> ConfigurationFactory.getInstance().getConfig(SERVER_RAFT_SSL_CLIENT_KEYSTORE_PATH));
       Assertions.assertDoesNotThrow(()-> ConfigurationFactory.getInstance().getConfig(SERVER_RAFT_SSL_SERVER_KEYSTORE_PATH));
       Assertions.assertDoesNotThrow(()-> ConfigurationFactory.getInstance().getConfig(SERVER_RAFT_SSL_KMF_ALGORITHM));
       Assertions.assertDoesNotThrow(()-> ConfigurationFactory.getInstance().getConfig(SERVER_RAFT_SSL_TMF_ALGORITHM));
        System.setProperty("server.raftPort", "9091");
        System.setProperty(ConfigurationKeys.SERVER_RAFT_SERVER_ADDR,
            XID.getIpAddress() + ":9091" + "," + XID.getIpAddress() + ":9092" + "," + XID.getIpAddress() + ":9093");
        StoreConfig.setStartupParameter("raft", "raft", "raft");
        Assertions.assertDoesNotThrow(RaftServerManager::init);
        Assertions.assertNotNull(RaftServerManager.getRaftServer("default"));
        Assertions.assertNotNull(RaftServerManager.groups());
        Assertions.assertNotNull(RaftServerManager.getCliServiceInstance());
        Assertions.assertNotNull(RaftServerManager.getCliClientServiceInstance());
        Assertions.assertFalse(RaftServerManager.isLeader("default"));
        RaftServerManager.start();
    }

    @Test
    public void initRaftServerFail() {
        StoreConfig.setStartupParameter("raft", "raft", "raft");
        Assertions.assertThrows(IllegalArgumentException.class, RaftServerManager::init);
    }

    @Test
    public void initRaftServerFailByRaftPortNull() {
        System.setProperty(ConfigurationKeys.SERVER_RAFT_SERVER_ADDR,
            XID.getIpAddress() + ":9091" + "," + XID.getIpAddress() + ":9092" + "," + XID.getIpAddress() + ":9093");
        StoreConfig.setStartupParameter("raft", "raft", "raft");
        Assertions.assertThrows(IllegalArgumentException.class, RaftServerManager::init);
    }

}
