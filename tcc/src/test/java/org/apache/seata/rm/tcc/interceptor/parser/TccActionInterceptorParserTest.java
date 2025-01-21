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
package org.apache.seata.rm.tcc.interceptor.parser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.seata.core.context.RootContext;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.core.model.ResourceManager;
import org.apache.seata.core.model.TransactionManager;
import org.apache.seata.integration.tx.api.interceptor.handler.ProxyInvocationHandler;
import org.apache.seata.integration.tx.api.interceptor.parser.DefaultInterfaceParser;
import org.apache.seata.integration.tx.api.util.ProxyUtil;
import org.apache.seata.rm.DefaultResourceManager;
import org.apache.seata.rm.tcc.BranchSessionMock;
import org.apache.seata.rm.tcc.NestTccAction;
import org.apache.seata.rm.tcc.NestTccActionImpl;
import org.apache.seata.rm.tcc.NormalTccActionImpl;
import org.apache.seata.rm.tcc.TCCResourceManager;
import org.apache.seata.rm.tcc.TccAction;
import org.apache.seata.rm.tcc.TccActionImpl;
import org.apache.seata.tm.TransactionManagerHolder;
import org.apache.seata.tm.api.GlobalTransaction;
import org.apache.seata.tm.api.GlobalTransactionContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;


class TccActionInterceptorParserTest {

    @BeforeAll
    public static void init() throws IOException {
        System.setProperty("config.type", "file");
        System.setProperty("config.file.name", "file.conf");
        System.setProperty("txServiceGroup", "default_tx_group");
        System.setProperty("service.vgroupMapping.default_tx_group", "default");
    }

    @AfterEach
    public void clearTccResource(){
        resourceManager.getManagedResources().clear();
    }

    @Test
    void parserInterfaceToProxy() throws Exception {

        //given
        TccActionInterceptorParser tccActionInterceptorParser = new TccActionInterceptorParser();
        NormalTccActionImpl tccAction = new NormalTccActionImpl();

        //when
        ProxyInvocationHandler proxyInvocationHandler = tccActionInterceptorParser.parserInterfaceToProxy(tccAction, tccAction.getClass().getName());

        //then
        Assertions.assertNotNull(proxyInvocationHandler);

    }


