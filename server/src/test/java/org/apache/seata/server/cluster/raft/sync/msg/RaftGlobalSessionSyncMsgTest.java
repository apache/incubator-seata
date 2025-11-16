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
package org.apache.seata.server.cluster.raft.sync.msg;

import org.apache.seata.server.cluster.raft.sync.msg.dto.GlobalTransactionDTO;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static org.junit.jupiter.api.Assertions.*;

public class RaftGlobalSessionSyncMsgTest {

    @Test
    public void testDefaultConstructor() {
        RaftGlobalSessionSyncMsg msg = new RaftGlobalSessionSyncMsg();
        assertNotNull(msg);
        assertNull(msg.getGlobalSession());
        assertNull(msg.getMsgType());
    }

    @Test
    public void testConstructorWithParameters() {
        GlobalTransactionDTO dto = new GlobalTransactionDTO("xid:123456");
        RaftGlobalSessionSyncMsg msg = new RaftGlobalSessionSyncMsg(RaftSyncMsgType.ADD_GLOBAL_SESSION, dto);

        assertEquals(RaftSyncMsgType.ADD_GLOBAL_SESSION, msg.getMsgType());
        assertEquals(dto, msg.getGlobalSession());
    }

    @Test
    public void testSetAndGetGlobalSession() {
        RaftGlobalSessionSyncMsg msg = new RaftGlobalSessionSyncMsg();
        GlobalTransactionDTO dto = new GlobalTransactionDTO("xid:789");
        msg.setGlobalSession(dto);

        assertEquals(dto, msg.getGlobalSession());
    }

    @Test
    public void testToString() {
        GlobalTransactionDTO dto = new GlobalTransactionDTO("xid:123");
        RaftGlobalSessionSyncMsg msg = new RaftGlobalSessionSyncMsg(RaftSyncMsgType.REMOVE_GLOBAL_SESSION, dto);

        String str = msg.toString();
        assertNotNull(str);
        assertFalse(str.isEmpty());
    }

    @Test
    public void testSerialization() throws Exception {
        GlobalTransactionDTO dto = new GlobalTransactionDTO("xid:123456");
        RaftGlobalSessionSyncMsg original =
                new RaftGlobalSessionSyncMsg(RaftSyncMsgType.UPDATE_GLOBAL_SESSION_STATUS, dto);

        // Serialize
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(original);
        oos.flush();
        byte[] serialized = bos.toByteArray();

        // Deserialize
        ByteArrayInputStream bis = new ByteArrayInputStream(serialized);
        ObjectInputStream ois = new ObjectInputStream(bis);
        RaftGlobalSessionSyncMsg deserialized = (RaftGlobalSessionSyncMsg) ois.readObject();

        // Verify
        assertEquals(original.getMsgType(), deserialized.getMsgType());
        assertEquals(
                original.getGlobalSession().getXid(),
                deserialized.getGlobalSession().getXid());
    }
}
