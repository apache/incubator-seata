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
package org.apache.seata.saga.engine.tm;

import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.model.BranchStatus;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.tm.api.GlobalTransaction;
import org.apache.seata.tm.api.TransactionalExecutor;
import org.apache.seata.tm.api.transaction.TransactionInfo;

/**
 * Template of executing business logic with a global transaction for SAGA mode
 *
 */
public interface SagaTransactionalTemplate {

    void commitTransaction(GlobalTransaction tx) throws TransactionalExecutor.ExecutionException;

    void rollbackTransaction(GlobalTransaction tx, Throwable ex)
        throws TransactionException, TransactionalExecutor.ExecutionException;

    GlobalTransaction beginTransaction(TransactionInfo txInfo) throws TransactionalExecutor.ExecutionException;

    GlobalTransaction reloadTransaction(String xid)
        throws TransactionalExecutor.ExecutionException, TransactionException;

    void reportTransaction(GlobalTransaction tx, GlobalStatus globalStatus)
        throws TransactionalExecutor.ExecutionException;

    long branchRegister(String resourceId, String clientId, String xid, String applicationData, String lockKeys)
        throws TransactionException;

    void branchReport(String xid, long branchId, BranchStatus status, String applicationData)
        throws TransactionException;

    void triggerAfterCompletion(GlobalTransaction tx);

    void cleanUp(GlobalTransaction tx);
}
