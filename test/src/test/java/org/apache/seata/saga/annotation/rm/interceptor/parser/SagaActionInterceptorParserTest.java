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
package org.apache.seata.saga.annotation.rm.interceptor.parser;

import org.apache.seata.common.XID;
import org.apache.seata.common.util.UUIDGenerator;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.core.model.ResourceManager;
import org.apache.seata.core.model.TransactionManager;
import org.apache.seata.integration.tx.api.interceptor.handler.ProxyInvocationHandler;
import org.apache.seata.integration.tx.api.util.ProxyUtil;
import org.apache.seata.rm.DefaultResourceManager;
import org.apache.seata.saga.annotation.BranchSessionMock;
import org.apache.seata.saga.annotation.NormalSagaAnnotationActionImpl;
import org.apache.seata.saga.annotation.SagaParam;
import org.apache.seata.saga.rm.SagaAnnotationResourceManager;
import org.apache.seata.saga.rm.interceptor.parser.SagaAnnotationActionInterceptorParser;
import org.apache.seata.tm.TransactionManagerHolder;
import org.apache.seata.tm.api.GlobalTransaction;
import org.apache.seata.tm.api.GlobalTransactionContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Saga Action InterceptorParser Test
 */
public class SagaActionInterceptorParserTest {

    private ResourceManager backResourceManager;
    private TransactionManager backTransactionManager;

    @BeforeEach
    public void beforeEach() {
        backTransactionManager = TransactionManagerHolder.get();
        backResourceManager = DefaultResourceManager.get().getResourceManager(BranchType.SAGA_ANNOTATION);

        Map<String, List<BranchSessionMock>> applicationDataMap = new ConcurrentHashMap<>();
        ResourceManager mockResourceManager = new SagaAnnotationResourceManager() {

            @Override
            public Long branchRegister(BranchType branchType, String resourceId, String clientId, String xid, String applicationData, String lockKeys) throws TransactionException {

                long branchId = System.currentTimeMillis();

                List<BranchSessionMock> branches = applicationDataMap.computeIfAbsent(xid, s -> new ArrayList<>());
                BranchSessionMock branchSessionMock = new BranchSessionMock();
                branchSessionMock.setXid(xid);
                branchSessionMock.setBranchType(branchType);
                branchSessionMock.setResourceId(resourceId);
                branchSessionMock.setApplicationData(applicationData);
                branchSessionMock.setBranchId(branchId);

                branches.add(branchSessionMock);

                return branchId;
            }
        };
        DefaultResourceManager.mockResourceManager(BranchType.SAGA_ANNOTATION, mockResourceManager);

        TransactionManager mockTransactionManager = new TransactionManager() {
            @Override
            public String begin(String applicationId, String transactionServiceGroup, String name, int timeout) throws TransactionException {
                return XID.generateXID(UUIDGenerator.generateUUID());
            }

            @Override
            public GlobalStatus commit(String xid) throws TransactionException {
                return GlobalStatus.Committed;
            }

            @Override
            public GlobalStatus rollback(String xid) throws TransactionException {
                List<BranchSessionMock> branches = applicationDataMap.computeIfAbsent(xid, s -> new ArrayList<>());
                for (BranchSessionMock branch : branches) {
                    mockResourceManager.branchRollback(branch.getBranchType(), branch.getXid(), branch.getBranchId(), branch.getResourceId(), branch.getApplicationData());
                }

                return GlobalStatus.Rollbacked;
            }

            @Override
            public GlobalStatus getStatus(String xid) throws TransactionException {
                return GlobalStatus.Begin;
            }

            @Override
            public GlobalStatus globalReport(String xid, GlobalStatus globalStatus) throws TransactionException {
                return globalStatus;
            }
        };
        TransactionManagerHolder.set(mockTransactionManager);
    }

    @AfterEach
    public void afterEach() {
        DefaultResourceManager.mockResourceManager(BranchType.SAGA_ANNOTATION, backResourceManager);
        TransactionManagerHolder.set(backTransactionManager);
    }

    @Test
    void parserInterfaceToProxy() {
        NormalSagaAnnotationActionImpl sagaAction = new NormalSagaAnnotationActionImpl();

        SagaAnnotationActionInterceptorParser sagaAnnotationActionInterceptorParser = new SagaAnnotationActionInterceptorParser();

        ProxyInvocationHandler proxyInvocationHandler = sagaAnnotationActionInterceptorParser.parserInterfaceToProxy(sagaAction, "sagaAction");
        Assertions.assertNotNull(proxyInvocationHandler);
    }


    @Test
    public void testSagaAnnotation_should_commit() throws TransactionException {
        NormalSagaAnnotationActionImpl sagaActionProxy = ProxyUtil.createProxy(new NormalSagaAnnotationActionImpl());

        SagaParam sagaParam = new SagaParam(2, "abc@163.com");
        List<String> listB = Collections.singletonList("b");

        GlobalTransaction tx = GlobalTransactionContext.getCurrentOrCreate();

        try {
            tx.begin(60000, "testBiz");

            boolean result = sagaActionProxy.commit(null, 2, listB, sagaParam);

            Assertions.assertTrue(result);

            tx.commit();
        } catch (Exception exx) {
            tx.rollback();
            throw exx;
        }

        Assertions.assertTrue(sagaActionProxy.isCommit());
    }

    @Test
    @Disabled
    public void testSagaAnnotation_should_rollback() throws TransactionException {
        NormalSagaAnnotationActionImpl sagaActionProxy = ProxyUtil.createProxy(new NormalSagaAnnotationActionImpl());

        SagaParam sagaParam = new SagaParam(1, "abc@163.com");
        List<String> listB = Collections.singletonList("b");

        GlobalTransaction tx = GlobalTransactionContext.getCurrentOrCreate();

        try {
            tx.begin(60000, "testBiz");

            boolean result = sagaActionProxy.commit(null, 1, listB, sagaParam);

            Assertions.assertFalse(result);
            tx.rollback();
        } catch (Exception exx) {
            tx.rollback();
            throw exx;
        }

        Assertions.assertFalse(sagaActionProxy.isCommit());
    }


}