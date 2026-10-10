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
import org.apache.seata.server.storage.redis.JedisPooledFactory;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;
import redis.clients.jedis.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RedisLuaLockerUnitTest extends org.apache.seata.server.BaseSpringBootTest {
    private Jedis jedis;
    private MockedStatic<JedisPooledFactory> factory;
    private RedisLuaLocker locker;
    private Map<String, String> scripts;
    private Map<String, String> previous;

    @BeforeEach
    void open() {
        scripts = (Map<String, String>) ReflectionTestUtils.getField(RedisLuaLocker.class, "LOCK_SHA_MAP");
        previous = new HashMap<>(scripts);
        scripts.clear();
        jedis = mock(Jedis.class);
        when(jedis.scriptLoad(anyString())).thenReturn("sha");
        factory = mockStatic(JedisPooledFactory.class);
        factory.when(JedisPooledFactory::getJedisInstance).thenReturn(jedis);
        locker = new RedisLuaLocker();
    }

    @AfterEach
    void close() {
        scripts.clear();
        scripts.putAll(previous);
        factory.close();
    }

    private List<RowLock> rows() {
        RowLock r = new RowLock();
        r.setXid("host:8091:42");
        r.setTransactionId(42L);
        r.setBranchId(7L);
        r.setResourceId("db");
        r.setTableName("orders");
        r.setPk("1");
        r.setRowKey("db^^^orders^^^1");
        return Arrays.asList(r, r);
    }

    @Test
    void acquireDeduplicatesRowsAndChecksReturnedOwner() {
        when(jedis.evalsha(anyString(), anyList(), anyList()))
                .thenReturn("{\"success\":true,\"data\":\"host:8091:42\"}");
        assertTrue(locker.acquireLock(rows()));
        verify(jedis)
                .evalsha(
                        eq("sha"),
                        argThat(k -> k.size() == 3 && k.get(2).equals("7")),
                        argThat(a -> a.get(0).equals("1") && a.get(2).equals("host:8091:42")));
        when(jedis.evalsha(anyString(), anyList(), anyList()))
                .thenReturn("{\"success\":false,\"status\":\"AnotherHoldIng\",\"data\":\"other\"}");
        assertFalse(locker.acquireLock(rows()));
        when(jedis.evalsha(anyString(), anyList(), anyList()))
                .thenReturn("{\"success\":false,\"status\":\"AnotherRollbackIng\"}");
        assertThrows(StoreException.class, () -> locker.acquireLock(rows()));
    }

    @Test
    void lockabilityReleaseAndStatusUseCorrectScriptArguments() {
        assertTrue(locker.acquireLock(Collections.emptyList()));
        assertTrue(locker.isLockable(Collections.emptyList()));
        when(jedis.evalsha(anyString(), anyList(), anyList())).thenReturn("true", "false");
        assertTrue(locker.isLockable(rows()));
        assertFalse(locker.isLockable(rows()));
        String xid = "host:8091:42", key = locker.buildXidLockKey(xid);
        assertTrue(locker.releaseLock(xid));
        assertTrue(locker.releaseLock(xid, 7L));
        assertTrue(locker.releaseLock(xid, null));
        verify(jedis).evalsha("sha", Arrays.asList(key, "7"), Collections.emptyList());
        locker.updateLockStatus(xid, LockStatus.Rollbacking);
        verify(jedis)
                .evalsha(
                        "sha",
                        Arrays.asList(key, "status"),
                        Collections.singletonList(String.valueOf(LockStatus.Rollbacking.getCode())));
    }

    @Test
    void unavailableScriptsFallBackToPipeline() {
        scripts.clear();
        when(jedis.scriptLoad(anyString())).thenThrow(new UnsupportedOperationException("no scripting"));
        locker = new RedisLuaLocker();
        assertTrue(scripts.isEmpty());
        Pipeline pipeline = mock(Pipeline.class);
        when(jedis.pipelined()).thenReturn(pipeline);
        when(pipeline.syncAndReturnAll()).thenReturn(Collections.singletonList("host:8091:42"));
        assertTrue(locker.acquireLock(rows()));
        assertTrue(locker.isLockable(rows()));
        assertTrue(locker.releaseLock("host:8091:42"));
        assertTrue(locker.releaseLock("host:8091:42", 7L));
        locker.updateLockStatus("host:8091:42", LockStatus.Locked);
        verify(jedis, never()).evalsha(anyString(), anyList(), anyList());
    }
}
