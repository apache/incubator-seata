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

import org.apache.seata.server.cluster.raft.sync.msg.dto.RaftClusterMetadata;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static org.junit.jupiter.api.Assertions.*;

public class RaftClusterMetadataMsgTest {

    @Test
    public void testDefaultConstructor() {
        RaftClusterMetadataMsg msg = new RaftClusterMetadataMsg();
        assertNotNull(msg);
        assertNull(msg.getRaftClusterMetadata());
    }

    @Test
    public void testConstructorWithMetadata() {
        RaftClusterMetadata metadata = new RaftClusterMetadata(123L);
        RaftClusterMetadataMsg msg = new RaftClusterMetadataMsg(metadata);

        assertEquals(RaftSyncMsgType.REFRESH_CLUSTER_METADATA, msg.getMsgType());
        assertEquals(metadata, msg.getRaftClusterMetadata());
    }

    @Test
    public void testSetAndGetRaftClusterMetadata() {
        RaftClusterMetadataMsg msg = new RaftClusterMetadataMsg();
        RaftClusterMetadata metadata = new RaftClusterMetadata(456L);
        msg.setRaftClusterMetadata(metadata);

        assertEquals(metadata, msg.getRaftClusterMetadata());
    }

    @Test
    public void testToString() {
        RaftClusterMetadata metadata = new RaftClusterMetadata(789L);
        RaftClusterMetadataMsg msg = new RaftClusterMetadataMsg(metadata);

        String str = msg.toString();
        assertNotNull(str);
        assertFalse(str.isEmpty());
    }

    @Test
    public void testSerialization() throws Exception {
        RaftClusterMetadata metadata = new RaftClusterMetadata(123L);
        RaftClusterMetadataMsg original = new RaftClusterMetadataMsg(metadata);

        // Serialize
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(original);
        oos.flush();
        byte[] serialized = bos.toByteArray();

        // Deserialize
        ByteArrayInputStream bis = new ByteArrayInputStream(serialized);
        ObjectInputStream ois = new ObjectInputStream(bis);
        RaftClusterMetadataMsg deserialized = (RaftClusterMetadataMsg) ois.readObject();

        // Verify
        assertEquals(original.getMsgType(), deserialized.getMsgType());
        assertEquals(
                original.getRaftClusterMetadata().getTerm(),
                deserialized.getRaftClusterMetadata().getTerm());
    }
}
