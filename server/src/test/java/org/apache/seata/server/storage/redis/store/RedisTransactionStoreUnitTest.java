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

    @Test
    void pagedReadSessionSingleStatusTraversesAllPagesInOrder() {
        List<String> allXids = Arrays.asList("xid-1", "xid-2", "xid-3", "xid-4", "xid-5", "xid-6", "xid-7");
        String statusKey = "SEATA_STATUS_" + GlobalStatus.Committing.getCode();
        when(jedis.lrange(eq(statusKey), anyLong(), anyLong())).thenAnswer(invocation -> {
            long start = invocation.getArgument(1);
            long end = invocation.getArgument(2);
            if (start >= allXids.size()) {
                return Collections.emptyList();
            }
            int from = (int) Math.max(start, 0);
            int to = (int) Math.min(end + 1, allXids.size());
            return new ArrayList<>(allXids.subList(from, to));
        });

        RedisTransactionStoreManager reader = spy(store);
        doAnswer(invocation -> {
                    String xid = invocation.getArgument(0);
                    boolean withBranches = invocation.getArgument(1);
                    GlobalSession s = new GlobalSession("app", "group", "order", 1000, !withBranches);
                    s.setXid(xid);
                    return s;
                })
                .when(reader)
                .readSession(anyString(), anyBoolean());

        List<String> visited = new ArrayList<>();
        int offset = 0;
        int pageSize = 3;
        while (true) {
            SessionCondition condition = new SessionCondition(GlobalStatus.Committing);
            condition.setOffset(offset);
            condition.setPageSize(pageSize);
            condition.setLazyLoadBranch(true);
            List<GlobalSession> page = reader.readSession(condition);
            if (page.isEmpty()) {
                break;
            }
            for (GlobalSession s : page) {
                assertTrue(s.isLazyLoadBranch());
                visited.add(s.getXid());
            }
            offset += pageSize;
        }

        assertEquals(allXids, visited);
        verify(jedis).lrange(statusKey, 0L, 2L);
        verify(jedis).lrange(statusKey, 3L, 5L);
        verify(jedis).lrange(statusKey, 6L, 8L);
        verify(jedis).lrange(statusKey, 9L, 11L);
        verify(jedis, never()).pipelined();
    }

    @Test
    void pagedReadSessionMultipleStatusesSpansListBoundariesDeterministically() {
        List<String> listA = Arrays.asList("a0", "a1", "a2", "a3", "a4");
        List<String> listB = Arrays.asList("b0", "b1", "b2");
        List<String> listC = Arrays.asList("c0", "c1", "c2", "c3");
        String keyA = "SEATA_STATUS_" + GlobalStatus.Committing.getCode();
        String keyB = "SEATA_STATUS_" + GlobalStatus.CommitRetrying.getCode();
        String keyC = "SEATA_STATUS_" + GlobalStatus.AsyncCommitting.getCode();

        Map<String, List<String>> backing = new HashMap<>();
        backing.put(keyA, listA);
        backing.put(keyB, listB);
        backing.put(keyC, listC);

        when(pipeline.syncAndReturnAll()).thenReturn(Arrays.asList(5L, 3L, 4L));
        when(jedis.lrange(anyString(), anyLong(), anyLong())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            long start = invocation.getArgument(1);
            long end = invocation.getArgument(2);
            List<String> data = backing.getOrDefault(key, Collections.emptyList());
            if (start >= data.size()) {
                return Collections.emptyList();
            }
            int from = (int) Math.max(start, 0);
            int to = (int) Math.min(end + 1, data.size());
            return new ArrayList<>(data.subList(from, to));
        });

        RedisTransactionStoreManager reader = spy(store);
        doAnswer(invocation -> {
                    String xid = invocation.getArgument(0);
                    boolean withBranches = invocation.getArgument(1);
                    GlobalSession s = new GlobalSession("app", "group", "order", 1000, !withBranches);
                    s.setXid(xid);
                    return s;
                })
                .when(reader)
                .readSession(anyString(), anyBoolean());

        List<String> visited = new ArrayList<>();
        int pageSize = 3;
        for (int offset = 0; ; offset += pageSize) {
            SessionCondition condition = new SessionCondition(
                    GlobalStatus.Committing, GlobalStatus.CommitRetrying, GlobalStatus.AsyncCommitting);
            condition.setOffset(offset);
            condition.setPageSize(pageSize);
            condition.setLazyLoadBranch(false);
            List<GlobalSession> page = reader.readSession(condition);
            if (page.isEmpty()) {
                break;
            }
            for (GlobalSession s : page) {
                assertFalse(s.isLazyLoadBranch());
                visited.add(s.getXid());
            }
        }

        assertEquals(Arrays.asList("a0", "a1", "a2", "a3", "a4", "b0", "b1", "b2", "c0", "c1", "c2", "c3"), visited);
    }

    @Test
    void pagedReadSessionHandlesDefaultsNegativeOffsetFallbackLimitAndBeginRoute() {
        RedisTransactionStoreManager reader = spy(store);
        reader.setLogQueryLimit(4);
        GlobalSession session = new GlobalSession("app", "group", "order", 1000);
        session.setXid("xid-1");
        doReturn(session).when(reader).readSession(anyString(), anyBoolean());

        String committingKey = "SEATA_STATUS_" + GlobalStatus.Committing.getCode();
        when(jedis.lrange(eq(committingKey), anyLong(), anyLong())).thenReturn(Collections.singletonList("xid-1"));

        // 1. Only pageSize set -> offset defaults to 0
        SessionCondition pageOnly = new SessionCondition(GlobalStatus.Committing);
        pageOnly.setPageSize(2);
        assertEquals(1, reader.readSession(pageOnly).size());
        verify(jedis).lrange(committingKey, 0L, 1L);

        // 2. Negative offset clamped to 0, non-positive pageSize falls back to logQueryLimit (4)
        SessionCondition negativeOffset = new SessionCondition(GlobalStatus.Committing);
        negativeOffset.setOffset(-5);
        negativeOffset.setPageSize(0);
        assertEquals(1, reader.readSession(negativeOffset).size());
        verify(jedis).lrange(committingKey, 0L, 3L);

        // 3. Duplicate statuses deduplicated to single key (no llen pipeline needed)
        SessionCondition dupStatuses = new SessionCondition(GlobalStatus.Committing, GlobalStatus.Committing);
        dupStatuses.setOffset(2);
        dupStatuses.setPageSize(2);
        assertEquals(1, reader.readSession(dupStatuses).size());
        verify(jedis).lrange(committingKey, 2L, 3L);
        verify(jedis, never()).pipelined();

        // 4. Single Begin status still uses sorted-set timeout path even when offset/pageSize set
        doReturn(Collections.emptyList()).when(reader).readSortByTimeoutBeginSessions(anyBoolean());
        SessionCondition beginPaged = new SessionCondition(GlobalStatus.Begin);
        beginPaged.setOffset(0);
        beginPaged.setPageSize(5);
        reader.readSession(beginPaged);
        verify(reader).readSortByTimeoutBeginSessions(true);

        // 5. logQueryLimit <= 0 with null pageSize returns empty immediately
        reader.setLogQueryLimit(0);
        SessionCondition zeroLimit = new SessionCondition(GlobalStatus.Committing);
        zeroLimit.setOffset(0);
        assertTrue(reader.readSession(zeroLimit).isEmpty());
    }

    @Test
    void pagedReadSessionUnderConcurrentListMutationAndDeletedHashIsBestEffort() {
        List<String> mutableList = new ArrayList<>(Arrays.asList("x1", "x2", "x3", "x4", "x5", "x6"));
        Set<String> deletedHashes = new HashSet<>();
        String statusKey = "SEATA_STATUS_" + GlobalStatus.Committing.getCode();
        when(jedis.lrange(eq(statusKey), anyLong(), anyLong())).thenAnswer(invocation -> {
            long start = invocation.getArgument(1);
            long end = invocation.getArgument(2);
            if (start >= mutableList.size()) {
                return Collections.emptyList();
            }
            int from = (int) Math.max(start, 0);
            int to = (int) Math.min(end + 1, mutableList.size());
            return new ArrayList<>(mutableList.subList(from, to));
        });

        RedisTransactionStoreManager reader = spy(store);
        doAnswer(invocation -> {
                    String xid = invocation.getArgument(0);
                    if (deletedHashes.contains(xid)) {
                        return null;
                    }
                    GlobalSession s = new GlobalSession("app", "group", "order", 1000, true);
                    s.setXid(xid);
                    return s;
                })
                .when(reader)
                .readSession(anyString(), anyBoolean());

        // Page 1 (offset=0, pageSize=2) -> [x1, x2]
        SessionCondition page1Cond = new SessionCondition(GlobalStatus.Committing);
        page1Cond.setOffset(0);
        page1Cond.setPageSize(2);
        page1Cond.setLazyLoadBranch(true);
        assertTrue(page1Cond.isRedisPagedQuery());
        List<GlobalSession> page1 = reader.readSession(page1Cond);
        assertEquals(
                Arrays.asList("x1", "x2"),
                Arrays.asList(page1.get(0).getXid(), page1.get(1).getXid()));

        // Concurrent mutations between page 1 and page 2:
        // - x1 removed from head (lrem), shifting list to [x2, x3, x4, x5, x6]
        // - x4 hash deleted while xid still present in list
        mutableList.remove("x1");
        deletedHashes.add("x4");

        // Page 2 (offset=2, pageSize=2) -> lrange(2, 3) returns [x4, x5]; x4 hash is null -> short page [x5]
        // (x3 was shifted ahead of offset=2; callers rely on next tick for eventual consistency)
        SessionCondition page2Cond = new SessionCondition(GlobalStatus.Committing);
        page2Cond.setOffset(2);
        page2Cond.setPageSize(2);
        page2Cond.setLazyLoadBranch(true);
        List<GlobalSession> page2 = reader.readSession(page2Cond);
        assertEquals(
                Collections.singletonList("x5"),
                Collections.singletonList(page2.get(0).getXid()));

        // Caller MUST NOT stop on short page (size 1 < pageSize 2); continue until empty page
        SessionCondition page3Cond = new SessionCondition(GlobalStatus.Committing);
        page3Cond.setOffset(4);
        page3Cond.setPageSize(2);
        page3Cond.setLazyLoadBranch(true);
        List<GlobalSession> page3 = reader.readSession(page3Cond);
        assertEquals(
                Collections.singletonList("x6"),
                Collections.singletonList(page3.get(0).getXid()));

        // Page 4 (offset=6, pageSize=2) -> empty page signals termination
        SessionCondition page4Cond = new SessionCondition(GlobalStatus.Committing);
        page4Cond.setOffset(6);
        page4Cond.setPageSize(2);
        page4Cond.setLazyLoadBranch(true);
        assertTrue(reader.readSession(page4Cond).isEmpty());
    }

    @Test
    void batchDeleteGlobalSessionsUsesPipelinedCheckAndDelete() {
        assertTrue(store.removeGlobalSessions(null));
        assertTrue(store.removeGlobalSessions(Collections.emptyList()));
        assertTrue(store.removeGlobalSessions(Collections.singletonList(null)));
        assertTrue(store.deleteGlobalTransactionDOs(null));
        assertTrue(store.deleteGlobalTransactionDOs(Collections.emptyList()));
        assertTrue(store.deleteGlobalTransactionDOs(Collections.singletonList(null)));
        factory.verify(JedisPooledFactory::getJedisInstance, never());

        GlobalSession beginSession = new GlobalSession("app", "group", "tx1", 1000);
        beginSession.setXid("127.0.0.1:8091:101");
        beginSession.setTransactionId(101L);
        beginSession.setStatus(GlobalStatus.Begin);

        GlobalSession unknownSession = new GlobalSession("app", "group", "tx2", 1000);
        unknownSession.setXid("127.0.0.1:8091:102");
        unknownSession.setTransactionId(102L);
        unknownSession.setStatus(GlobalStatus.UnKnown);

        GlobalSession committedSession = new GlobalSession("app", "group", "tx3", 1000);
        committedSession.setXid("127.0.0.1:8091:103");
        committedSession.setTransactionId(103L);
        committedSession.setStatus(GlobalStatus.Committed);

        GlobalSession missingSession = new GlobalSession("app", "group", "tx4", 1000);
        missingSession.setXid("127.0.0.1:8091:104");
        missingSession.setTransactionId(104L);
        missingSession.setStatus(GlobalStatus.Begin);

        when(pipeline.syncAndReturnAll())
                .thenReturn(
                        Arrays.asList(beginSession.getXid(), unknownSession.getXid(), committedSession.getXid(), null));

        assertTrue(store.removeGlobalSessions(
                Arrays.asList(beginSession, unknownSession, committedSession, missingSession)));

        verify(pipeline).hget("SEATA_GLOBAL_101", "xid");
        verify(pipeline).hget("SEATA_GLOBAL_102", "xid");
        verify(pipeline).hget("SEATA_GLOBAL_103", "xid");
        verify(pipeline).hget("SEATA_GLOBAL_104", "xid");

        verify(pipeline).lrem("SEATA_STATUS_" + GlobalStatus.Begin.getCode(), 0, beginSession.getXid());
        verify(pipeline).del("SEATA_GLOBAL_101");
        verify(pipeline).zrem("SEATA_BEGIN_TRANSACTIONS", "SEATA_GLOBAL_101");

        verify(pipeline).lrem("SEATA_STATUS_" + GlobalStatus.UnKnown.getCode(), 0, unknownSession.getXid());
        verify(pipeline).del("SEATA_GLOBAL_102");
        verify(pipeline).zrem("SEATA_BEGIN_TRANSACTIONS", "SEATA_GLOBAL_102");

        verify(pipeline).lrem("SEATA_STATUS_" + GlobalStatus.Committed.getCode(), 0, committedSession.getXid());
        verify(pipeline).del("SEATA_GLOBAL_103");
        verify(pipeline, never()).zrem("SEATA_BEGIN_TRANSACTIONS", "SEATA_GLOBAL_103");

        verify(pipeline, never()).del("SEATA_GLOBAL_104");
        verify(pipeline, times(1)).sync();
        verify(jedis, times(2)).pipelined();
        verify(jedis, times(1)).close();
    }

    @Test
    void batchDeleteIsIdempotentWhenAllMissingAndWrapsErrors() {
        GlobalTransactionDO missing1 = global();
        GlobalTransactionDO missing2 = global();
        missing2.setTransactionId(43L);
        missing2.setXid("127.0.0.1:8091:43");

        when(pipeline.syncAndReturnAll()).thenReturn(Arrays.asList(null, ""));
        assertTrue(store.deleteGlobalTransactionDOs(Arrays.asList(missing1, missing2)));
        verify(jedis, times(1)).pipelined();
        verify(pipeline, never()).sync();
        verify(pipeline, never()).del(anyString());

        when(pipeline.syncAndReturnAll()).thenReturn(Collections.singletonList(new RuntimeException("redis err")));
        assertThrows(RedisException.class, () -> store.deleteGlobalTransactionDOs(Collections.singletonList(missing1)));

        factory.when(JedisPooledFactory::getJedisInstance).thenThrow(new IllegalStateException("offline"));
        assertThrows(RedisException.class, () -> store.deleteGlobalTransactionDOs(Collections.singletonList(missing1)));
    }

    @Test
    void batchDeleteChunksLargeBatchesAcrossPipelines() {
        int total = RedisTransactionStoreManager.MAX_BATCH_DELETE_SIZE + 20;
        List<GlobalTransactionDO> items = new ArrayList<>(total);
        List<Object> firstChunkXids = new ArrayList<>(RedisTransactionStoreManager.MAX_BATCH_DELETE_SIZE);
        List<Object> secondChunkXids = new ArrayList<>(20);
        for (int i = 0; i < total; i++) {
            GlobalTransactionDO item = new GlobalTransactionDO();
            String xid = "127.0.0.1:8091:" + (1000 + i);
            item.setXid(xid);
            item.setTransactionId(1000L + i);
            item.setStatus(GlobalStatus.Committed.getCode());
            items.add(item);
            if (i < RedisTransactionStoreManager.MAX_BATCH_DELETE_SIZE) {
                firstChunkXids.add(xid);
            } else {
                secondChunkXids.add(xid);
            }
        }
        when(pipeline.syncAndReturnAll()).thenReturn(firstChunkXids, secondChunkXids);

        assertTrue(store.deleteGlobalTransactionDOs(items));
        // 2 chunks: each chunk checks out Jedis once and uses 2 pipelines (check + delete)
        verify(jedis, times(2)).close();
        verify(jedis, times(4)).pipelined();
        verify(pipeline, times(2)).syncAndReturnAll();
        verify(pipeline, times(2)).sync();
        verify(pipeline, times(total)).hget(anyString(), eq("xid"));
        verify(pipeline, times(total)).del(anyString());
    }
}
