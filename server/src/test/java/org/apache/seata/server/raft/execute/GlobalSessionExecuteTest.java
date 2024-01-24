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
package org.apache.seata.server.raft.execute;

import org.apache.seata.common.util.NetUtil;
import org.apache.seata.config.ConfigurationCache;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.server.cluster.raft.execute.global.AddGlobalSessionExecute;
import org.apache.seata.server.cluster.raft.execute.global.RemoveGlobalSessionExecute;
import org.apache.seata.server.cluster.raft.execute.global.UpdateGlobalSessionExecute;
import org.apache.seata.server.cluster.raft.sync.msg.RaftGlobalSessionSyncMsg;
import org.apache.seata.server.cluster.raft.sync.msg.dto.GlobalTransactionDTO;
import org.apache.seata.server.lock.LockerManagerFactory;
import org.apache.seata.server.session.GlobalSession;
import org.apache.seata.server.session.SessionHolder;
import org.apache.seata.server.session.SessionManager;
import org.apache.seata.server.storage.SessionConverter;
import org.apache.seata.server.store.StoreConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;


/**
 */
@SpringBootTest
class GlobalSessionExecuteTest {
    @BeforeAll
    public static void setUp(ApplicationContext context){
        System.setProperty("server.raft.serverAddr", NetUtil.getLocalIp() + ":9091");
        SessionHolder.init(StoreConfig.SessionMode.RAFT);
        LockerManagerFactory.destroy();
        LockerManagerFactory.init(StoreConfig.LockMode.RAFT);
    }

    @AfterAll
    public static void destroy(){
        // Clear configuration
        ConfigurationCache.clear();
        System.clearProperty("server.raft.serverAddr");

        // Destroy SessionHolder and LockerManagerFactory
        SessionHolder.destroy();
        SessionHolder.init(null);
        LockerManagerFactory.destroy();
    }

    @AfterEach
    public void removeTestSession() throws TransactionException {
        SessionManager sm = SessionHolder.getRootSessionManager();
        GlobalSession globalSession = mockGlobalSession();
        sm.removeGlobalSession(globalSession);
    }

    @Test
    public void testAdd() throws Throwable {
        GlobalSession expected = mockGlobalSession();

        AddGlobalSessionExecute execute = new AddGlobalSessionExecute();
        boolean success = execute.execute(convertToGlobalSessionMsg(expected));
        Assertions.assertTrue(success);

        SessionManager sm = SessionHolder.getRootSessionManager();
        GlobalSession globalSession = sm.findGlobalSession("123:123");
        assertGlobalSessionValid(expected, globalSession);
    }

    @Test
    public void testRemove() throws Throwable {
        GlobalSession expected = mockGlobalSession();
        SessionHolder.getRootSessionManager().addGlobalSession(expected);

        SessionManager sm = SessionHolder.getRootSessionManager();
        GlobalSession globalSession = sm.findGlobalSession("123:123");
        Assertions.assertNotNull(globalSession);

        RemoveGlobalSessionExecute execute = new RemoveGlobalSessionExecute();
        boolean success = execute.execute(convertToGlobalSessionMsg(expected));
        Assertions.assertTrue(success);

        // Since RemoveGlobalSessionExecute executes asynchronously, wait until remove done
        Thread.sleep(1000);
        globalSession = sm.findGlobalSession("123:123");
        Assertions.assertNull(globalSession);
    }

    @Test
    public void testUpdate() throws Throwable {
        SessionHolder.getRootSessionManager().addGlobalSession(mockGlobalSession());

        SessionManager sm = SessionHolder.getRootSessionManager();
        GlobalSession globalSession = sm.findGlobalSession("123:123");
        Assertions.assertNotNull(globalSession);

        GlobalSession expected = mockGlobalSession();
        expected.setStatus(GlobalStatus.Committed);
        UpdateGlobalSessionExecute execute = new UpdateGlobalSessionExecute();
        boolean success = execute.execute(convertToGlobalSessionMsg(expected));
        Assertions.assertTrue(success);

        globalSession = sm.findGlobalSession("123:123");
        assertGlobalSessionValid(expected, globalSession);
    }

    private static GlobalSession mockGlobalSession() {
        GlobalSession session = new GlobalSession("test", "test", "test", 5000);
        session.setXid("123:123");
        session.setApplicationData("hello, world");
        session.setTransactionId(123);
        session.setBeginTime(System.currentTimeMillis());
        return session;
    }

    private static void assertGlobalSessionValid(GlobalSession expected, GlobalSession globalSession) {
        Assertions.assertNotNull(globalSession);
        Assertions.assertEquals(expected.getTransactionId(), globalSession.getTransactionId());
        Assertions.assertEquals(expected.getTimeout(), globalSession.getTimeout());
        Assertions.assertEquals(expected.getApplicationId(), globalSession.getApplicationId());
        Assertions.assertEquals(expected.getTransactionServiceGroup(), globalSession.getTransactionServiceGroup());
        Assertions.assertEquals(expected.getTransactionName(), globalSession.getTransactionName());
        Assertions.assertTrue(expected.isActive());
    }

    private static RaftGlobalSessionSyncMsg convertToGlobalSessionMsg(GlobalSession globalSession) {
        RaftGlobalSessionSyncMsg sessionMsg = new RaftGlobalSessionSyncMsg();
        GlobalTransactionDTO dto = new GlobalTransactionDTO();
        SessionConverter.convertGlobalTransactionDO(dto, globalSession);
        sessionMsg.setGlobalSession(dto);
        return sessionMsg;
    }

}
