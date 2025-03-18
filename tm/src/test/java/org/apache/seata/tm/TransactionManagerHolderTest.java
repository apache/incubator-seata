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
package org.apache.seata.tm;


import org.apache.seata.common.exception.ShouldNeverHappenException;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.core.model.TransactionManager;
import org.apache.seata.tm.api.transaction.MockTransactionManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TransactionManagerHolderTest {

    private TransactionManager backTransactionManager;

    @BeforeEach
    public void beforeEach() {
        backTransactionManager = TransactionManagerHolder.get();
    }

    @AfterEach
    public void afterEach() {
        TransactionManagerHolder.set(backTransactionManager);
    }


    @Test
    void getTest() {
        Assertions.assertThrows(ShouldNeverHappenException.class, () -> {
            TransactionManagerHolder.set(null);
            TransactionManagerHolder.get();
        });
    }


    @Test
    void getInstanceTest() {
        MockTransactionManager mockTransactionManager = new MockTransactionManager();
        TransactionManagerHolder.set(mockTransactionManager);
        TransactionManager transactionManager = TransactionManagerHolder.get();
        Assertions.assertInstanceOf(MockTransactionManager.class, transactionManager);
    }

}
