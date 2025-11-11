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
package org.apache.seata.server.cluster.raft.sync.msg.dto;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static org.junit.jupiter.api.Assertions.*;

public class GlobalTransactionDTOTest {

    @Test
    public void testDefaultConstructor() {
        GlobalTransactionDTO dto = new GlobalTransactionDTO();
        assertNotNull(dto);
    }

    @Test
    public void testConstructorWithXid() {
        String xid = "192.168.1.1:8091:12345678";
        GlobalTransactionDTO dto = new GlobalTransactionDTO(xid);

        assertEquals(xid, dto.getXid());
    }

    @Test
    public void testSerialization() throws Exception {
        GlobalTransactionDTO original = new GlobalTransactionDTO("xid:123456");

        // Serialize
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(original);
        oos.flush();
        byte[] serialized = bos.toByteArray();

        // Deserialize
        ByteArrayInputStream bis = new ByteArrayInputStream(serialized);
        ObjectInputStream ois = new ObjectInputStream(bis);
        GlobalTransactionDTO deserialized = (GlobalTransactionDTO) ois.readObject();

        // Verify
        assertEquals(original.getXid(), deserialized.getXid());
    }
}
