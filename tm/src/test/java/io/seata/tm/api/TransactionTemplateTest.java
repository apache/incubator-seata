/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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
package io.seata.tm.api;

import io.seata.core.model.GlobalStatus;
import io.seata.core.model.TransactionManager;
import io.seata.tm.DefaultTransactionManager;
import io.seata.tm.api.transaction.TransactionHook;
import io.seata.tm.api.transaction.TransactionHookManager;

import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author guoyao
 * @date 2019/3/6
 */
public class TransactionTemplateTest {

    private static final String DEFAULT_XID = "123456789";
    private static final String DEFAULT_NAME = "test";
    private static final int DEFAULT_TIME_OUT = 30000;

    @Before
    public void init() throws Exception {
        TransactionManager transactionManager = mock(TransactionManager.class);
        when(transactionManager.begin(null, null, DEFAULT_NAME, DEFAULT_TIME_OUT)).thenReturn(DEFAULT_XID);
        when(transactionManager.commit(DEFAULT_XID)).thenReturn(GlobalStatus.Committed);
        when(transactionManager.rollback(DEFAULT_XID)).thenReturn(GlobalStatus.Rollbacked);
        when(transactionManager.getStatus(DEFAULT_XID)).thenReturn(GlobalStatus.Begin);
        DefaultTransactionManager.set(transactionManager);
    }

    @After
    public void assertHooks() {
        Assertions.assertThat(TransactionHookManager.getHooks()).isEmpty();
    }

    @Test
    public void testTransactionCommitHook() throws Exception {
        TransactionHook transactionHook = Mockito.mock(TransactionHook.class);
        TransactionalExecutor transactionalExecutor = Mockito.mock(TransactionalExecutor.class);
        when(transactionalExecutor.name()).thenReturn(DEFAULT_NAME);
        when(transactionalExecutor.timeout()).thenReturn(DEFAULT_TIME_OUT);
        TransactionHookManager.registerHook(transactionHook);
        TransactionalTemplate template = new TransactionalTemplate();
        template.execute(transactionalExecutor);
        verify(transactionHook).beforeBegin();
        verify(transactionHook).afterBegin();
        verify(transactionHook).beforeCommit();
        verify(transactionHook).afterCommit();
        verify(transactionHook).afterCompletion();
    }

    @Test
    public void testTransactionRollbackHook() throws Throwable {
        TransactionHook transactionHook = Mockito.mock(TransactionHook.class);
        TransactionalExecutor transactionalExecutor = Mockito.mock(TransactionalExecutor.class);
        when(transactionalExecutor.name()).thenReturn(DEFAULT_NAME);
        when(transactionalExecutor.timeout()).thenReturn(DEFAULT_TIME_OUT);
        when(transactionalExecutor.execute()).thenThrow(new RuntimeException());
        TransactionHookManager.registerHook(transactionHook);
        TransactionalTemplate template = new TransactionalTemplate();
        try {
            template.execute(transactionalExecutor);
        } catch (Exception e) {
            //catch rollback exception
        }
        verify(transactionHook).beforeBegin();
        verify(transactionHook).afterBegin();
        verify(transactionHook).beforeRollback();
        verify(transactionHook).afterRollback();
        verify(transactionHook).afterCompletion();
    }

}
