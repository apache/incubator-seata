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
import org.apache.seata.common.metadata.Instance;
import org.apache.seata.core.store.MappingDO;
import org.apache.seata.server.BaseSpringBootTest;
import org.apache.seata.server.storage.redis.JedisPooledFactory;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import redis.clients.jedis.Jedis;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RedisVGroupMappingUnitTest extends BaseSpringBootTest {
    @Test
    void mappingOperationsAreScopedToNamespaceAndCluster() {
        Jedis jedis = mock(Jedis.class);
        Instance instance = mock(Instance.class);
        when(instance.getNamespace()).thenReturn("tenant");
        when(instance.getClusterName()).thenReturn("east");
        try (MockedStatic<JedisPooledFactory> pool = mockStatic(JedisPooledFactory.class);
                MockedStatic<Instance> instances = mockStatic(Instance.class)) {
            pool.when(JedisPooledFactory::getJedisInstance).thenReturn(jedis);
            instances.when(Instance::getInstance).thenReturn(instance);
            RedisVGroupMappingStoreManager manager = new RedisVGroupMappingStoreManager();
            MappingDO mapping = new MappingDO();
            mapping.setVGroup("payments");
            mapping.setNamespace("tenant");
            mapping.setCluster("east");
            assertTrue(manager.addVGroup(mapping));
            String key = "SEATA_NAMINGSERVER_NAMESPACE_tenant";
            verify(jedis).hset(key, "payments", "east");
            when(jedis.hget(key, "payments")).thenReturn("west", "east");
            assertFalse(manager.removeVGroup("payments"));
            assertTrue(manager.removeVGroup("payments"));
            verify(jedis).hdel(key, "payments");
            Map<String, String> mappings = new HashMap<>();
            mappings.put("payments", "east");
            mappings.put("orders", "west");
            when(jedis.hgetAll(key)).thenReturn(mappings);
            assertEquals(
                    Collections.singleton("payments"), manager.loadVGroups().keySet());
            pool.when(JedisPooledFactory::getJedisInstance).thenThrow(new IllegalStateException("offline"));
            assertThrows(RedisException.class, () -> manager.addVGroup(mapping));
            assertThrows(RedisException.class, () -> manager.removeVGroup("payments"));
            assertThrows(RedisException.class, manager::loadVGroups);
        }
    }
}
