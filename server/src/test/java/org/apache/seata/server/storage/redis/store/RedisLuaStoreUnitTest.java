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
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.core.store.BranchTransactionDO;
import org.apache.seata.core.store.GlobalTransactionDO;
import org.apache.seata.server.BaseSpringBootTest;
import org.apache.seata.server.storage.redis.JedisPooledFactory;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;
import redis.clients.jedis.*;
import redis.clients.jedis.exceptions.JedisNoScriptException;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RedisLuaStoreUnitTest extends BaseSpringBootTest {
    private Jedis jedis;
    private MockedStatic<JedisPooledFactory> factory;
    private RedisLuaTransactionStoreManager store;
    private Map<String, String> scripts;
    private Map<String, String> previous;

    @BeforeEach
    void open() {
        scripts = (Map<String, String>)
                ReflectionTestUtils.getField(RedisLuaTransactionStoreManager.class, "LOCK_SHA_MAP");
        previous = new HashMap<>(scripts);
        scripts.clear();
        jedis = mock(Jedis.class);
        when(jedis.scriptLoad(anyString())).thenReturn("sha");
        factory = mockStatic(JedisPooledFactory.class);
        factory.when(JedisPooledFactory::getJedisInstance).thenReturn(jedis);
        store = new RedisLuaTransactionStoreManager();
    }

    @AfterEach
    void close() {
        scripts.clear();
        scripts.putAll(previous);
        factory.close();
    }

    private GlobalTransactionDO global() {
        GlobalTransactionDO g = new GlobalTransactionDO();
        g.setXid("host:8091:42");
        g.setTransactionId(42L);
        g.setStatus(GlobalStatus.Committing.getCode());
        g.setBeginTime(100L);
        g.setTimeout(1000);
        return g;
    }

    private BranchTransactionDO branch() {
        BranchTransactionDO b = new BranchTransactionDO();
        b.setBranchId(7L);
        b.setTransactionId(42L);
        b.setXid("host:8091:42");
        b.setStatus(1);
        b.setApplicationData("payload");
        return b;
    }

    @Test
    void writesCarryTransactionKeysAndTimeoutIndex() {
        assertTrue(store.insertGlobalTransactionDO(global()));
        assertTrue(store.insertBranchTransactionDO(branch()));
        verify(jedis)
                .evalsha(
                        eq("sha"),
                        argThat(k -> k.contains("SEATA_GLOBAL_42") && k.contains("SEATA_BEGIN_TRANSACTIONS")),
                        argThat(a -> a.contains("1100") && a.get(0).equals("global")));
        verify(jedis).evalsha(eq("sha"), argThat(k -> k.contains("SEATA_BRANCH_7")), argThat(a -> a.get(0)
                .equals("branch")));
        assertTrue(store.deleteGlobalTransactionDO(global()));
        assertTrue(store.deleteBranchTransactionDO(branch()));
        verify(jedis)
                .evalsha(
                        "sha",
                        Arrays.asList("SEATA_BRANCH_7", "SEATA_BRANCHES_host:8091:42", "xid"),
                        Collections.singletonList("branch"));
    }

    @Test
    void updatesInterpretScriptResultsAndReloadEvictedScript() {
        when(jedis.evalsha(anyString(), anyList(), anyList()))
                .thenThrow(new JedisNoScriptException("evicted"))
                .thenReturn("{\"success\":true}");
        assertTrue(store.updateBranchTransactionDO(branch()));
        assertTrue(store.updateGlobalTransactionDO(global()));
        verify(jedis, times(3)).evalsha(anyString(), anyList(), anyList());
        when(jedis.evalsha(anyString(), anyList(), anyList()))
                .thenReturn("{\"success\":false,\"status\":\"NotExisted\"}");
        assertThrows(RedisException.class, () -> store.updateBranchTransactionDO(branch()));
        assertThrows(RedisException.class, () -> store.updateGlobalTransactionDO(global()));
        when(jedis.evalsha(anyString(), anyList(), anyList()))
                .thenReturn("{\"success\":false,\"status\":\"ChangeStatusFail\",\"data\":\"9\"}");
        assertThrows(RedisException.class, () -> store.updateGlobalTransactionDO(global()));
    }

    @Test
    void failedScriptLoadFallsBackToPipeline() {
        when(jedis.scriptLoad(anyString())).thenThrow(new UnsupportedOperationException("no scripting"));
        store = new RedisLuaTransactionStoreManager();
        assertTrue(scripts.isEmpty());
        Pipeline pipeline = mock(Pipeline.class);
        when(jedis.pipelined()).thenReturn(pipeline);
        assertTrue(store.insertGlobalTransactionDO(global()));
        assertTrue(store.insertBranchTransactionDO(branch()));
        assertTrue(store.deleteGlobalTransactionDO(global()));
        assertTrue(store.deleteBranchTransactionDO(branch()));
        when(jedis.hget("SEATA_BRANCH_7", "status")).thenReturn("1");
        assertTrue(store.updateBranchTransactionDO(branch()));
        when(jedis.hmget(anyString(), any(String[].class))).thenReturn(Arrays.asList("2", "100"));
        assertTrue(store.updateGlobalTransactionDO(global()));
        verify(pipeline, times(2)).sync();
    }

    @Test
    void everyScriptWriteWrapsConnectionFailure() {
        factory.when(JedisPooledFactory::getJedisInstance).thenThrow(new IllegalStateException("offline"));
        assertThrows(RedisException.class, () -> store.insertGlobalTransactionDO(global()));
        assertThrows(RedisException.class, () -> store.updateGlobalTransactionDO(global()));
        assertThrows(RedisException.class, () -> store.deleteGlobalTransactionDO(global()));
        assertThrows(RedisException.class, () -> store.insertBranchTransactionDO(branch()));
        assertThrows(RedisException.class, () -> store.updateBranchTransactionDO(branch()));
        assertThrows(RedisException.class, () -> store.deleteBranchTransactionDO(branch()));
    }
}
