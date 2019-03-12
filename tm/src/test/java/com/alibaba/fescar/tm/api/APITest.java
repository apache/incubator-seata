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
package com.alibaba.fescar.tm.api;

import com.alibaba.fescar.core.context.RootContext;
import com.alibaba.fescar.core.exception.TransactionException;
import com.alibaba.fescar.core.model.GlobalStatus;
import com.alibaba.fescar.core.model.TransactionManager;
import com.alibaba.fescar.tm.DefaultTransactionManager;

import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * The type Api test.
 */
public class APITest {

    private static final String DEFAULT_XID = "1234567890";

    /**
     * Init.
     */
    @BeforeClass
    public static void init() {
        DefaultTransactionManager.set(new TransactionManager() {
            @Override
            public String begin(String applicationId, String transactionServiceGroup, String name, int timeout)
                throws TransactionException {
                return DEFAULT_XID;
            }

            @Override
            public GlobalStatus commit(String xid) throws TransactionException {
                return GlobalStatus.Committed;
            }

            @Override
            public GlobalStatus rollback(String xid) throws TransactionException {
                return GlobalStatus.Rollbacked;
            }

            @Override
            public GlobalStatus getStatus(String xid) throws TransactionException {
                return GlobalStatus.Begin;
            }
        });
    }

    /**
     * Clean root context.
     */
    @After
    public void cleanRootContext() {
        RootContext.unbind();
    }

    /**
     * Test current.
     *
     * @throws Exception the exception
     */
    @Test
    public void testCurrent() throws Exception {
        RootContext.bind(DEFAULT_XID);
        GlobalTransaction tx = GlobalTransactionContext.getCurrentOrCreate();
        Assert.assertEquals(tx.getXid(), DEFAULT_XID);
        Assert.assertEquals(tx.getStatus(), GlobalStatus.Begin);

    }

    /**
     * Test new tx.
     *
     * @throws Exception the exception
     */
    @Test
    public void testNewTx() throws Exception {
        GlobalTransaction tx = GlobalTransactionContext.getCurrentOrCreate();
        Assert.assertEquals(tx.getStatus(), GlobalStatus.UnKnown);
        Assert.assertNull(tx.getXid());
    }

    /**
     * Test begin.
     *
     * @throws Exception the exception
     */
    @Test
    public void testBegin() throws Exception {
        GlobalTransaction tx = GlobalTransactionContext.getCurrentOrCreate();
        tx.begin();
        Assert.assertEquals(tx.getStatus(), GlobalStatus.Begin);
        Assert.assertNotNull(tx.getXid());

    }

    /**
     * Test nested commit.
     *
     * @throws Exception the exception
     */
    @Test
    public void testNestedCommit() throws Exception {
        TransactionalTemplate template = new TransactionalTemplate();
        template.execute(new AbstractTransactionalExecutor() {
            @Override
            public Object execute() throws Throwable {

                TransactionalTemplate template = new TransactionalTemplate();
                template.execute(new AbstractTransactionalExecutor() {
                    @Override
                    public Object execute() throws Throwable {

                        TransactionalTemplate template = new TransactionalTemplate();
                        template.execute(new AbstractTransactionalExecutor() {
                            @Override
                            public Object execute() throws Throwable {
                                return null;
                            }
                        });
                        return null;
                    }
                });
                return null;
            }
        });
    }

    /**
     * Test nested rollback.
     *
     * @throws Exception the exception
     */
    @Test
    public void testNestedRollback() throws Exception {

        final String oexMsg = "xxx";

        TransactionalTemplate template = new TransactionalTemplate();
        try {
            template.execute(new AbstractTransactionalExecutor() {
                @Override
                public Object execute() throws Throwable {

                    TransactionalTemplate template = new TransactionalTemplate();
                    try {
                        template.execute(new AbstractTransactionalExecutor() {
                            @Override
                            public Object execute() throws Throwable {

                                TransactionalTemplate template = new TransactionalTemplate();
                                try {
                                    template.execute(new AbstractTransactionalExecutor() {
                                        @Override
                                        public Object execute() throws Throwable {
                                            throw new RuntimeException(oexMsg);
                                        }
                                    });
                                } catch (TransactionalExecutor.ExecutionException ex) {
                                    throw ex.getOriginalException();
                                }
                                return null;
                            }
                        });
                    } catch (TransactionalExecutor.ExecutionException ex) {
                        throw ex.getOriginalException();
                    }
                    return null;
                }
            });
        } catch (TransactionalExecutor.ExecutionException ex) {
            Throwable oex = ex.getOriginalException();
            Assert.assertEquals(oex.getMessage(), oexMsg);
        }
    }

    private static abstract class AbstractTransactionalExecutor implements TransactionalExecutor {

        @Override
        public int timeout() {
            return 30000;
        }

        @Override
        public String name() {
            return "test";
        }
    }
}
