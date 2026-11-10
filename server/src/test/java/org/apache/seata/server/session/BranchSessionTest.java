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
package org.apache.seata.server.session;

import org.apache.seata.common.util.UUIDGenerator;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.server.BaseSpringBootTest;
import org.apache.seata.server.lock.LockManager;
import org.apache.seata.server.lock.LockerManagerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.context.ApplicationContext;

import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

import static org.apache.seata.common.DefaultValues.DEFAULT_TX_GROUP;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The type Branch session test.
 *
 * @since 2019 /1/23
 */
public class BranchSessionTest extends BaseSpringBootTest {

    @BeforeAll
    public static void setUp(ApplicationContext context) {}

    @AfterEach
    void afterEach() {
        LockerManagerFactory.destroy();
    }

    @org.junit.jupiter.api.Test
    void constructionDoesNotInitializeLockManagerAndOperationsResolveCurrentManager() throws Exception {
        LockerManagerFactory.destroy();
        BranchSession branchSession = new BranchSession(BranchType.AT);
        branchSession.setLockKey("t_order:1");

        java.lang.reflect.Field field = LockerManagerFactory.class.getDeclaredField("LOCK_MANAGER");
        field.setAccessible(true);
        Assertions.assertNull(field.get(null));

        LockManager first = mock(LockManager.class);
        when(first.acquireLock(branchSession, true, false)).thenReturn(true);
        field.set(null, first);
        Assertions.assertTrue(branchSession.lock());
        verify(first).acquireLock(branchSession, true, false);

        LockManager second = mock(LockManager.class);
        when(second.releaseLock(branchSession)).thenReturn(true);
        field.set(null, second);
        Assertions.assertTrue(branchSession.unlock());
        verify(second).releaseLock(branchSession);
    }

    /**
     * Codec test.
     *
     * @param branchSession the branch session
     */
    @ParameterizedTest
    @MethodSource("branchSessionProvider")
    public void codecTest(BranchSession branchSession) throws TransactionException {
        byte[] result = branchSession.encode();
        Assertions.assertNotNull(result);
        BranchSession expected = new BranchSession();
        expected.decode(result);
        Assertions.assertEquals(branchSession.getTransactionId(), expected.getTransactionId());
        Assertions.assertEquals(branchSession.getBranchId(), expected.getBranchId());
        Assertions.assertEquals(branchSession.getResourceId(), expected.getResourceId());
        Assertions.assertEquals(branchSession.getLockKey(), expected.getLockKey());
        Assertions.assertEquals(branchSession.getClientId(), expected.getClientId());
        Assertions.assertEquals(branchSession.getApplicationData(), expected.getApplicationData());
    }

    @ParameterizedTest
    @MethodSource("branchSessionProvider")
    public void checkSizeTest(BranchSession branchSession) throws TransactionException {
        Assertions.assertDoesNotThrow(branchSession::checkSize);
        int size = 28 * 1024;
        String alphanumeric = "!@#$%^&*()ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(size);
        for (int i = 0; i < size; i++) {
            sb.append(alphanumeric.charAt(ThreadLocalRandom.current().nextInt(alphanumeric.length())));
        }
        String str = sb.toString();
        branchSession.setLockKey(str);
        Assertions.assertThrows(TransactionException.class, branchSession::checkSize);
        branchSession.setLockKey(null);
        branchSession.setApplicationData(str);
        Assertions.assertThrows(TransactionException.class, branchSession::checkSize);
    }

    /**
     * Branch session provider object [ ] [ ].
     *
     * @return the object [ ] [ ]
     */
    static Stream<Arguments> branchSessionProvider() {
        BranchSession branchSession = new BranchSession();
        branchSession.setTransactionId(UUIDGenerator.generateUUID());
        branchSession.setBranchId(1L);
        branchSession.setClientId("c1");
        branchSession.setResourceGroupId(DEFAULT_TX_GROUP);
        branchSession.setResourceId("tb_1");
        branchSession.setLockKey("t_1");
        branchSession.setBranchType(BranchType.AT);
        branchSession.setApplicationData("{\"data\":\"test\"}");
        branchSession.setBranchType(BranchType.AT);
        return Stream.of(Arguments.of(branchSession));
    }
}
