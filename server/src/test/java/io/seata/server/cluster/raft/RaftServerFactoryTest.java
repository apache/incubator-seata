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
package io.seata.server.cluster.raft;

import io.seata.common.XID;
import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.common.util.NetUtil;
import io.seata.core.rpc.RemotingServer;
import io.seata.server.coordinator.AbstractCore;
import io.seata.server.coordinator.DefaultCoordinator;
import io.seata.server.coordinator.DefaultCoordinatorTest;
import io.seata.server.coordinator.DefaultCore;
import io.seata.server.session.SessionHolder;
import io.seata.server.store.StoreConfig;
import javax.transaction.xa.Xid;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.*;

class RaftServerFactoryTest {

    static {
        System.setProperty("config.type", "file");
        System.setProperty("config.file.name", "file.conf");
        System.setProperty("registry.type", "file");
        System.setProperty("registry.file.name", "registry.conf");
        System.setProperty("server.raftPort", "9091");
        System.setProperty("server.raft.serverAddr", NetUtil.getLocalIp() + ":9091");
        System.setProperty("store.session.mode", "raft");
        System.setProperty("store.file.dir", "test_file_store/data");
        System.setProperty("server.raft.group", "test_raft_group");
        System.setProperty("server.raft.snapshotInterval", "10000");
        System.setProperty("server.raft.applyBatch", "10000");
        System.setProperty("server.raft.maxAppendBufferSize", "1024");
        System.setProperty("server.raft.disruptorBufferSize", "1024");
        System.setProperty("server.raft.maxReplicatorInflightMsgs", "10000");
        System.setProperty("server.raft.sync", "true");
        System.setProperty("server.raft.electionTimeoutMs", "10000");
        System.setProperty("store.mode", "raft");
        System.setProperty("store.lock.mode", "raft");
        System.setProperty("server.distributedLockExpireTime", "10000");
        System.setProperty("server.raft.reporterEnabled", "false");
        // no exception
    }

    @BeforeAll
    public static void beforeClass() throws Exception {
        XID.setIpAddress(NetUtil.getLocalIp());
    }

    @AfterAll
    public static void destroy() {
        SessionHolder.destroy();
    }

    @Test
    void start() {
        RaftServerFactory raftServerFactory = new RaftServerFactory();
        raftServerFactory.init();
        raftServerFactory.start();
    }

    @Test
    void close() {
        RaftServerFactory raftServerFactory = new RaftServerFactory();
        raftServerFactory.init();
        raftServerFactory.start();
        raftServerFactory.getRaftServers().forEach((v) -> {
            v.close();
        });
        // no exception
    }
}