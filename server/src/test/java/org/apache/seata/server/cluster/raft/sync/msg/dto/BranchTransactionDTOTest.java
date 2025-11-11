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

public class BranchTransactionDTOTest {

    @Test
    public void testDefaultConstructor() {
        BranchTransactionDTO dto = new BranchTransactionDTO();
        assertNotNull(dto);
        assertNull(dto.getLockKey());
    }

    @Test
    public void testConstructorWithParameters() {
        String xid = "192.168.1.1:8091:12345678";
        long branchId = 123456L;
        BranchTransactionDTO dto = new BranchTransactionDTO(xid, branchId);

        assertEquals(xid, dto.getXid());
        assertEquals(branchId, dto.getBranchId());
        assertNull(dto.getLockKey());
    }

    @Test
    public void testSetAndGetLockKey() {
        BranchTransactionDTO dto = new BranchTransactionDTO();
        String lockKey = "table:1,2,3";
        dto.setLockKey(lockKey);
        assertEquals(lockKey, dto.getLockKey());
    }

    @Test
    public void testSerialization() throws Exception {
        BranchTransactionDTO original = new BranchTransactionDTO("xid:123", 456L);
        original.setLockKey("table:lock:keys");

        // Serialize
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(original);
        oos.flush();
        byte[] serialized = bos.toByteArray();

        // Deserialize
        ByteArrayInputStream bis = new ByteArrayInputStream(serialized);
        ObjectInputStream ois = new ObjectInputStream(bis);
        BranchTransactionDTO deserialized = (BranchTransactionDTO) ois.readObject();

        // Verify
        assertEquals(original.getXid(), deserialized.getXid());
        assertEquals(original.getBranchId(), deserialized.getBranchId());
        assertEquals(original.getLockKey(), deserialized.getLockKey());
    }
}
