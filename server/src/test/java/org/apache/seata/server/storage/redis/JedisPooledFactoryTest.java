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

import org.apache.seata.server.BaseSpringBootTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolAbstract;
import redis.clients.jedis.JedisPoolConfig;

import java.lang.reflect.Field;

@EnabledIfSystemProperty(named = "redisCaseEnabled", matches = "true")
public class JedisPooledFactoryTest extends BaseSpringBootTest {

    @BeforeEach
    public void setUp() throws Exception {
        // Reset the singleton jedisPool field to null before each test to ensure test isolation
        Field jedisPoolField = JedisPooledFactory.class.getDeclaredField("jedisPool");
        jedisPoolField.setAccessible(true);
        JedisPoolAbstract existingPool = (JedisPoolAbstract) jedisPoolField.get(null);

        // Close existing pool if present to prevent resource leaks
        if (existingPool != null) {
            existingPool.close();
        }

        // Reset to null
        jedisPoolField.set(null, null);
    }

    @AfterEach
    public void tearDown() throws Exception {
        // Clean up resources after each test
        Field jedisPoolField = JedisPooledFactory.class.getDeclaredField("jedisPool");
        jedisPoolField.setAccessible(true);
        JedisPoolAbstract pool = (JedisPoolAbstract) jedisPoolField.get(null);

        if (pool != null) {
            pool.close();
        }

        // Reset to null for next test
        jedisPoolField.set(null, null);
    }

    @Test
    public void testGetJedisPoolInstanceWithProvidedPool() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMinIdle(1);
        poolConfig.setMaxIdle(10);
        poolConfig.setMaxTotal(20);

        JedisPool jedisPool = new JedisPool(poolConfig, "127.0.0.1", 6379, 60000);

        JedisPoolAbstract poolInstance = JedisPooledFactory.getJedisPoolInstance(jedisPool);

        Assertions.assertNotNull(poolInstance);
        Assertions.assertEquals(jedisPool, poolInstance);
    }

    @Test
    public void testGetJedisPoolInstanceSingleton() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMinIdle(1);
        poolConfig.setMaxIdle(10);

        JedisPool jedisPool = new JedisPool(poolConfig, "127.0.0.1", 6379, 60000);

        JedisPoolAbstract instance1 = JedisPooledFactory.getJedisPoolInstance(jedisPool);
        JedisPoolAbstract instance2 = JedisPooledFactory.getJedisPoolInstance();

        Assertions.assertNotNull(instance1);
        Assertions.assertNotNull(instance2);
        Assertions.assertEquals(instance1, instance2);
    }

    @Test
    public void testGetJedisInstance() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMinIdle(1);
        poolConfig.setMaxIdle(10);

        JedisPool jedisPool = new JedisPool(poolConfig, "127.0.0.1", 6379, 60000);
        JedisPooledFactory.getJedisPoolInstance(jedisPool);

        Jedis jedis = JedisPooledFactory.getJedisInstance();

        Assertions.assertNotNull(jedis);
        jedis.close();
    }

    @Test
    public void testGetJedisInstanceMultipleTimes() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMinIdle(1);
        poolConfig.setMaxIdle(10);

        JedisPool jedisPool = new JedisPool(poolConfig, "127.0.0.1", 6379, 60000);
        JedisPooledFactory.getJedisPoolInstance(jedisPool);

        Jedis jedis1 = JedisPooledFactory.getJedisInstance();
        Jedis jedis2 = JedisPooledFactory.getJedisInstance();

        Assertions.assertNotNull(jedis1);
        Assertions.assertNotNull(jedis2);

        jedis1.close();
        jedis2.close();
    }
}
