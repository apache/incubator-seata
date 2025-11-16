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

import org.apache.seata.server.cluster.raft.sync.msg.dto.BranchTransactionDTO;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static org.apache.seata.common.DefaultValues.DEFAULT_SEATA_GROUP;
import static org.junit.jupiter.api.Assertions.*;

public class RaftBranchSessionSyncMsgTest {

    @Test
    public void testDefaultConstructor() {
        RaftBranchSessionSyncMsg msg = new RaftBranchSessionSyncMsg();
        assertNotNull(msg);
        assertEquals(DEFAULT_SEATA_GROUP, msg.getGroup());
        assertNull(msg.getBranchSession());
        assertNull(msg.getMsgType());
    }

    @Test
    public void testConstructorWithParameters() {
        BranchTransactionDTO dto = new BranchTransactionDTO("xid:123", 456L);
        RaftBranchSessionSyncMsg msg = new RaftBranchSessionSyncMsg(RaftSyncMsgType.ADD_BRANCH_SESSION, dto);

        assertEquals(RaftSyncMsgType.ADD_BRANCH_SESSION, msg.getMsgType());
        assertEquals(dto, msg.getBranchSession());
    }

    @Test
    public void testSetAndGetBranchSession() {
        RaftBranchSessionSyncMsg msg = new RaftBranchSessionSyncMsg();
        BranchTransactionDTO dto = new BranchTransactionDTO("xid:789", 101L);
        msg.setBranchSession(dto);

        assertEquals(dto, msg.getBranchSession());
    }

    @Test
    public void testSetAndGetGroup() {
        RaftBranchSessionSyncMsg msg = new RaftBranchSessionSyncMsg();
        msg.setGroup("custom-group");
        assertEquals("custom-group", msg.getGroup());
    }

    @Test
    public void testToString() {
        BranchTransactionDTO dto = new BranchTransactionDTO("xid:123", 456L);
        RaftBranchSessionSyncMsg msg = new RaftBranchSessionSyncMsg(RaftSyncMsgType.UPDATE_BRANCH_SESSION_STATUS, dto);

        String str = msg.toString();
        assertNotNull(str);
        assertFalse(str.isEmpty());
    }

    @Test
    public void testSerialization() throws Exception {
        BranchTransactionDTO dto = new BranchTransactionDTO("xid:123", 456L);
        dto.setLockKey("table:1,2,3");
        RaftBranchSessionSyncMsg original = new RaftBranchSessionSyncMsg(RaftSyncMsgType.ADD_BRANCH_SESSION, dto);
        original.setGroup("test-group");

        // Serialize
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(original);
        oos.flush();
        byte[] serialized = bos.toByteArray();

        // Deserialize
        ByteArrayInputStream bis = new ByteArrayInputStream(serialized);
        ObjectInputStream ois = new ObjectInputStream(bis);
        RaftBranchSessionSyncMsg deserialized = (RaftBranchSessionSyncMsg) ois.readObject();

        // Verify
        assertEquals(original.getMsgType(), deserialized.getMsgType());
        assertEquals(original.getGroup(), deserialized.getGroup());
        assertEquals(
                original.getBranchSession().getXid(),
                deserialized.getBranchSession().getXid());
        assertEquals(
                original.getBranchSession().getBranchId(),
                deserialized.getBranchSession().getBranchId());
    }
}
