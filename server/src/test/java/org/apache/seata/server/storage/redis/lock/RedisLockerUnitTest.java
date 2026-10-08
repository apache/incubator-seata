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
package org.apache.seata.server.storage.redis.lock;

import org.apache.seata.common.exception.StoreException;
import org.apache.seata.core.lock.RowLock;
import org.apache.seata.core.model.LockStatus;
import org.apache.seata.core.store.DistributedLockDO;
import org.apache.seata.server.storage.redis.JedisPooledFactory;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import redis.clients.jedis.*;
import redis.clients.jedis.params.SetParams;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RedisLockerUnitTest extends org.apache.seata.server.BaseSpringBootTest {
    private Jedis jedis;
    private Pipeline pipeline;
    private MockedStatic<JedisPooledFactory> factory;
    private final RedisLocker locker = new RedisLocker();

    @BeforeEach
    void open() {
        jedis = mock(Jedis.class);
        pipeline = mock(Pipeline.class);
        factory = mockStatic(JedisPooledFactory.class);
        factory.when(JedisPooledFactory::getJedisInstance).thenReturn(jedis);
        when(jedis.pipelined()).thenReturn(pipeline);
    }

    @AfterEach
    void close() {
        factory.close();
    }

    private RowLock row(String pk) {
        RowLock r = new RowLock();
        r.setXid("host:8091:42");
        r.setTransactionId(42L);
        r.setBranchId(7L);
        r.setResourceId("db");
        r.setTableName("orders");
        r.setPk(pk);
        r.setRowKey("db^^^orders^^^" + pk);
        return r;
    }

    @Test
    void emptyLocksAndReentrantOwnership() {
        assertTrue(locker.acquireLock(Collections.emptyList()));
        assertTrue(locker.isLockable(Collections.emptyList()));
        assertTrue(locker.releaseLock("host:8091:42", null));
        factory.verifyNoInteractions();
        when(pipeline.syncAndReturnAll()).thenReturn(Collections.singletonList("host:8091:42"));
        assertTrue(locker.acquireLock(Collections.singletonList(row("1"))));
        assertTrue(locker.isLockable(Collections.singletonList(row("1"))));
        verify(pipeline, never()).hsetnx(anyString(), anyString(), anyString());
    }

    @Test
    void conflictingOwnershipAndRollbackFailFast() {
        when(pipeline.syncAndReturnAll()).thenReturn(Collections.singletonList("other:8091:1"));
        assertFalse(locker.acquireLock(Collections.singletonList(row("1"))));
        assertFalse(locker.isLockable(Collections.singletonList(row("1"))));
        when(pipeline.syncAndReturnAll())
                .thenReturn(Arrays.asList("other:8091:1", String.valueOf(LockStatus.Rollbacking.getCode())));
        assertThrows(StoreException.class, () -> locker.acquireLock(Collections.singletonList(row("1")), false, false));
        verify(jedis, times(3)).close();
    }

    @Test
    void deduplicatesAndIndexesSuccessfulAcquisition() {
        when(pipeline.syncAndReturnAll())
                .thenReturn(Collections.singletonList(null), Arrays.asList(1, 1, 1, 1, 1, 1, 1));
        RowLock r = row("1");
        assertTrue(locker.acquireLock(Arrays.asList(r, r)));
        verify(pipeline, times(1)).hsetnx(locker.buildLockKey(r.getRowKey()), "xid", r.getXid());
        verify(jedis).hset(locker.buildXidLockKey(r.getXid()), "7", locker.buildLockKey(r.getRowKey()));
    }

    @Test
    void acquisitionRaceRemovesOnlyNewlyAcquiredRows() {
        when(pipeline.syncAndReturnAll())
                .thenReturn(Arrays.asList(null, null), Arrays.asList(1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0));
        assertFalse(locker.acquireLock(Arrays.asList(row("1"), row("2"))));
        verify(jedis).del(any(String[].class));
        verify(jedis, never()).hset(anyString(), anyString(), anyString());
    }

    @Test
    void releaseAndStatusUpdateTraverseAllBranchRows() {
        String xid = "host:8091:42", key = locker.buildXidLockKey(xid);
        Map<String, String> branches = new LinkedHashMap<>();
        branches.put("7", "row1;row2");
        branches.put("8", "row3");
        branches.put("9", "");
        when(jedis.hgetAll(key)).thenReturn(branches);
        locker.updateLockStatus(xid, LockStatus.Rollbacking);
        for (String row : Arrays.asList("row1", "row2", "row3")) {
            verify(pipeline).hset(row, "status", String.valueOf(LockStatus.Rollbacking.getCode()));
        }
        assertTrue(locker.releaseLock(xid));
        verify(pipeline).del(key);
        verify(pipeline).del(new String[] {"row1", "row2"});
        verify(pipeline).del("row3");
        when(jedis.hget(key, "7")).thenReturn("row1");
        assertTrue(locker.releaseLock(xid, 7L));
        verify(pipeline).hdel(key, "7");
        when(jedis.hgetAll(key)).thenReturn(Collections.emptyMap());
        assertTrue(locker.releaseLock(xid));
        locker.updateLockStatus(xid, LockStatus.Locked);
    }

    @Test
    void distributedLockHonorsOwnershipAndConnectionFailures() {
        DistributedLockDO lock = new DistributedLockDO();
        lock.setLockKey("lease");
        lock.setLockValue("owner");
        lock.setExpireTime(1000L);
        RedisDistributedLocker distributed = new RedisDistributedLocker();
        when(jedis.set(eq("lease"), eq("owner"), any(SetParams.class))).thenReturn("OK", null);
        assertTrue(distributed.acquireLock(lock));
        assertFalse(distributed.acquireLock(lock));
        when(jedis.get("lease")).thenReturn("other");
        assertTrue(distributed.releaseLock(lock));
        verify(jedis).unwatch();
        verify(jedis, never()).multi();
        Transaction transaction = mock(Transaction.class);
        when(jedis.multi()).thenReturn(transaction);
        when(jedis.get("lease")).thenReturn("owner");
        assertTrue(distributed.releaseLock(lock));
        verify(transaction).del("lease");
        verify(transaction).exec();
        factory.when(JedisPooledFactory::getJedisInstance).thenThrow(new IllegalStateException("offline"));
        assertFalse(distributed.acquireLock(lock));
        assertFalse(distributed.releaseLock(lock));
    }
}
