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

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.BranchType;
import io.seata.core.store.BranchTransactionDO;
import io.seata.core.store.GlobalTransactionDO;
import io.seata.server.cluster.raft.msg.RaftSyncMsg;
import io.seata.server.cluster.raft.msg.RaftSyncMsgSerializer;
import io.seata.server.cluster.raft.snapshot.RaftSnapshot;
import io.seata.server.cluster.raft.snapshot.RaftSnapshotSerializer;
import io.seata.server.session.BranchSession;
import io.seata.server.session.GlobalSession;
import io.seata.server.session.SessionHelper;
import io.seata.server.session.SessionHolder;
import io.seata.server.storage.raft.RaftSessionSyncMsg;
import io.seata.server.store.StoreConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

/**
 * @author jianbin.chen
 */
@SpringBootTest
public class RaftSyncMsgTest {

    private static final String BRANCH_SESSION_MAP_KEY = "branchSessionMap";

    private static final String GLOBAL_SESSION_MAP_KEY = "globalSessionMap";

    @BeforeAll
    public static void setUp(ApplicationContext context){
        SessionHolder.init(StoreConfig.SessionMode.FILE);
    }

    @AfterAll
    public static void destroy(){
        SessionHolder.destroy();
    }
    @Test
    public void testMsgSerialize() throws IOException {
        RaftSyncMsg raftSyncMsg = new RaftSyncMsg();
        RaftSessionSyncMsg raftSessionSyncMsg = new RaftSessionSyncMsg();
        raftSessionSyncMsg.setBranchSession(new BranchTransactionDO("123:123", 1234));
        raftSessionSyncMsg.setGlobalSession(new GlobalTransactionDO("123:123"));
        raftSyncMsg.setBody(raftSessionSyncMsg);
        byte[] msg = RaftSyncMsgSerializer.encode(raftSyncMsg);
        RaftSyncMsg raftSyncMsg1 = RaftSyncMsgSerializer.decode(msg);
        Assertions.assertEquals("123:123", ((RaftSessionSyncMsg)raftSyncMsg1.getBody()).getBranchSession().getXid());
        Assertions.assertEquals("123:123", ((RaftSessionSyncMsg)raftSyncMsg1.getBody()).getGlobalSession().getXid());
        Assertions.assertEquals(1234, ((RaftSessionSyncMsg)raftSyncMsg1.getBody()).getBranchSession().getBranchId());
    }

    @Test
    public void testSnapshotSerialize() throws IOException, TransactionException {
        Map<String, Object> maps = new HashMap<>(2);
        Map<String, GlobalSession> sessionMap = new HashMap<>();
        GlobalSession globalSession = GlobalSession.createGlobalSession("123", "123", "123", 11111);
        sessionMap.put(globalSession.getXid(), globalSession);
        globalSession
            .addBranch(SessionHelper.newBranchByGlobal(globalSession, BranchType.AT, "!23", null, "123", "123"));
        Map<String, byte[]> globalSessionByteMap = new HashMap<>();
        // each transaction is expected to have two branches
        Map<Long, byte[]> branchSessionByteMap = new HashMap<>();
        sessionMap.forEach((k, v) -> {
            globalSessionByteMap.put(v.getXid(), v.encode());
            List<BranchSession> branchSessions = Collections.unmodifiableList(v.getBranchSessions());
            branchSessions.forEach(
                branchSession -> branchSessionByteMap.put(branchSession.getBranchId(), branchSession.encode()));
        });
        maps.put(GLOBAL_SESSION_MAP_KEY, globalSessionByteMap);
        maps.put(BRANCH_SESSION_MAP_KEY, branchSessionByteMap);
        RaftSnapshot raftSnapshot = new RaftSnapshot();
        raftSnapshot.setBody(maps);
        byte[] msg = RaftSnapshotSerializer.encode(raftSnapshot);
        RaftSnapshot raftSnapshot1 = RaftSnapshotSerializer.decode(msg);
        Assertions.assertEquals(1,
            ((Map<Long, byte[]>)((Map<String, Object>)raftSnapshot1.getBody()).get(BRANCH_SESSION_MAP_KEY)).size());
        Assertions.assertEquals(1,  ((Map<String, byte[]>)((Map<String, Object>)raftSnapshot1.getBody()).get(GLOBAL_SESSION_MAP_KEY)).size());
        ((Map<Long, byte[]>)((Map<String, Object>)raftSnapshot1.getBody()).get(BRANCH_SESSION_MAP_KEY)).forEach((k,v)->{
            BranchSession branchSession = new BranchSession();
            branchSession.decode(v);
            Assertions.assertEquals(globalSession.getXid(), branchSession.getXid());
        });
        ((Map<String, byte[]>)((Map<String, Object>)raftSnapshot1.getBody()).get(GLOBAL_SESSION_MAP_KEY)).forEach((k,v)->{
            GlobalSession globalSession1 = new GlobalSession();
            globalSession1.decode(v);
            Assertions.assertEquals(globalSession.getXid(), globalSession1.getXid());
        });
    }

}
