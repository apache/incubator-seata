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
package org.apache.seata.server.store;

import org.apache.seata.server.cluster.raft.sync.msg.RaftGlobalSessionSyncMsg;
import org.apache.seata.server.cluster.raft.sync.msg.RaftSyncMsgType;
import org.apache.seata.server.cluster.raft.sync.msg.dto.GlobalTransactionDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import org.apache.seata.server.cluster.raft.sync.RaftSyncMessageSerializer;
import org.apache.seata.server.cluster.raft.sync.msg.RaftSyncMessage;
import org.apache.seata.server.session.GlobalSession;
import org.apache.seata.server.storage.SessionConverter;

/**
 */
@SpringBootTest
public class RaftSyncMessageSerializerTest {
    @BeforeAll
    public static void setUp(ApplicationContext context) {

    }

    @Test
    public void testSerializerTest() throws Exception {
        RaftGlobalSessionSyncMsg raftSessionSyncMsg = new RaftGlobalSessionSyncMsg();
        raftSessionSyncMsg.setMsgType(RaftSyncMsgType.ADD_GLOBAL_SESSION);
        GlobalSession session = GlobalSession.createGlobalSession("test", "test", "test123", 100);
        GlobalTransactionDTO globalTransactionDTO = new GlobalTransactionDTO();
        SessionConverter.convertGlobalTransactionDO(globalTransactionDTO,session);
        raftSessionSyncMsg.setGlobalSession(globalTransactionDTO);
        RaftSyncMessage raftSyncMessage = new RaftSyncMessage();
        raftSyncMessage.setBody(raftSessionSyncMsg);
        byte[] bytes = RaftSyncMessageSerializer.encode(raftSyncMessage);
        RaftSyncMessage raftSyncMessage2 = RaftSyncMessageSerializer.decode(bytes);
        RaftGlobalSessionSyncMsg raftSessionSyncMsg2 = (RaftGlobalSessionSyncMsg) raftSyncMessage2.getBody();
        Assertions.assertTrue(raftSessionSyncMsg2.getGlobalSession().getXid().equals(session.getXid()));
        Assertions.assertTrue(raftSessionSyncMsg.getMsgType().equals(raftSessionSyncMsg2.getMsgType()));
    }

}
