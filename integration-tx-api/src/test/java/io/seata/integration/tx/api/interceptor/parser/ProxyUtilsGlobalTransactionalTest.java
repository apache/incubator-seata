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
package io.seata.integration.tx.api.interceptor.parser;

import io.seata.integration.tx.api.util.ProxyUtil;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.GlobalStatus;
import io.seata.core.model.TransactionManager;
import io.seata.tm.TransactionManagerHolder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

/**
 * @author leezongjie
 */
public class ProxyUtilsGlobalTransactionalTest {

    private final String DEFAULT_XID = "default_xid";


    @Test
    public void testTcc() {
        //given
        BusinessImpl business = new BusinessImpl();

        AtomicReference<String> branchReference = new AtomicReference<String>();

        Business businessProxy = ProxyUtil.createProxy(business);

        TransactionManager mockTransactionManager = new TransactionManager() {
            @Override
            public String begin(String applicationId, String transactionServiceGroup, String name, int timeout) throws TransactionException {
                return DEFAULT_XID;
            }

            @Override
            public GlobalStatus commit(String xid) throws TransactionException {
                return GlobalStatus.Committed;
            }

            @Override
            public GlobalStatus rollback(String xid) throws TransactionException {
                return null;
            }

            @Override
            public GlobalStatus getStatus(String xid) throws TransactionException {
                return null;
            }

            @Override
            public GlobalStatus globalReport(String xid, GlobalStatus globalStatus) throws TransactionException {
                return null;
            }
        };

        TransactionManagerHolder.set(mockTransactionManager);

        //when
        String result = businessProxy.doBiz("test");

        //then
        Assertions.assertNotNull(result);

    }


}
