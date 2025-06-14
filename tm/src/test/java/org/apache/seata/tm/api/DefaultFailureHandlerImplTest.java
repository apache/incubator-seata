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


import io.netty.util.HashedWheelTimer;
import org.apache.seata.common.util.ReflectionUtil;
import org.apache.seata.core.context.RootContext;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.core.model.TransactionManager;
import org.apache.seata.tm.api.transaction.MyRuntimeException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;


class DefaultFailureHandlerImplTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultFailureHandlerImplTest.class);

    private static final String DEFAULT_XID = "1234567890";
    private static GlobalStatus globalStatus = GlobalStatus.Begin;

    private TransactionManager getTransactionManager() {
        return new TransactionManager() {
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
                return globalStatus;
            }

            @Override
            public GlobalStatus globalReport(String xid, GlobalStatus globalStatus) throws TransactionException {
                return globalStatus;
            }
        };
    }

    @Test
    void onBeginFailure() throws Exception {
        try {
            RootContext.bind(DEFAULT_XID);
            GlobalTransaction tx = GlobalTransactionContext.getCurrentOrCreate();
            ReflectionUtil.setFieldValue(tx, "transactionManager", getTransactionManager());

            FailureHandler failureHandler = new DefaultFailureHandlerImpl();
            failureHandler.onBeginFailure(tx, new MyRuntimeException("").getCause());
        } finally {
            RootContext.unbind();
        }
    }

    @Test
    void onCommitFailure() throws Exception {

        try {
            RootContext.bind(DEFAULT_XID);
            GlobalTransaction tx = GlobalTransactionContext.getCurrentOrCreate();
            //TransactionManagerHolder.set has interaction, using globalTransaction instance level tm
            ReflectionUtil.setFieldValue(tx, "transactionManager", getTransactionManager());

            FailureHandler failureHandler = new DefaultFailureHandlerImpl();
            failureHandler.onCommitFailure(tx, new MyRuntimeException("").getCause());

            // get timer
            Class<?> c = Class.forName("org.apache.seata.tm.api.DefaultFailureHandlerImpl");
            Field field = c.getDeclaredField("TIMER");
            field.setAccessible(true);
            HashedWheelTimer timer = (HashedWheelTimer) field.get(failureHandler);
            // assert timer pendingCount: first time is 1
            Long pendingTimeout = timer.pendingTimeouts();
            Assertions.assertEquals(pendingTimeout, 1L);
            //set globalStatus
            globalStatus = GlobalStatus.Committed;
            Thread.sleep(25 * 1000L);
            pendingTimeout = timer.pendingTimeouts();
            LOGGER.info("pendingTimeout {}", pendingTimeout);
            //all timer is done
            Assertions.assertEquals(pendingTimeout, 0L);
        } finally {
            RootContext.unbind();
        }
    }

    @Test
    void onRollbackFailure() throws Exception {


        try {
            RootContext.bind(DEFAULT_XID);
            GlobalTransaction tx = GlobalTransactionContext.getCurrentOrCreate();
            ReflectionUtil.setFieldValue(tx, "transactionManager", getTransactionManager());

            FailureHandler failureHandler = new DefaultFailureHandlerImpl();
            failureHandler.onRollbackFailure(tx, new MyRuntimeException("").getCause());

            // get timer
            Class<?> c = Class.forName("org.apache.seata.tm.api.DefaultFailureHandlerImpl");
            Field field = c.getDeclaredField("TIMER");
            field.setAccessible(true);
            HashedWheelTimer timer = (HashedWheelTimer) field.get(failureHandler);
            // assert timer pendingCount: first time is 1
            Long pendingTimeout = timer.pendingTimeouts();
            Assertions.assertEquals(pendingTimeout, 1L);
            //set globalStatus
            globalStatus = GlobalStatus.Rollbacked;
            Thread.sleep(25 * 1000L);
            pendingTimeout = timer.pendingTimeouts();
            LOGGER.info("pendingTimeout {}", pendingTimeout);
            //all timer is done
            Assertions.assertEquals(pendingTimeout, 0L);
        } finally {
            RootContext.unbind();
        }


    }


}
