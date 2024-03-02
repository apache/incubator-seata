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
package org.apache.seata.saga.rm.interceptor.parser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.core.model.ResourceManager;
import org.apache.seata.core.model.TransactionManager;
import org.apache.seata.integration.tx.api.interceptor.handler.ProxyInvocationHandler;
import org.apache.seata.integration.tx.api.util.ProxyUtil;
import org.apache.seata.rm.DefaultResourceManager;
import org.apache.seata.saga.BranchSessionMock;
import org.apache.seata.saga.NormalSagaAction;
import org.apache.seata.saga.NormalSagaActionImpl;
import org.apache.seata.saga.SagaParam;
import org.apache.seata.saga.rm.SagaAnnotationResourceManager;
import org.apache.seata.tm.TransactionManagerHolder;
import org.apache.seata.tm.api.GlobalTransaction;
import org.apache.seata.tm.api.GlobalTransactionContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 *
 */
public class SagaActionInterceptorParserTest {

    public static String DEFAULT_XID = "default_xid";


    @BeforeAll
    public static void init() throws IOException {
        System.setProperty("config.type", "file");
        System.setProperty("config.file.name", "file.conf");
        System.setProperty("txServiceGroup", "default_tx_group");
        System.setProperty("service.vgroupMapping.default_tx_group", "default");
    }


    @Test
    void parserInterfaceToProxy() {
        //given
        NormalSagaActionImpl sagaAction = new NormalSagaActionImpl();

        SagaActionInterceptorParser sagaActionInterceptorParser = new SagaActionInterceptorParser();

        //when
        ProxyInvocationHandler proxyInvocationHandler = sagaActionInterceptorParser.parserInterfaceToProxy(sagaAction, "sagaAction");

        //then
        Assertions.assertNotNull(proxyInvocationHandler);

    }

    @Test
    public void testSagaAnnotation_should_commit() throws TransactionException {
        //given

        DefaultResourceManager.get();
        DefaultResourceManager.mockResourceManager(BranchType.SAGA_ANNOTATION, resourceManager);

        TransactionManagerHolder.set(transactionManager);

        NormalSagaActionImpl sagaAction = new NormalSagaActionImpl();
        NormalSagaAction tccActionProxy = ProxyUtil.createProxy(sagaAction);

        SagaParam sagaParam = new SagaParam(2, "abc@163.com");
        List<String> listB = Arrays.asList("b");


        GlobalTransaction tx = GlobalTransactionContext.getCurrentOrCreate();

        try {
            tx.begin(60000, "testBiz");

            boolean result = tccActionProxy.commit(null, 2, listB, sagaParam);

            Assertions.assertTrue(result);

            if (result) {
                tx.commit();
            } else {
                tx.rollback();
            }
        } catch (Exception exx) {
            tx.rollback();
            throw exx;
        }

        Assertions.assertTrue(sagaAction.isCommit());

    }

    @Test
    public void testSagaAnnotation_should_rollback() throws TransactionException {
        //given

        DefaultResourceManager.get();
        DefaultResourceManager.mockResourceManager(BranchType.SAGA_ANNOTATION, resourceManager);

        TransactionManagerHolder.set(transactionManager);

        NormalSagaActionImpl sagaAction = new NormalSagaActionImpl();
        NormalSagaAction tccActionProxy = ProxyUtil.createProxy(sagaAction);

        SagaParam sagaParam = new SagaParam(1, "abc@163.com");
        List<String> listB = Arrays.asList("b");


        GlobalTransaction tx = GlobalTransactionContext.getCurrentOrCreate();

        try {
            tx.begin(60000, "testBiz");

            boolean result = tccActionProxy.commit(null, 1, listB, sagaParam);

            Assertions.assertFalse(result);

            if (result) {
                tx.commit();
            } else {
                tx.rollback();
            }
        } catch (Exception exx) {
            tx.rollback();
            throw exx;
        }

        Assertions.assertFalse(sagaAction.isCommit());
    }

    private static Map<String, List<BranchSessionMock>> applicationDataMap = new ConcurrentHashMap<>();


    private static TransactionManager transactionManager = new TransactionManager() {
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

            rollbackAll(xid);

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


    private static ResourceManager resourceManager = new SagaAnnotationResourceManager() {

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


    public static void rollbackAll(String xid) throws TransactionException {

        List<BranchSessionMock> branches = applicationDataMap.computeIfAbsent(xid, s -> new ArrayList<>());
        for (BranchSessionMock branch : branches) {
            resourceManager.branchRollback(branch.getBranchType(), branch.getXid(), branch.getBranchId(), branch.getResourceId(), branch.getApplicationData());
        }
    }

}