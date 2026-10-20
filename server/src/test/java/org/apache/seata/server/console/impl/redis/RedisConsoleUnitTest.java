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
package org.apache.seata.server.console.impl.redis;

import org.apache.seata.common.result.PageResult;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.core.store.BranchTransactionDO;
import org.apache.seata.server.BaseSpringBootTest;
import org.apache.seata.server.console.entity.param.*;
import org.apache.seata.server.console.entity.vo.*;
import org.apache.seata.server.console.exception.ConsoleException;
import org.apache.seata.server.lock.*;
import org.apache.seata.server.session.*;
import org.apache.seata.server.storage.redis.JedisPooledFactory;
import org.apache.seata.server.storage.redis.store.*;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import redis.clients.jedis.Jedis;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RedisConsoleUnitTest extends BaseSpringBootTest {
    @Test
    void globalQueriesDispatchPaginationXidAndStatus() {
        RedisTransactionStoreManager store = mock(RedisTransactionStoreManager.class);
        try (MockedStatic<RedisTransactionStoreManagerFactory> factory =
                mockStatic(RedisTransactionStoreManagerFactory.class)) {
            factory.when(RedisTransactionStoreManagerFactory::getInstance).thenReturn(store);
            GlobalSessionRedisServiceImpl service = new GlobalSessionRedisServiceImpl();
            GlobalSessionParam p = new GlobalSessionParam();
            p.setPageNum(1);
            p.setPageSize(10);
            GlobalSession session = GlobalSession.createGlobalSession("app", "group", "order", 1000);
            when(store.countByGlobalSessions(any())).thenReturn(1L);
            when(store.findGlobalSessionByPage(1, 10, false)).thenReturn(Collections.singletonList(session));
            assertEquals(1, service.query(p).getTotal());
            p.setXid(session.getXid());
            when(store.readSession(any(SessionCondition.class))).thenReturn(Collections.singletonList(session));
            PageResult<GlobalSessionVO> result = service.query(p);
            assertEquals(session.getXid(), result.getData().get(0).getXid());
            verify(store)
                    .readSession(argThat(
                            (SessionCondition c) -> c.getXid().equals(session.getXid()) && c.isLazyLoadBranch()));
            p.setStatus(GlobalStatus.Begin.getCode());
            assertEquals(1, service.query(p).getTotal());
            p.setXid(null);
            when(store.readSessionStatusByPage(p)).thenReturn(Collections.singletonList(session));
            assertEquals(1, service.query(p).getData().size());
            p.setTimeStart(1L);
            assertFalse(service.query(p).isSuccess());
        }
    }

    @Test
    void branchQueryCopiesRecordsAndHandlesEmptyXid() {
        RedisTransactionStoreManager store = mock(RedisTransactionStoreManager.class);
        try (MockedStatic<RedisTransactionStoreManagerFactory> factory =
                mockStatic(RedisTransactionStoreManagerFactory.class)) {
            factory.when(RedisTransactionStoreManagerFactory::getInstance).thenReturn(store);
            BranchSessionRedisServiceImpl service = new BranchSessionRedisServiceImpl();
            assertTrue(service.queryByXid(" ").getData().isEmpty());
            factory.verifyNoInteractions();
            assertTrue(service.queryByXid("missing").getData().isEmpty());
            BranchTransactionDO branch = new BranchTransactionDO();
            branch.setXid("xid");
            branch.setBranchId(7L);
            branch.setResourceId("orders");
            when(store.findBranchSessionByXid("xid")).thenReturn(Collections.singletonList(branch));
            BranchSessionVO result = service.queryByXid("xid").getData().get(0);
            assertEquals("7", result.getBranchId());
            assertEquals("orders", result.getResourceId());
        }
    }

    @Test
    void lockQueriesResolveIndexesAndRejectIncompleteParameters() throws Exception {
        Jedis jedis = mock(Jedis.class);
        LockManager locks = mock(LockManager.class);
        try (MockedStatic<JedisPooledFactory> factory = mockStatic(JedisPooledFactory.class);
                MockedStatic<LockerManagerFactory> managers = mockStatic(LockerManagerFactory.class)) {
            factory.when(JedisPooledFactory::getJedisInstance).thenReturn(jedis);
            managers.when(LockerManagerFactory::getLockManager).thenReturn(locks);
            GlobalLockRedisServiceImpl service = new GlobalLockRedisServiceImpl();
            GlobalLockParam p = new GlobalLockParam();
            p.setPageNum(1);
            p.setPageSize(10);
            assertFalse(service.query(p).isSuccess());
            p.setXid("xid");
            assertTrue(service.query(p).getData().isEmpty());
            Map<String, String> row = new HashMap<>();
            row.put("xid", "xid");
            row.put("branchId", "7");
            row.put("pk", "1");
            when(jedis.hgetAll("SEATA_GLOBAL_LOCKxid")).thenReturn(Collections.singletonMap("7", "row1;row2"));
            when(jedis.hgetAll("row1")).thenReturn(row);
            when(jedis.hgetAll("row2")).thenReturn(row);
            assertEquals(2, service.query(p).getData().size());
            p.setXid(null);
            p.setTableName("orders");
            p.setPk("1");
            p.setResourceId("db");
            when(jedis.hgetAll("SEATA_ROW_LOCK_db^^^orders^^^1")).thenReturn(row);
            assertEquals(1, service.query(p).getData().size());
            p.setXid("xid");
            p.setBranchId("7");
            assertTrue(service.deleteLock(p).isSuccess());
            verify(locks).releaseLock(argThat(b -> b.getBranchId() == 7L && "xid".equals(b.getXid())));
            doThrow(new TransactionException("failed")).when(locks).releaseLock(any(BranchSession.class));
            assertThrows(ConsoleException.class, () -> service.deleteLock(p));
        }
    }
}