    @Test
    public void testNestTcc_should_commit() throws Exception {
        //given
        RootContext.unbind();
        DefaultResourceManager.get();
        DefaultResourceManager.mockResourceManager(BranchType.TCC, resourceManager);

        TransactionManagerHolder.set(transactionManager);

        TccActionImpl tccAction = new TccActionImpl();
        TccAction tccActionProxy =  ProxyUtil.createProxy(tccAction);
        Assertions.assertNotNull(tccActionProxy);

        NestTccActionImpl nestTccAction = new NestTccActionImpl();
        nestTccAction.setTccAction(tccActionProxy);

        //when
        ProxyInvocationHandler proxyInvocationHandler = DefaultInterfaceParser.get().parserInterfaceToProxy(nestTccAction, nestTccAction.getClass().getName());

        //then
        Assertions.assertNotNull(proxyInvocationHandler);


        //when
        NestTccAction nestTccActionProxy = ProxyUtil.createProxy(nestTccAction);
        //then
        Assertions.assertNotNull(nestTccActionProxy);


        // transaction commit test
        GlobalTransaction tx = GlobalTransactionContext.getCurrentOrCreate();

        try {
            tx.begin(60000, "testBiz");

            boolean result = nestTccActionProxy.prepare(null, 2);

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

        Assertions.assertTrue(nestTccAction.isCommit());
        Assertions.assertTrue(tccAction.isCommit());

    }


    @Test
    public void testNestTcc_should_rollback() throws Exception {
        //given
        RootContext.unbind();
        DefaultResourceManager.get();
        DefaultResourceManager.mockResourceManager(BranchType.TCC, resourceManager);

        TransactionManagerHolder.set(transactionManager);

        TccActionImpl tccAction = new TccActionImpl();
        TccAction tccActionProxy =  ProxyUtil.createProxy(tccAction);
        Assertions.assertNotNull(tccActionProxy);

        NestTccActionImpl nestTccAction = new NestTccActionImpl();
        nestTccAction.setTccAction(tccActionProxy);

        //when
        ProxyInvocationHandler proxyInvocationHandler = DefaultInterfaceParser.get().parserInterfaceToProxy(nestTccAction, nestTccAction.getClass().getName());

        //then
        Assertions.assertNotNull(proxyInvocationHandler);


        //when
        NestTccAction nestTccActionProxy = ProxyUtil.createProxy(nestTccAction);
        //then
        Assertions.assertNotNull(nestTccActionProxy);


        // transaction commit test
        GlobalTransaction tx = GlobalTransactionContext.getCurrentOrCreate();

        try {
            tx.begin(60000, "testBiz");

            boolean result = nestTccActionProxy.prepare(null, 1);

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

        Assertions.assertFalse(nestTccAction.isCommit());
        Assertions.assertFalse(tccAction.isCommit());

    }


    @Test
    public void testNestTcc_required_new_should_rollback_commit() throws Exception {
        //given
        RootContext.unbind();
        DefaultResourceManager.get();
        DefaultResourceManager.mockResourceManager(BranchType.TCC, resourceManager);

        TransactionManagerHolder.set(transactionManager);

        TccActionImpl tccAction = new TccActionImpl();
        TccAction tccActionProxy =  ProxyUtil.createProxy(tccAction);
        Assertions.assertNotNull(tccActionProxy);

        NestTccActionImpl nestTccAction = new NestTccActionImpl();
        nestTccAction.setTccAction(tccActionProxy);

        //when
        ProxyInvocationHandler proxyInvocationHandler = DefaultInterfaceParser.get().parserInterfaceToProxy(nestTccAction, nestTccAction.getClass().getName());

        //then
        Assertions.assertNotNull(proxyInvocationHandler);

        //when
        NestTccAction nestTccActionProxy = ProxyUtil.createProxy(nestTccAction);
        //then
        Assertions.assertNotNull(nestTccActionProxy);


        // transaction commit test
        GlobalTransaction tx = GlobalTransactionContext.getCurrentOrCreate();

        try {
            tx.begin(60000, "testBiz");

            boolean result = nestTccActionProxy.prepareNestRequiredNew(null, 1);

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

        Assertions.assertTrue(nestTccAction.isCommit());
        Assertions.assertTrue(tccAction.isCommit());

    }



    @Test
    public void testNestTcc_required_new_should_both_commit() throws Exception {
        //given
        RootContext.unbind();
        DefaultResourceManager.get();
        DefaultResourceManager.mockResourceManager(BranchType.TCC, resourceManager);

        TransactionManagerHolder.set(transactionManager);

        TccActionImpl tccAction = new TccActionImpl();
        TccAction tccActionProxy =  ProxyUtil.createProxy(tccAction);
        Assertions.assertNotNull(tccActionProxy);

        NestTccActionImpl nestTccAction = new NestTccActionImpl();
        nestTccAction.setTccAction(tccActionProxy);

        //when
        ProxyInvocationHandler proxyInvocationHandler = DefaultInterfaceParser.get().parserInterfaceToProxy(nestTccAction, nestTccAction.getClass().getName());

        //then
        Assertions.assertNotNull(proxyInvocationHandler);

        //when
        NestTccAction nestTccActionProxy = ProxyUtil.createProxy(nestTccAction);
        //then
        Assertions.assertNotNull(nestTccActionProxy);


        // transaction commit test
        GlobalTransaction tx = GlobalTransactionContext.getCurrentOrCreate();

        try {
            tx.begin(60000, "testBiz");

            boolean result = nestTccActionProxy.prepareNestRequiredNew(null, 2);

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

        Assertions.assertTrue(nestTccAction.isCommit());
        Assertions.assertTrue(tccAction.isCommit());

    }



    private static Map<String, List<BranchSessionMock>> applicationDataMap = new ConcurrentHashMap<>();


    private static TransactionManager transactionManager = new TransactionManager() {
        @Override
        public String begin(String applicationId, String transactionServiceGroup, String name, int timeout) throws TransactionException {
            return UUID.randomUUID().toString();
        }

        @Override
        public GlobalStatus commit(String xid) throws TransactionException {
            commitAll(xid);
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


    private static ResourceManager resourceManager = new TCCResourceManager() {

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

    public static void commitAll(String xid) throws TransactionException {

        List<BranchSessionMock> branches = applicationDataMap.computeIfAbsent(xid, s -> new ArrayList<>());
        for (BranchSessionMock branch : branches) {
            resourceManager.branchCommit(branch.getBranchType(), branch.getXid(), branch.getBranchId(), branch.getResourceId(), branch.getApplicationData());
        }
    }

    public static void rollbackAll(String xid) throws TransactionException {

        List<BranchSessionMock> branches = applicationDataMap.computeIfAbsent(xid, s -> new ArrayList<>());
        for (BranchSessionMock branch : branches) {
            resourceManager.branchRollback(branch.getBranchType(), branch.getXid(), branch.getBranchId(), branch.getResourceId(), branch.getApplicationData());
        }
    }

}
