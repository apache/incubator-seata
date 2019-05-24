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
package io.seata.server.lock;

import io.seata.core.model.BranchType;
import io.seata.server.UUIDGenerator;
import io.seata.server.lock.memory.MemoryLockManagerForTest;
import io.seata.server.session.BranchSession;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

/**
 * The type Lock manager test.
 *
 * @author tianming.xm @gmail.com
 * @since 2019 /1/23
 */
public class LockManagerTest {

    /**
     * Acquire lock success.
     *
     * @param branchSession the branch session
     * @throws Exception the exception
     */
    @ParameterizedTest
    @MethodSource("branchSessionProvider")
    public void acquireLock_success(BranchSession branchSession) throws Exception {
        LockManager lockManager = new MemoryLockManagerForTest();
        Assertions.assertTrue(lockManager.acquireLock(branchSession));
    }

    /**
     * Acquire lock failed.
     *
     * @param branchSession1 the branch session 1
     * @param branchSession2 the branch session 2
     * @throws Exception the exception
     */
    @ParameterizedTest
    @MethodSource("branchSessionsProvider")
    public void acquireLock_failed(BranchSession branchSession1, BranchSession branchSession2) throws Exception {
        LockManager lockManager = new MemoryLockManagerForTest();
        Assertions.assertTrue(lockManager.acquireLock(branchSession1));
        Assertions.assertFalse(lockManager.acquireLock(branchSession2));
    }

    /**
     * Is lockable test.
     *
     * @param branchSession the branch session
     * @throws Exception the exception
     */
    @ParameterizedTest
    @MethodSource("branchSessionProvider")
    public void isLockableTest(BranchSession branchSession) throws Exception {
        branchSession.setLockKey("t:4");
        LockManager lockManager = new MemoryLockManagerForTest();
        Assertions.assertTrue(lockManager
                .isLockable(branchSession.getXid(), branchSession.getResourceId(), branchSession.getLockKey()));
        lockManager.acquireLock(branchSession);
        branchSession.setTransactionId(UUIDGenerator.generateUUID());
        Assertions.assertFalse(lockManager
                .isLockable(branchSession.getXid(), branchSession.getResourceId(), branchSession.getLockKey()));
    }

    /**
     * Branch session provider object [ ] [ ].
     *
     * @return the object [ ] [ ]
     */
    static Stream<Arguments> branchSessionProvider() {
        BranchSession branchSession = new BranchSession();
        branchSession.setTransactionId(UUIDGenerator.generateUUID());
        branchSession.setBranchId(0L);
        branchSession.setClientId("c1");
        branchSession.setResourceGroupId("my_test_tx_group");
        branchSession.setResourceId("tb_1");
        branchSession.setLockKey("t:0");
        branchSession.setBranchType(BranchType.AT);
        branchSession.setApplicationData("{\"data\":\"test\"}");
        branchSession.setBranchType(BranchType.AT);
        return Stream.of(
                Arguments.of(branchSession));
    }

    /**
     * Branch sessions provider object [ ] [ ].
     *
     * @return the object [ ] [ ]
     */
    static Stream<Arguments> branchSessionsProvider() {
        BranchSession branchSession1 = new BranchSession();
        branchSession1.setTransactionId(UUIDGenerator.generateUUID());
        branchSession1.setBranchId(1L);
        branchSession1.setClientId("c1");
        branchSession1.setResourceGroupId("my_test_tx_group");
        branchSession1.setResourceId("tb_1");
        branchSession1.setLockKey("t:1,2");
        branchSession1.setBranchType(BranchType.AT);
        branchSession1.setApplicationData("{\"data\":\"test\"}");
        branchSession1.setBranchType(BranchType.AT);

        BranchSession branchSession2 = new BranchSession();
        branchSession2.setTransactionId(UUIDGenerator.generateUUID());
        branchSession2.setBranchId(2L);
        branchSession2.setClientId("c1");
        branchSession2.setResourceGroupId("my_test_tx_group");
        branchSession2.setResourceId("tb_1");
        branchSession2.setLockKey("t:1,2");
        branchSession2.setBranchType(BranchType.AT);
        branchSession2.setApplicationData("{\"data\":\"test\"}");
        branchSession2.setBranchType(BranchType.AT);
        return Stream.of(
                Arguments.of(branchSession1, branchSession2));
    }
}
