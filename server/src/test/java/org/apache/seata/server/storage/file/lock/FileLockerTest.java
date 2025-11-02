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
package org.apache.seata.server.storage.file.lock;

import org.apache.seata.common.XID;
import org.apache.seata.core.lock.AbstractLocker;
import org.apache.seata.core.model.BranchStatus;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.server.BaseSpringBootTest;
import org.apache.seata.server.session.BranchSession;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

public class FileLockerTest extends BaseSpringBootTest {

    private FileLocker fileLocker;
    private BranchSession branchSession;

    @BeforeEach
    public void setUp() {
        branchSession = createBranchSession();
        fileLocker = new FileLocker(branchSession);
    }

    @Test
    public void testInstanceCreation() {
        Assertions.assertNotNull(fileLocker);
    }

    @Test
    public void testExtendsAbstractLocker() {
        Assertions.assertTrue(fileLocker instanceof AbstractLocker);
    }

    @Test
    public void testAcquireLockWithEmptyList() {
        boolean result = fileLocker.acquireLock(new ArrayList<>());
        Assertions.assertTrue(result);
    }

    @Test
    public void testAcquireLockWithAutoCommitAndEmptyList() {
        boolean result = fileLocker.acquireLock(new ArrayList<>(), true, false);
        Assertions.assertTrue(result);
    }

    private BranchSession createBranchSession() {
        BranchSession session = new BranchSession();
        String xid = XID.generateXID(12345L);
        session.setXid(xid);
        session.setTransactionId(12345L);
        session.setBranchId(1L);
        session.setResourceGroupId("test-group");
        session.setResourceId("test-resource");
        session.setLockKey("test:1");
        session.setBranchType(BranchType.AT);
        session.setStatus(BranchStatus.Registered);
        session.setClientId("test-client:127.0.0.1:8080");
        session.setApplicationData("test-branch-data");
        return session;
    }
}
