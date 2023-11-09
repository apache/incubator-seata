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
package io.seata.server.raft;

import io.seata.common.ConfigurationKeys;
import io.seata.common.XID;
import io.seata.server.cluster.raft.RaftServerFactory;
import io.seata.server.store.StoreConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

@SpringBootTest
public class RaftServerTest {

    @BeforeAll
    public static void setUp(ApplicationContext context) {
    }

    @AfterEach
    public void destroy() {
        System.setProperty("server.raftPort", "0");
        System.setProperty(ConfigurationKeys.SERVER_RAFT_SERVER_ADDR, "");
        StoreConfig.setStartupParameter("file", "file", "file");
    }

    @Test
    public void initRaftServerStart() {
        System.setProperty("server.raftPort", "9091");
        System.setProperty(ConfigurationKeys.SERVER_RAFT_SERVER_ADDR,
            XID.getIpAddress() + ":9091" + "," + XID.getIpAddress() + ":9092" + "," + XID.getIpAddress() + ":9093");
        StoreConfig.setStartupParameter("raft", "raft", "raft");
        Assertions.assertDoesNotThrow(() -> RaftServerFactory.getInstance().init());
        Assertions.assertNotNull(RaftServerFactory.getInstance().getRaftServer("default"));
        RaftServerFactory.getInstance().start();
        RaftServerFactory.getInstance().destroy();
    }

    @Test
    public void initRaftServerFail() {
        StoreConfig.setStartupParameter("raft", "raft", "raft");
        Assertions.assertThrows(IllegalArgumentException.class, () -> RaftServerFactory.getInstance().init());
    }

    @Test
    public void initRaftServerFailByRaftPortNull() {
        System.setProperty(ConfigurationKeys.SERVER_RAFT_SERVER_ADDR,
            XID.getIpAddress() + ":9091" + "," + XID.getIpAddress() + ":9092" + "," + XID.getIpAddress() + ":9093");
        StoreConfig.setStartupParameter("raft", "raft", "raft");
        Assertions.assertThrows(IllegalArgumentException.class, () -> RaftServerFactory.getInstance().init());
    }

}
