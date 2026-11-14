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
package org.apache.seata.server.storage.redis.session;

import org.apache.seata.common.exception.StoreException;
import org.apache.seata.core.model.*;
import org.apache.seata.server.BaseSpringBootTest;
import org.apache.seata.server.session.*;
import org.apache.seata.server.storage.redis.store.*;
import org.apache.seata.server.store.TransactionStoreManager.LogOperation;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RedisSessionManagerUnitTest extends BaseSpringBootTest {
    @Test
    void writesPropagateOperationAndRejectFailedPersistence() throws Exception {
        RedisTransactionStoreManager store = mock(RedisTransactionStoreManager.class);
        try (MockedStatic<RedisTransactionStoreManagerFactory> factory =
                mockStatic(RedisTransactionStoreManagerFactory.class)) {
            factory.when(RedisTransactionStoreManagerFactory::getInstance).thenReturn(store);
            RedisSessionManager manager = new RedisSessionManager();
            manager.init();
            GlobalSession global = new GlobalSession();
            BranchSession branch = new BranchSession();
            when(store.writeSession(any(), any())).thenReturn(true);
            manager.addGlobalSession(global);
            manager.updateGlobalSessionStatus(global, GlobalStatus.Committing);
            manager.removeGlobalSession(global);
            manager.addBranchSession(global, branch);
            manager.updateBranchSessionStatus(branch, BranchStatus.Registered);
            manager.removeBranchSession(global, branch);
            assertEquals(GlobalStatus.Committing, global.getStatus());
            for (LogOperation op :
                    new LogOperation[] {LogOperation.GLOBAL_ADD, LogOperation.GLOBAL_UPDATE, LogOperation.GLOBAL_REMOVE
                    }) {
                verify(store).writeSession(op, global);
            }
            for (LogOperation op :
                    new LogOperation[] {LogOperation.BRANCH_ADD, LogOperation.BRANCH_UPDATE, LogOperation.BRANCH_REMOVE
                    }) {
                verify(store).writeSession(op, branch);
            }
            when(store.writeSession(any(), any())).thenReturn(false);
            assertThrows(StoreException.class, () -> manager.addGlobalSession(global));
            assertThrows(
                    StoreException.class, () -> manager.updateGlobalSessionStatus(global, GlobalStatus.Committing));
            assertThrows(StoreException.class, () -> manager.removeGlobalSession(global));
            assertThrows(StoreException.class, () -> manager.addBranchSession(global, branch));
            assertThrows(
                    StoreException.class, () -> manager.updateBranchSessionStatus(branch, BranchStatus.Registered));
            assertThrows(StoreException.class, () -> manager.removeBranchSession(global, branch));
            when(store.readSession("xid", true)).thenReturn(global);
            assertSame(global, manager.findGlobalSession("xid"));
            List<GlobalSession> sessions = Collections.singletonList(global);
            when(store.readSession(any(SessionCondition.class))).thenReturn(sessions);
            assertSame(sessions, manager.allSessions());
            assertSame(sessions, manager.findGlobalSessions(new SessionCondition()));
            assertEquals("executed", manager.lockAndExecute(global, () -> "executed"));
        }
    }
}
