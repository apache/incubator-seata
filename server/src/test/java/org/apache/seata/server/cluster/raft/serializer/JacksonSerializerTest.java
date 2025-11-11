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
package org.apache.seata.server.cluster.raft.serializer;

import org.apache.seata.server.cluster.raft.sync.msg.RaftBaseMsg;
import org.apache.seata.server.cluster.raft.sync.msg.RaftSyncMsgType;
import org.apache.seata.server.cluster.raft.sync.msg.dto.BranchTransactionDTO;
import org.apache.seata.server.cluster.raft.sync.msg.dto.GlobalTransactionDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class JacksonSerializerTest {

    private final JacksonSerializer serializer = new JacksonSerializer();

    @Test
    public void testSerializeAndDeserializeSimpleObject() {
        RaftBaseMsg original = new RaftBaseMsg();
        original.setMsgType(RaftSyncMsgType.ADD_GLOBAL_SESSION);
        original.setGroup("test-group");

        byte[] bytes = serializer.serialize(original);
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);

        RaftBaseMsg deserialized = serializer.deserialize(bytes);
        assertNotNull(deserialized);
        assertEquals(original.getMsgType(), deserialized.getMsgType());
        assertEquals(original.getGroup(), deserialized.getGroup());
    }

    @Test
    public void testSerializeAndDeserializeBranchTransactionDTO() {
        BranchTransactionDTO original = new BranchTransactionDTO("xid:123", 456L);
        original.setLockKey("table:1,2,3");

        byte[] bytes = serializer.serialize(original);
        assertNotNull(bytes);

        BranchTransactionDTO deserialized = serializer.deserialize(bytes);
        assertNotNull(deserialized);
        assertEquals(original.getXid(), deserialized.getXid());
        assertEquals(original.getBranchId(), deserialized.getBranchId());
        assertEquals(original.getLockKey(), deserialized.getLockKey());
    }

    @Test
    public void testSerializeAndDeserializeGlobalTransactionDTO() {
        GlobalTransactionDTO original = new GlobalTransactionDTO("xid:789");

        byte[] bytes = serializer.serialize(original);
        assertNotNull(bytes);

        GlobalTransactionDTO deserialized = serializer.deserialize(bytes);
        assertNotNull(deserialized);
        assertEquals(original.getXid(), deserialized.getXid());
    }

    @Test
    public void testSerializeNull() {
        assertThrows(RuntimeException.class, () -> {
            serializer.serialize(null);
        });
    }
}
