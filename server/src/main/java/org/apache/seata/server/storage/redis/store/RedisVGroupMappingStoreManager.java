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
import org.apache.seata.common.loader.LoadLevel;
import org.apache.seata.common.metadata.namingserver.Instance;
import org.apache.seata.core.store.MappingDO;
import org.apache.seata.server.storage.redis.JedisPooledFactory;
import org.apache.seata.server.store.VGroupMappingStoreManager;
import redis.clients.jedis.Jedis;

import java.util.HashMap;
import java.util.Map;

@LoadLevel(name = "redis")
public class RedisVGroupMappingStoreManager implements VGroupMappingStoreManager {

    private static final String REDIS_SPLIT_KEY = ":";

    @Override
    public boolean addVGroup(MappingDO mappingDO) {
        String vGroup = mappingDO.getVGroup();
        String namespace = mappingDO.getNamespace();
        String clusterName = mappingDO.getCluster();
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            jedis.hset(namespace, vGroup, clusterName);
            return true;
        } catch (Exception ex) {
            throw new RedisException(ex);
        }
    }

    @Override
    public boolean removeVGroup(String vGroup) {
        Instance instance = Instance.getInstance();
        String namespace = instance.getNamespace();
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            jedis.hdel(namespace, vGroup);
            return true;
        } catch (Exception ex) {
            throw new RedisException(ex);
        }
    }

    @Override
    public HashMap<String, Object> loadVGroups() {
        Instance instance = Instance.getInstance();
        String namespace = instance.getNamespace();
        String clusterName = instance.getClusterName();
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            Map<String, String> mappingKeyMap = jedis.hgetAll(namespace);
            HashMap<String, Object> result = new HashMap<>();
            for (Map.Entry<String, String> entry : mappingKeyMap.entrySet()) {
                result.put(entry.getKey(), null);
            }
            return result;
        } catch (Exception ex) {
            throw new RedisException(ex);
        }
    }
}
