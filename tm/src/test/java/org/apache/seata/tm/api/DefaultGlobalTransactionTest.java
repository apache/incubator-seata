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
package org.apache.seata.tm.api;


import org.apache.seata.common.XID;
import org.apache.seata.common.util.UUIDGenerator;
import org.apache.seata.core.context.RootContext;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.core.model.TransactionManager;
import org.apache.seata.tm.TransactionManagerHolder;
import org.apache.seata.tm.api.transaction.MyRuntimeException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


class DefaultGlobalTransactionTest {

    private TransactionManager backTransactionManager;

    @BeforeEach
    public void init() {
        backTransactionManager = TransactionManagerHolder.get();

        TransactionManagerHolder.set(new TransactionManager() {
            @Override
            public String begin(String applicationId, String transactionServiceGroup, String name, int timeout)
                    throws TransactionException {
                return XID.generateXID(UUIDGenerator.generateUUID());
            }

            @Override
            public GlobalStatus commit(String xid) throws TransactionException {
                throw new MyRuntimeException("");
            }

            @Override
            public GlobalStatus rollback(String xid) throws TransactionException {
                throw new MyRuntimeException("");
            }

            @Override
            public GlobalStatus getStatus(String xid) throws TransactionException {
                throw new MyRuntimeException("");
            }

            @Override
            public GlobalStatus globalReport(String xid, GlobalStatus globalStatus) throws TransactionException {
                return globalStatus;
            }
        });
        RootContext.unbind();
    }


    @Test
    public void commitRetryExceptionTest() throws TransactionException {
        GlobalTransaction tx = GlobalTransactionContext.getCurrentOrCreate();
        tx.begin();
        Assertions.assertThrows(TransactionException.class, tx::commit);
    }

    @Test
    public void commitNoXIDExceptionTest() throws TransactionException {
        GlobalTransaction tx = GlobalTransactionContext.getCurrentOrCreate();
        Assertions.assertThrows(IllegalStateException.class, tx::commit);
    }


    @Test
    public void rollBackRetryExceptionTest() throws TransactionException {
        GlobalTransaction tx = GlobalTransactionContext.getCurrentOrCreate();
        tx.begin();
        Assertions.assertThrows(TransactionException.class, tx::rollback);
    }

    @Test
    public void rollBackNoXIDExceptionTest() throws TransactionException {
        GlobalTransaction tx = GlobalTransactionContext.getCurrentOrCreate();
        tx.begin();
        Assertions.assertThrows(TransactionException.class, tx::rollback);
    }

    @AfterEach
    public void afterEach() {
        TransactionManagerHolder.set(backTransactionManager);
    }

}
