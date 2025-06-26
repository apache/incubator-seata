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
import org.apache.seata.server.DynamicPortTestConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;

import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

import static org.apache.seata.common.DefaultValues.DEFAULT_TX_GROUP;

/**
 * The type Branch session test.
 *
 * @since 2019 /1/23
 */
@SpringBootTest
@Import(DynamicPortTestConfig.class)
public class BranchSessionTest {

    @BeforeAll
    public static void setUp(ApplicationContext context) {}

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
