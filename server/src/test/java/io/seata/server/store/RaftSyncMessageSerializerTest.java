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
package io.seata.server.store;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import io.seata.server.cluster.raft.msg.RaftSyncMessageSerializer;
import io.seata.server.cluster.raft.msg.RaftSyncMessage;
import io.seata.server.session.GlobalSession;
import io.seata.server.storage.SessionConverter;
import io.seata.server.storage.raft.RaftSessionSyncMsg;

/**
 * @author funkye
 */
@SpringBootTest
public class RaftSyncMessageSerializerTest {
    @BeforeAll
    public static void setUp(ApplicationContext context) {

    }

    @Test
    public void testSerializerTest() throws Exception {
        RaftSessionSyncMsg raftSessionSyncMsg = new RaftSessionSyncMsg();
        raftSessionSyncMsg.setMsgType(RaftSessionSyncMsg.MsgType.ADD_GLOBAL_SESSION);
        GlobalSession session = GlobalSession.createGlobalSession("test", "test", "test123", 100);
        raftSessionSyncMsg.setGlobalSession(SessionConverter.convertGlobalTransactionDO(session));
        RaftSyncMessage raftSyncMessage = new RaftSyncMessage();
        raftSyncMessage.setBody(raftSessionSyncMsg);
        byte[] bytes = RaftSyncMessageSerializer.encode(raftSyncMessage);
        RaftSyncMessage raftSyncMessage2 = RaftSyncMessageSerializer.decode(bytes);
        RaftSessionSyncMsg raftSessionSyncMsg2 = (RaftSessionSyncMsg) raftSyncMessage2.getBody();
        Assertions.assertTrue(raftSessionSyncMsg2.getGlobalSession().getXid().equals(session.getXid()));
    }

}
