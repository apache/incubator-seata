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
package org.apache.seata.server.lock.file;

import java.util.stream.Stream;

import org.apache.seata.common.XID;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.common.util.UUIDGenerator;
import org.apache.seata.server.lock.LockManager;
import org.apache.seata.server.session.BranchSession;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.apache.seata.common.DefaultValues.DEFAULT_TX_GROUP;


/**
 * The type Default lock manager impl test.
 *
 * @since 2019 /1/23
 */
@SpringBootTest
public class FileLockManagerImplTest {

    private LockManager lockManager = new FileLockManagerForTest();

    private static final long transactionId = UUIDGenerator.generateUUID();

    private static final String resourceId = "tb_1";

    private static final String lockKey = "tb_1:13";


    @BeforeAll
    public static void setup(ApplicationContext context){

    }
    /**
     * Acquire lock test.
     *
     * @param branchSession the branch session
     * @throws Exception the exception
     */
    @ParameterizedTest
    @MethodSource("branchSessionProvider")
    public void acquireLockTest(BranchSession branchSession) throws Exception {

        boolean result = lockManager.acquireLock(branchSession);
        Assertions.assertTrue(result);
        branchSession.unlock();
    }

    /**
     * Is lockable test.
     *
     * @throws Exception the exception
     */
    @Test
    public void isLockableTest() throws Exception {
        boolean resultOne = lockManager.isLockable(XID.generateXID(transactionId), resourceId, lockKey);

        Assertions.assertTrue(resultOne);
    }

    /**
     * Branch session provider object [ ] [ ].
     *
     * @return Stream<BranchSession>
     */
    static Stream<BranchSession> branchSessionProvider() {
        BranchSession branchSession = new BranchSession();
        branchSession.setXid(XID.generateXID(transactionId));
        branchSession.setBranchId(1L);
        branchSession.setTransactionId(transactionId);
        branchSession.setClientId("c1");
        branchSession.setResourceGroupId(DEFAULT_TX_GROUP);
        branchSession.setResourceId(resourceId);
        branchSession.setLockKey(lockKey);
        branchSession.setBranchType(BranchType.AT);
        branchSession.setApplicationData("{\"data\":\"test\"}");
        return Stream.of(branchSession);
    }

}
