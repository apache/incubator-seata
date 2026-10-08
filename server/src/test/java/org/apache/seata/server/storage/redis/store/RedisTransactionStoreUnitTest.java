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
package org.apache.seata.server.storage.redis.store;

import org.apache.seata.common.exception.RedisException;
import org.apache.seata.common.util.BeanUtils;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.core.store.BranchTransactionDO;
import org.apache.seata.core.store.GlobalTransactionDO;
import org.apache.seata.server.BaseSpringBootTest;
import org.apache.seata.server.console.entity.param.GlobalSessionParam;
import org.apache.seata.server.session.GlobalSession;
import org.apache.seata.server.session.SessionCondition;
import org.apache.seata.server.storage.redis.JedisPooledFactory;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import redis.clients.jedis.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RedisTransactionStoreUnitTest extends BaseSpringBootTest {
    private Jedis jedis;
    private Pipeline pipeline;
    private Transaction transaction;
    private MockedStatic<JedisPooledFactory> factory;
    private RedisTransactionStoreManager store;

    @BeforeEach
    void open() {
        jedis = mock(Jedis.class);
        pipeline = mock(Pipeline.class);
        transaction = mock(Transaction.class);
        factory = mockStatic(JedisPooledFactory.class);
        factory.when(JedisPooledFactory::getJedisInstance).thenReturn(jedis);
        when(jedis.pipelined()).thenReturn(pipeline);
        when(jedis.multi()).thenReturn(transaction);
        store = new RedisTransactionStoreManager();
    }

    @AfterEach
    void close() {
        factory.close();
    }

    private GlobalTransactionDO global() {
        GlobalTransactionDO g = new GlobalTransactionDO();
        g.setXid("127.0.0.1:8091:42");
        g.setTransactionId(42L);
        g.setStatus(GlobalStatus.Begin.getCode());
        g.setBeginTime(100L);
        g.setTimeout(2000);
        g.setApplicationId("app");
        g.setTransactionServiceGroup("group");
        g.setTransactionName("order");
        return g;
    }

    private BranchTransactionDO branch() {
        BranchTransactionDO b = new BranchTransactionDO();
        b.setXid(global().getXid());
        b.setTransactionId(42L);
        b.setBranchId(7L);
        b.setBranchType("AT");
        b.setStatus(1);
        b.setResourceId("db");
        b.setClientId("client");
        return b;
    }

    @Test
    void insertIndexesAndTimestamps() {
        GlobalTransactionDO g = global();
        BranchTransactionDO b = branch();
        assertTrue(store.insertGlobalTransactionDO(g));
        assertTrue(store.insertBranchTransactionDO(b));
        verify(pipeline).zadd("SEATA_BEGIN_TRANSACTIONS", 2100D, "SEATA_GLOBAL_42");
        verify(pipeline).rpush("SEATA_STATUS_1", g.getXid());
        verify(pipeline).rpush("SEATA_BRANCHES_" + g.getXid(), "SEATA_BRANCH_7");
        verify(pipeline).hmset(eq("SEATA_BRANCH_7"), argThat(m -> "7".equals(m.get("branchId"))));
        assertEquals(g.getGmtCreate(), g.getGmtModified());
        assertNotNull(b.getGmtCreate());
        verify(pipeline, times(2)).sync();
        verify(jedis, times(2)).close();
    }

    @Test
    void deleteIsIdempotentAndRemovesIndexes() {
        assertTrue(store.deleteGlobalTransactionDO(global()));
        assertTrue(store.deleteBranchTransactionDO(branch()));
        verify(jedis, never()).pipelined();
        when(jedis.hget(anyString(), eq("xid"))).thenReturn(global().getXid());
        assertTrue(store.deleteGlobalTransactionDO(global()));
        assertTrue(store.deleteBranchTransactionDO(branch()));
        verify(pipeline).zrem("SEATA_BEGIN_TRANSACTIONS", "SEATA_GLOBAL_42");
        verify(pipeline).del("SEATA_GLOBAL_42");
        verify(pipeline).del("SEATA_BRANCH_7");
        verify(pipeline).lrem("SEATA_BRANCHES_" + global().getXid(), 0, "SEATA_BRANCH_7");
    }

    @Test
    void branchUpdateRejectsMissingAndPreservesBlankApplicationData() {
        BranchTransactionDO b = branch();
        assertThrows(RedisException.class, () -> store.updateBranchTransactionDO(b));
        when(jedis.hget("SEATA_BRANCH_7", "status")).thenReturn("1");
        assertTrue(store.updateBranchTransactionDO(b));
        verify(jedis)
                .hmset(
                        eq("SEATA_BRANCH_7"),
                        argThat(m -> !m.containsKey("applicationData") && "1".equals(m.get("status"))));
        b.setApplicationData("payload");
        assertTrue(store.updateBranchTransactionDO(b));
        verify(jedis).hmset(eq("SEATA_BRANCH_7"), argThat(m -> "payload".equals(m.get("applicationData"))));
    }

    @Test
    void globalUpdateHandlesMissingUnchangedInvalidAndConcurrentChanges() {
        GlobalTransactionDO g = global();
        when(jedis.hmget(anyString(), any(String[].class))).thenReturn(Arrays.asList(null, null));
        assertThrows(RedisException.class, () -> store.updateGlobalTransactionDO(g));
        when(jedis.hmget(anyString(), any(String[].class))).thenReturn(Arrays.asList("1", "100"));
        assertTrue(store.updateGlobalTransactionDO(g));
        verify(jedis, times(2)).unwatch();
        verify(jedis, never()).multi();
        g.setStatus(GlobalStatus.Committing.getCode());
        when(transaction.exec()).thenReturn(Collections.emptyList());
        assertTrue(store.updateGlobalTransactionDO(g));
        when(transaction.exec()).thenReturn(Arrays.asList("OK", 1L, 1L, 1L));
        assertTrue(store.updateGlobalTransactionDO(g));
        verify(transaction, times(2)).zrem("SEATA_BEGIN_TRANSACTIONS", "SEATA_GLOBAL_42");
        when(jedis.hmget(anyString(), any(String[].class)))
                .thenReturn(Arrays.asList(String.valueOf(GlobalStatus.Rollbacking.getCode()), "100"));
        assertThrows(RedisException.class, () -> store.updateGlobalTransactionDO(g));
    }

    @Test
    void failedStatusIndexUpdateCompensatesSuccessfulOperations() {
        GlobalTransactionDO g = global();
        g.setStatus(GlobalStatus.Committing.getCode());
        when(jedis.hmget(anyString(), any(String[].class))).thenReturn(Arrays.asList("1", "100"));
        when(jedis.hget("SEATA_GLOBAL_42", "xid")).thenReturn(g.getXid());
        when(transaction.exec()).thenReturn(Arrays.asList("OK", 0L, 1L));
        assertFalse(store.updateGlobalTransactionDO(g));
        verify(transaction).hmset(eq("SEATA_GLOBAL_42"), eq(new HashMap<String, String>() {
            {
                put("status", "1");
                put("gmtModified", "100");
            }
        }));
        verify(jedis).lrem("SEATA_STATUS_2", 0, g.getXid());
        when(transaction.exec()).thenReturn(Arrays.asList("ERR", 1L, 0L));
        assertFalse(store.updateGlobalTransactionDO(g));
        verify(jedis).rpush("SEATA_STATUS_1", g.getXid());
    }

    @Test
    void connectionFailuresAreWrappedForEveryWrite() {
        factory.when(JedisPooledFactory::getJedisInstance).thenThrow(new IllegalStateException("offline"));
        assertThrows(RedisException.class, () -> store.insertGlobalTransactionDO(global()));
        assertThrows(RedisException.class, () -> store.deleteGlobalTransactionDO(global()));
        assertThrows(RedisException.class, () -> store.updateGlobalTransactionDO(global()));
        assertThrows(RedisException.class, () -> store.insertBranchTransactionDO(branch()));
        assertThrows(RedisException.class, () -> store.deleteBranchTransactionDO(branch()));
        assertThrows(RedisException.class, () -> store.updateBranchTransactionDO(branch()));
    }

    @Test
    void readsReconstructSessionsAndHandleMissingRecords() {
        String xid = global().getXid();
        assertNull(store.readSession(xid));
        assertTrue(store.readSession(new SessionCondition(xid)).isEmpty());
        SessionCondition condition = new SessionCondition();
        condition.setTransactionId(42L);
        assertTrue(store.readSession(condition).isEmpty());
        assertNull(store.readSession(new SessionCondition()));
        when(jedis.hgetAll("SEATA_GLOBAL_42")).thenReturn(BeanUtils.objectToMap(global()));
        when(jedis.lrange("SEATA_BRANCHES_" + xid, 0, 20)).thenReturn(Collections.singletonList("SEATA_BRANCH_7"));
        when(pipeline.syncAndReturnAll()).thenReturn(Arrays.asList(null, BeanUtils.objectToMap(branch())));
        GlobalSession session = store.readSession(xid);
        assertEquals(xid, session.getXid());
        assertEquals(1, session.getBranchSessions().size());
        assertEquals(7L, session.getBranchSessions().get(0).getBranchId());
        assertEquals(1, store.findBranchSessionByXid(xid).size());
        assertEquals(1, store.readSession(condition).size());
        assertEquals(1, store.readSession(new SessionCondition(xid)).size());
        assertTrue(store.readSession(xid, false).getBranchSessions().isEmpty());
    }

    @Test
    void statusPaginationCountsAndLimitsResults() {
        RedisTransactionStoreManager reader = spy(store);
        GlobalSession session = new GlobalSession();
        doReturn(session).when(reader).readSession(anyString(), anyBoolean());
        when(pipeline.syncAndReturnAll()).thenReturn(Arrays.asList(2L, 1L));
        assertEquals(
                3L, reader.countByGlobalSessions(new GlobalStatus[] {GlobalStatus.Begin, GlobalStatus.Committing}));
        when(jedis.lrange(anyString(), anyLong(), anyLong())).thenReturn(Collections.singletonList(global().getXid()));
        assertEquals(1, reader.findGlobalSessionByPage(1, 1, false).size());
        GlobalSessionParam param = new GlobalSessionParam();
        param.setPageNum(1);
        param.setPageSize(2);
        assertTrue(reader.readSessionStatusByPage(param).isEmpty());
        param.setStatus(1);
        assertEquals(1, reader.readSessionStatusByPage(param).size());
        reader.setLogQueryLimit(1);
        assertEquals(
                1,
                reader.readSession(new GlobalStatus[] {GlobalStatus.Begin, GlobalStatus.Committing}, false)
                        .size());
        reader.setLogQueryLimit(0);
        assertTrue(reader.readSession(new GlobalStatus[] {GlobalStatus.Begin, GlobalStatus.Committing}, false)
                .isEmpty());
        when(pipeline.syncAndReturnAll()).thenReturn(Collections.singletonList(1L));
        assertTrue(reader.readSortByTimeoutBeginSessions(false).isEmpty());
        when(pipeline.syncAndReturnAll()).thenReturn(Collections.emptyList());
        assertEquals(0L, reader.countByGlobalSessions(new GlobalStatus[0]));
    }

    @Test
    void timeoutQueryLoadsBranchesAndDispatchesConditions() {
        when(pipeline.syncAndReturnAll())
                .thenReturn(Collections.singletonList(1L), Collections.singletonList(BeanUtils.objectToMap(global())));
        when(jedis.zrangeByScore(eq("SEATA_BEGIN_TRANSACTIONS"), eq(0D), anyDouble(), eq(0), eq(1)))
                .thenReturn(Collections.singletonList("SEATA_GLOBAL_42"));
        assertEquals(
                global().getXid(),
                store.readSortByTimeoutBeginSessions(true).get(0).getXid());
        RedisTransactionStoreManager reader = spy(store);
        doReturn(Collections.emptyList()).when(reader).readSortByTimeoutBeginSessions(anyBoolean());
        doReturn(Collections.emptyList()).when(reader).readSession(any(GlobalStatus[].class), anyBoolean());
        reader.readSession(new SessionCondition(GlobalStatus.Begin));
        reader.readSession(new SessionCondition(GlobalStatus.Committing));
        verify(reader).readSortByTimeoutBeginSessions(true);
        verify(reader).readSession(new GlobalStatus[] {GlobalStatus.Committing}, true);
    }

    @Test
    void sessionWritesDispatchToGlobalAndBranchStores() {
        org.apache.seata.server.session.GlobalSession global =
                new org.apache.seata.server.session.GlobalSession("app", "group", "order", 1000);
        global.setXid("127.0.0.1:8091:42");
        global.setTransactionId(42L);
        global.setStatus(GlobalStatus.Begin);
        org.apache.seata.server.session.BranchSession branch = new org.apache.seata.server.session.BranchSession();
        branch.setXid(global.getXid());
        branch.setTransactionId(42L);
        branch.setBranchId(7L);
        branch.setBranchType(org.apache.seata.core.model.BranchType.AT);
        branch.setStatus(org.apache.seata.core.model.BranchStatus.Registered);
        assertTrue(store.writeSession(
                org.apache.seata.server.store.TransactionStoreManager.LogOperation.GLOBAL_ADD, global));
        assertTrue(store.writeSession(
                org.apache.seata.server.store.TransactionStoreManager.LogOperation.BRANCH_ADD, branch));
        verify(pipeline).hmset(eq("SEATA_GLOBAL_42"), anyMap());
        verify(pipeline).hmset(eq("SEATA_BRANCH_7"), anyMap());
    }
}
