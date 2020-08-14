/*
 * Copyright 1999-2019 Seata.io Group.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package io.seata.rm.datasource.undo;

import com.alibaba.fastjson.JSON;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.StringUtils;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.store.redis.JedisPooledFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author funkye
 */
public class UndoLogCache {

    private static final String DEFAULT_UNDO_LOG_CACHE_KEY_XID_PREFIX = "UNDO_LOG_CACHE_KEY_XID_";

    private static final String DEFAULT_BRANCH_ID_PREFIX = "BRANCH_ID_";

    private static boolean cacheEnable = false;

    private static final int XID = 0;

    private static final int BRANCH_ID = 1;

    private static final String INITIAL_CURSOR = "0";

    public static final int CONTEXT = 2;

    public static final int ROLL_BACK_INFO = 3;

    public static final int STATE = 4;

    /**
     * The query limit.
     */
    private static int logQueryLimit =
        ConfigurationFactory.getInstance().getInt(ConfigurationKeys.CLIENT_UNDO_REDIS_QUERY_LIMIT, 100);;

    static {
        cacheEnable = ConfigurationFactory.getInstance().getBoolean(ConfigurationKeys.CLIENT_UNDO_REDIS_CACHE_ENABLE,
            cacheEnable);
        if (cacheEnable) {
            JedisPooledFactory.ACTIVATE_NAME = "undo";
        }
    }

    public static Object[] get(String xid, Long branchId) {
        if(cacheEnable) {
            try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
                String value = jedis.get(getCacheKey(xid, branchId));
                if (StringUtils.isNotBlank(value)) {
                    Object[] objects = JSON.parseObject(value, Object[].class);
                    if (objects != null && objects.length > 0) {
                        return objects;
                    }
                }
            }
        }
        return null;
    }

    public static void remove(String xid, Long branchId) {
        if(cacheEnable) {
            try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
                jedis.del(getCacheKey(xid, branchId));
            }
        }
    }

    public static void remove(Set<String> xids) {
        if(cacheEnable) {
            try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
                Set<String> keys = new HashSet<>();
                String cursor = INITIAL_CURSOR;
                ScanParams params = new ScanParams();
                params.count(logQueryLimit);
                params.match(DEFAULT_UNDO_LOG_CACHE_KEY_XID_PREFIX + "*");
                ScanResult<String> scans;
                do {
                    scans = jedis.scan(cursor, params);
                    keys.addAll(scans.getResult());
                    cursor = scans.getCursor();
                } while (!INITIAL_CURSOR.equals(cursor));
                if (CollectionUtils.isNotEmpty(keys)) {
                    Set<String> delKeys = new HashSet<>();
                    xids.forEach(xid -> keys.forEach(key -> {
                        if (key.contains(xid)) {
                            delKeys.add(key);
                        }
                    }));
                    if (CollectionUtils.isNotEmpty(delKeys)) {
                        jedis.del(delKeys.toArray(new String[0]));
                    }
                }
            }
        }
    }

    public static void put(Object[] objects) {
        if(cacheEnable) {
            try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
                String key = getCacheKey((String)objects[XID], (Long)objects[BRANCH_ID]);
                jedis.set(key, JSON.toJSONString(objects));
                jedis.expire(key, 60);
            }
        }
    }

    private static String getCacheKey(String xid, Long branchId) {
        StringBuilder sb = new StringBuilder();
        sb.append(DEFAULT_UNDO_LOG_CACHE_KEY_XID_PREFIX).append(xid).append(DEFAULT_BRANCH_ID_PREFIX).append(branchId);
        return sb.toString();
    }

}
