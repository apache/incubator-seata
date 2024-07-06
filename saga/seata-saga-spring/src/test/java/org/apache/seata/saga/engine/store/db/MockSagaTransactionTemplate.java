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
package org.apache.seata.saga.engine.store.db;

import java.util.Random;

import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.model.BranchStatus;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.saga.engine.tm.MockGlobalTransaction;
import org.apache.seata.saga.engine.tm.SagaTransactionalTemplate;
import org.apache.seata.tm.api.GlobalTransaction;
import org.apache.seata.tm.api.TransactionalExecutor.ExecutionException;
import org.apache.seata.tm.api.transaction.TransactionInfo;

/**
 * MockSagaTransactionTemplate
 */
public class MockSagaTransactionTemplate implements SagaTransactionalTemplate {

    @Override
    public void commitTransaction(GlobalTransaction tx) throws ExecutionException {

    }

    @Override
    public void rollbackTransaction(GlobalTransaction tx, Throwable ex) throws TransactionException, ExecutionException {

    }

    @Override
    public GlobalTransaction beginTransaction(TransactionInfo txInfo) throws ExecutionException {
        GlobalTransaction globalTransaction = new MockGlobalTransaction();
        try {
            globalTransaction.begin();
        } catch (TransactionException e) {
            e.printStackTrace();
        }
        return globalTransaction;
    }

    @Override
    public GlobalTransaction reloadTransaction(String xid) throws ExecutionException, TransactionException {
        return new MockGlobalTransaction(xid, GlobalStatus.UnKnown);
    }

    @Override
    public void reportTransaction(GlobalTransaction tx, GlobalStatus globalStatus) throws ExecutionException {

    }

    @Override
    public long branchRegister(String resourceId, String clientId, String xid, String applicationData, String lockKeys)
            throws TransactionException {
        return new Random().nextLong();
    }

    @Override
    public void branchReport(String xid, long branchId, BranchStatus status, String applicationData) throws TransactionException {
        
    }

    @Override
    public void triggerAfterCompletion(GlobalTransaction tx) {

    }

    @Override
    public void cleanUp(GlobalTransaction tx) {

    }
}
