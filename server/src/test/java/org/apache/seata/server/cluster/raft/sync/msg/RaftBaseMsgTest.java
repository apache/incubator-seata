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

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static org.apache.seata.common.DefaultValues.DEFAULT_SEATA_GROUP;
import static org.junit.jupiter.api.Assertions.*;

public class RaftBaseMsgTest {

    @Test
    public void testDefaultValues() {
        RaftBaseMsg msg = new RaftBaseMsg();
        assertEquals(DEFAULT_SEATA_GROUP, msg.getGroup());
        assertNull(msg.getMsgType());
    }

    @Test
    public void testSetAndGetMsgType() {
        RaftBaseMsg msg = new RaftBaseMsg();
        msg.setMsgType(RaftSyncMsgType.ADD_GLOBAL_SESSION);
        assertEquals(RaftSyncMsgType.ADD_GLOBAL_SESSION, msg.getMsgType());
    }

    @Test
    public void testSetAndGetGroup() {
        RaftBaseMsg msg = new RaftBaseMsg();
        msg.setGroup("test-group");
        assertEquals("test-group", msg.getGroup());
    }

    @Test
    public void testSerialization() throws Exception {
        RaftBaseMsg original = new RaftBaseMsg();
        original.setMsgType(RaftSyncMsgType.ADD_BRANCH_SESSION);
        original.setGroup("custom-group");

        // Serialize
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(original);
        oos.flush();
        byte[] serialized = bos.toByteArray();

        // Deserialize
        ByteArrayInputStream bis = new ByteArrayInputStream(serialized);
        ObjectInputStream ois = new ObjectInputStream(bis);
        RaftBaseMsg deserialized = (RaftBaseMsg) ois.readObject();

        // Verify
        assertEquals(original.getMsgType(), deserialized.getMsgType());
        assertEquals(original.getGroup(), deserialized.getGroup());
    }
}
