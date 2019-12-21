/*
 *  Copyright 1999-2019 Seata.io Group.
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


import io.netty.util.HashedWheelTimer;
import io.seata.core.context.RootContext;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.GlobalStatus;
import io.seata.core.model.TransactionManager;
import io.seata.tm.TransactionManagerHolder;
import io.seata.tm.api.transaction.MyRuntimeException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;

/**
 * @author wangwei
 */
class DefaultFailureHandlerImplTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultFailureHandlerImplTest.class);

    private static final String DEFAULT_XID = "1234567890";
    private static GlobalStatus globalStatus = GlobalStatus.Begin;

    @BeforeAll
    public static void init() {

        TransactionManagerHolder.set(new TransactionManager() {
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
        });
    }

    @Test
    void onBeginFailure() {
        RootContext.bind(DEFAULT_XID);
        GlobalTransaction tx = GlobalTransactionContext.getCurrentOrCreate();
        FailureHandler failureHandler = new DefaultFailureHandlerImpl();
        failureHandler.onBeginFailure(tx, new MyRuntimeException("").getCause());
    }

    @Test
    void onCommitFailure() throws Exception{

        RootContext.bind(DEFAULT_XID);
        GlobalTransaction tx = GlobalTransactionContext.getCurrentOrCreate();
        FailureHandler failureHandler = new DefaultFailureHandlerImpl();
        failureHandler.onCommitFailure(tx, new MyRuntimeException("").getCause());

        // get timer
        Class c = Class.forName("io.seata.tm.api.DefaultFailureHandlerImpl");
        Field field = c.getDeclaredField("timer");
        field.setAccessible(true);
        HashedWheelTimer timer = (HashedWheelTimer) field.get(failureHandler);
        // assert timer pendingCount: first time is 1
        Long pendingTimeout = timer.pendingTimeouts();
        Assertions.assertEquals(pendingTimeout,1L);
        //set globalStatus
        globalStatus= GlobalStatus.Committed;
        Thread.sleep(25*1000L);
        pendingTimeout = timer.pendingTimeouts();
        LOGGER.info("pendingTimeout {}" ,pendingTimeout);
        //all timer is done
        Assertions.assertEquals(pendingTimeout,0L);
    }

    @Test
    void onRollbackFailure() throws Exception {


        RootContext.bind(DEFAULT_XID);
        GlobalTransaction tx = GlobalTransactionContext.getCurrentOrCreate();
        FailureHandler failureHandler = new DefaultFailureHandlerImpl();
        failureHandler.onRollbackFailure(tx, new MyRuntimeException("").getCause());

        // get timer
        Class c = Class.forName("io.seata.tm.api.DefaultFailureHandlerImpl");
        Field field = c.getDeclaredField("timer");
        field.setAccessible(true);
        HashedWheelTimer timer = (HashedWheelTimer) field.get(failureHandler);
        // assert timer pendingCount: first time is 1
        Long pendingTimeout = timer.pendingTimeouts();
        Assertions.assertEquals(pendingTimeout,1L);
        //set globalStatus
        globalStatus= GlobalStatus.Rollbacked;
        Thread.sleep(25*1000L);
        pendingTimeout = timer.pendingTimeouts();
        LOGGER.info("pendingTimeout {}" ,pendingTimeout);
        //all timer is done
        Assertions.assertEquals(pendingTimeout,0L);


    }


}
