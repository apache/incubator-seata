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

import com.alipay.sofa.jraft.RaftGroupService;
import io.seata.common.XID;
import io.seata.common.util.NetUtil;
import io.seata.common.util.ReflectionUtil;
import io.seata.server.session.SessionHolder;
import java.util.Collection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

    @BeforeEach
    public void before() throws Exception {
        XID.setIpAddress(NetUtil.getLocalIp());
    }

    @AfterEach
    public void destroy() {
        try {
            Collection<RaftServer> servers = RaftServerFactory.getInstance().getRaftServers();
            if (servers != null) {
                servers.forEach((v) -> {
                    closeSharedRaftServer(v);
                });
            }
            SessionHolder.destroy();
        } catch (Throwable e) {
            // ignore
        } finally {

            XID.setIpAddress(null);

            System.clearProperty("config.type");
            System.clearProperty("config.file.name");
            System.clearProperty("registry.type");
            System.clearProperty("registry.file.name");
            System.clearProperty("server.raftPort");
            System.clearProperty("server.raft.serverAddr");
            System.clearProperty("store.session.mode");
            System.clearProperty("store.file.dir");
            System.clearProperty("server.raft.group");
            System.clearProperty("server.raft.snapshotInterval");
            System.clearProperty("server.raft.applyBatch");
            System.clearProperty("server.raft.maxAppendBufferSize");
            System.clearProperty("server.raft.disruptorBufferSize");
            System.clearProperty("server.raft.maxReplicatorInflightMsgs");
            System.clearProperty("server.raft.sync");
            System.clearProperty("server.raft.electionTimeoutMs");
            System.clearProperty("store.mode");
            System.clearProperty("store.lock.mode");
            System.clearProperty("server.distributedLockExpireTime");
            System.clearProperty("server.raft.reporterEnabled");
        }

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
            closeSharedRaftServer(v);
        });
        // no exception
    }

    private static void closeSharedRaftServer(RaftServer v) {
        RaftGroupService raftGroupService = null;
        try {
            raftGroupService = ReflectionUtil.getFieldValue(v, "raftGroupService");
        } catch (NoSuchFieldException e) {

        }
        if (raftGroupService.getRpcServer() != null) {
            raftGroupService.getRpcServer().shutdown();
        }
        v.close();
    }
}