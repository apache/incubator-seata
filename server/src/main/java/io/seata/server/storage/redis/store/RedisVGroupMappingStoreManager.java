/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.server.storage.redis.store;

import io.seata.common.exception.RedisException;
import io.seata.common.loader.LoadLevel;
import io.seata.common.util.BeanUtils;
import io.seata.common.util.StringUtils;
import io.seata.core.store.MappingDO;
import io.seata.server.storage.redis.JedisPooledFactory;
import io.seata.server.store.VGroupMappingStoreManager;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

import java.util.HashMap;
import java.util.List;


@LoadLevel(name = "redis")
public class RedisVGroupMappingStoreManager implements VGroupMappingStoreManager {

    private final String mappingListKey = "mapping";


    @Override
    public boolean addVGroup(MappingDO mappingDO) {
        String vGroup = mappingDO.getVGroup();
        try (Jedis jedis = JedisPooledFactory.getJedisInstance(); Pipeline pipelined = jedis.pipelined()) {
            pipelined.hmset(vGroup, BeanUtils.objectToMap(mappingDO));
            pipelined.rpush(mappingListKey, vGroup);
            pipelined.sync();
            return true;
        } catch (Exception ex) {
            throw new RedisException(ex);
        }
    }

    @Override
    public boolean removeVGroup(String vGroup) {
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            String clusterName = jedis.hget(vGroup, "cluster");
            if (StringUtils.isEmpty(clusterName)) {
                return true;
            }
            try (Pipeline pipelined = jedis.pipelined()) {
                pipelined.lrem(mappingListKey, 0, vGroup);
                pipelined.del(vGroup);
                pipelined.sync();
            }
            return true;
        } catch (Exception ex) {
            throw new RedisException(ex);
        }
    }

    @Override
    public HashMap<String, Object> load() {
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            List<String> mappingKeyList = jedis.lrange(mappingListKey, 0, -1);
            HashMap<String, Object> result = new HashMap<>();
            for (String vGroup : mappingKeyList) {
                result.put(vGroup, null);
            }
            return result;
        } catch (Exception ex) {
            throw new RedisException(ex);
        }

    }
}
