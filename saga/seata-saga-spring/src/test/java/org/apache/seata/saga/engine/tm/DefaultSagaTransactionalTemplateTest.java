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

import java.util.Collections;

import org.apache.seata.core.model.BranchStatus;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.core.model.Resource;
import org.apache.seata.core.model.ResourceManager;
import org.apache.seata.rm.DefaultResourceManager;
import org.apache.seata.tm.api.GlobalTransactionContext;
import org.apache.seata.tm.api.transaction.TransactionHookManager;
import org.apache.seata.tm.api.transaction.TransactionInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;

/**
 * DefaultSagaTransactionalTemplateTest
 */
public class DefaultSagaTransactionalTemplateTest {
    private static SagaTransactionalTemplate sagaTransactionalTemplate;

    @BeforeEach
    public void init() {
        sagaTransactionalTemplate = new DefaultSagaTransactionalTemplate();
    }

    @Test
    public void testCommitTransaction() {
        MockGlobalTransaction mockGlobalTransaction = new MockGlobalTransaction();
        Assertions.assertDoesNotThrow(() -> sagaTransactionalTemplate.commitTransaction(mockGlobalTransaction));
    }

    @Test
    public void testRollbackTransaction() {
        MockedStatic<TransactionHookManager> enhancedTransactionHookManager = Mockito.mockStatic(TransactionHookManager.class);
        enhancedTransactionHookManager.when(TransactionHookManager::getHooks).thenReturn(Collections.singletonList(new MockTransactionHook()));
        MockGlobalTransaction mockGlobalTransaction = new MockGlobalTransaction();
        Assertions.assertDoesNotThrow(() -> sagaTransactionalTemplate.rollbackTransaction(mockGlobalTransaction, null));
        enhancedTransactionHookManager.close();
    }

    @Test
    public void testBeginTransaction() {
        MockedStatic<GlobalTransactionContext> enhancedServiceLoader = Mockito.mockStatic(GlobalTransactionContext.class);
        enhancedServiceLoader.when(GlobalTransactionContext::getCurrentOrCreate).thenReturn(new MockGlobalTransaction());
        MockedStatic<TransactionHookManager> enhancedTransactionHookManager = Mockito.mockStatic(TransactionHookManager.class);
        enhancedTransactionHookManager.when(TransactionHookManager::getHooks).thenReturn(Collections.singletonList(new MockTransactionHook()));
        TransactionInfo transactionInfo = new TransactionInfo();
        Assertions.assertDoesNotThrow(() -> sagaTransactionalTemplate.beginTransaction(transactionInfo));
        enhancedServiceLoader.close();
        enhancedTransactionHookManager.close();
    }

    @Test
    public void testReloadTransaction() {
        Assertions.assertDoesNotThrow(() -> sagaTransactionalTemplate.reloadTransaction(""));
    }

    @Test
    public void testReportTransaction() {
        MockGlobalTransaction mockGlobalTransaction = new MockGlobalTransaction();
        GlobalStatus globalStatus = GlobalStatus.Committed;
        Assertions.assertDoesNotThrow(() -> sagaTransactionalTemplate.reportTransaction(mockGlobalTransaction, globalStatus));
    }

    @Test
    public void testBranchRegister() {
        ResourceManager resourceManager = Mockito.mock(ResourceManager.class);
        Mockito.doNothing().when(resourceManager).registerResource(any(Resource.class));
        DefaultResourceManager.get();
        DefaultResourceManager.mockResourceManager(BranchType.SAGA, resourceManager);
        Assertions.assertDoesNotThrow(() -> sagaTransactionalTemplate.branchRegister("",
                "", "", "", ""));
    }

    @Test
    public void testBranchReport() {
        ResourceManager resourceManager = Mockito.mock(ResourceManager.class);
        Mockito.doNothing().when(resourceManager).registerResource(any(Resource.class));
        DefaultResourceManager.get();
        DefaultResourceManager.mockResourceManager(BranchType.SAGA, resourceManager);
        Assertions.assertDoesNotThrow(() -> sagaTransactionalTemplate.branchReport("",
                0, BranchStatus.Unknown, ""));
    }

    @Test
    public void testTriggerAfterCompletion() {
        MockedStatic<TransactionHookManager> enhancedTransactionHookManager = Mockito.mockStatic(TransactionHookManager.class);
        enhancedTransactionHookManager.when(TransactionHookManager::getHooks).thenReturn(Collections.singletonList(new MockTransactionHook()));
        MockGlobalTransaction mockGlobalTransaction = new MockGlobalTransaction();
        Assertions.assertDoesNotThrow(() -> sagaTransactionalTemplate.triggerAfterCompletion(mockGlobalTransaction));
        enhancedTransactionHookManager.close();
    }

    @Test
    public void testCleanUp() {
        MockGlobalTransaction mockGlobalTransaction = new MockGlobalTransaction();
        sagaTransactionalTemplate.cleanUp(mockGlobalTransaction);
    }
}