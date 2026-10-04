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
package org.apache.seata.server.storage.redis;

import org.apache.seata.common.exception.RedisException;
import org.apache.seata.config.*;
import org.apache.seata.server.BaseSpringBootTest;
import org.junit.jupiter.api.*;
import org.mockito.MockedConstruction;
import org.springframework.test.util.ReflectionTestUtils;
import redis.clients.jedis.*;
import redis.clients.jedis.util.Pool;

import java.util.*;

import static org.apache.seata.common.ConfigurationKeys.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JedisPoolUnitTest extends BaseSpringBootTest {
    private Object previous;
    private final Map<String, String> properties = new HashMap<>();

    @BeforeEach
    void open() {
        previous = ReflectionTestUtils.getField(JedisPooledFactory.class, "jedisPool");
        ReflectionTestUtils.setField(JedisPooledFactory.class, "jedisPool", null);
    }

    @AfterEach
    void close() {
        ReflectionTestUtils.setField(JedisPooledFactory.class, "jedisPool", previous);
        properties.forEach((key, value) -> {
            if (value == null) {
                System.clearProperty(key);
            } else {
                System.setProperty(key, value);
            }
        });
        properties.clear();
        ConfigurationCache.clear();
    }

    private void config(String key, String value) {
        if (!properties.containsKey(key)) {
            properties.put(key, System.getProperty(key));
        }
        System.setProperty(key, value);
        ConfigurationCache.clear();
    }

    @Test
    void suppliedPoolIsCachedAndConnectionsComeFromIt() {
        Pool<Jedis> pool = mock(Pool.class);
        Jedis jedis = mock(Jedis.class);
        when(pool.getResource()).thenReturn(jedis);
        assertSame(pool, JedisPooledFactory.getJedisPoolInstance(pool));
        assertSame(pool, JedisPooledFactory.getJedisPoolInstance(mock(Pool.class)));
        assertSame(jedis, JedisPooledFactory.getJedisInstance());
    }

    @Test
    void singleModePassesHostPortPasswordAndPoolBounds() {
        config(STORE_REDIS_MODE, REDIS_SINGLE_MODE);
        config(STORE_REDIS_SINGLE_HOST, "redis.example");
        config(STORE_REDIS_SINGLE_PORT, "1234");
        config(STORE_REDIS_PASSWORD, "secret");
        config(STORE_PUBLIC_KEY, "");
        try (MockedConstruction<JedisPool> pools = mockConstruction(JedisPool.class, (mock, context) -> {
            assertEquals("redis.example", context.arguments().get(1));
            assertEquals(1234, context.arguments().get(2));
            assertEquals("secret", context.arguments().get(4));
            assertTrue(((JedisPoolConfig) context.arguments().get(0)).getMaxTotal() > 0);
        })) {
            assertSame(
                    JedisPooledFactory.getJedisPoolInstance(),
                    pools.constructed().get(0));
        }
    }

    @Test
    void singleModeUsesLegacyAddressAndNoPassword() {
        config(STORE_REDIS_MODE, REDIS_SINGLE_MODE);
        config(STORE_REDIS_SINGLE_HOST, "");
        config(STORE_REDIS_SINGLE_PORT, "0");
        config(STORE_REDIS_HOST, "legacy");
        config(STORE_REDIS_PORT, "6380");
        config(STORE_REDIS_PASSWORD, "");
        try (MockedConstruction<JedisPool> pools = mockConstruction(JedisPool.class, (mock, context) -> {
            assertEquals("legacy", context.arguments().get(1));
            assertEquals(6380, context.arguments().get(2));
            assertNull(context.arguments().get(4));
        })) {
            assertSame(
                    JedisPooledFactory.getJedisPoolInstance(),
                    pools.constructed().get(0));
        }
    }

    @Test
    void sentinelModeParsesHostsAndRejectsMissingMaster() {
        config(STORE_REDIS_MODE, REDIS_SENTINEL_MODE);
        config(STORE_REDIS_SENTINEL_MASTERNAME, "");
        config(STORE_REDIS_PASSWORD, "");
        assertThrows(RedisException.class, JedisPooledFactory::getJedisPoolInstance);
        config(STORE_REDIS_SENTINEL_MASTERNAME, "primary");
        config(STORE_REDIS_SENTINEL_HOST, "s1:26379,s2:26379");
        config(STORE_REDIS_SENTINEL_PASSWORD, "");
        try (MockedConstruction<JedisSentinelPool> pools =
                mockConstruction(JedisSentinelPool.class, (mock, context) -> {
                    assertEquals("primary", context.arguments().get(0));
                    assertEquals(
                            new HashSet<>(Arrays.asList("s1:26379", "s2:26379")),
                            context.arguments().get(1));
                })) {
            assertSame(
                    JedisPooledFactory.getJedisPoolInstance(),
                    pools.constructed().get(0));
        }
    }

    @Test
    void invalidModeIsRejectedBeforeOpeningPool() {
        config(STORE_REDIS_MODE, "invalid");
        config(STORE_REDIS_PASSWORD, "");
        assertThrows(RedisException.class, JedisPooledFactory::getJedisPoolInstance);
    }
}
