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
import io.seata.server.cluster.raft.msg.RaftSyncMessage;
import io.seata.server.cluster.raft.msg.RaftSyncMessageSerializer;
import io.seata.server.cluster.raft.snapshot.RaftSnapshot;
import io.seata.server.cluster.raft.snapshot.RaftSnapshotSerializer;
import io.seata.server.cluster.raft.snapshot.session.SessionSnapshot;
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
public class RaftSyncMessageTest {

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
        RaftSyncMessage raftSyncMessage = new RaftSyncMessage();
        RaftSessionSyncMsg raftSessionSyncMsg = new RaftSessionSyncMsg();
        raftSessionSyncMsg.setBranchSession(new BranchTransactionDO("123:123", 1234));
        raftSessionSyncMsg.setGlobalSession(new GlobalTransactionDO("123:123"));
        raftSyncMessage.setBody(raftSessionSyncMsg);
        byte[] msg = RaftSyncMessageSerializer.encode(raftSyncMessage);
        RaftSyncMessage raftSyncMessage1 = RaftSyncMessageSerializer.decode(msg);
        Assertions.assertEquals("123:123", ((RaftSessionSyncMsg) raftSyncMessage1.getBody()).getBranchSession().getXid());
        Assertions.assertEquals("123:123", ((RaftSessionSyncMsg) raftSyncMessage1.getBody()).getGlobalSession().getXid());
        Assertions.assertEquals(1234, ((RaftSessionSyncMsg) raftSyncMessage1.getBody()).getBranchSession().getBranchId());
    }

    @Test
    public void testSnapshotSerialize() throws IOException, TransactionException {
        Map<String, GlobalSession> sessionMap = new HashMap<>();
        GlobalSession globalSession = GlobalSession.createGlobalSession("123", "123", "123", 11111);
        sessionMap.put(globalSession.getXid(), globalSession);
        globalSession
            .addBranch(SessionHelper.newBranchByGlobal(globalSession, BranchType.AT, "!23", null, "123", "123"));
        SessionSnapshot sessionSnapshot = new SessionSnapshot();
        sessionMap.forEach((xid, session) -> sessionSnapshot.convert2GlobalSessionByte(session));
        RaftSnapshot raftSnapshot = new RaftSnapshot();
        raftSnapshot.setBody(sessionSnapshot);
        byte[] msg = RaftSnapshotSerializer.encode(raftSnapshot);
        RaftSnapshot raftSnapshot1 = RaftSnapshotSerializer.decode(msg);
        SessionSnapshot sessionSnapshot2 = (SessionSnapshot)raftSnapshot1.getBody();
        Map<String, GlobalSession> map = sessionSnapshot2.convert2GlobalSession();
        Assertions.assertEquals(1, map.size());
        Assertions.assertNotNull(map.get(globalSession.getXid()));
        Assertions.assertEquals(1, map.get(globalSession.getXid()).getBranchSessions().size());
    }

}
