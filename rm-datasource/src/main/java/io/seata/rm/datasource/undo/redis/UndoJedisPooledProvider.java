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
package io.seata.rm.datasource.undo.redis;

import io.seata.common.loader.LoadLevel;
import io.seata.common.util.StringUtils;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.store.redis.AbstractJedisPooledProvider;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * @author funkye
 */
@LoadLevel(name = "undo")
public class UndoJedisPooledProvider extends AbstractJedisPooledProvider {

    @Override
    public JedisPool generate() {
        String password = CONFIGURATION.getConfig(ConfigurationKeys.CLIENT_UNDO_REDIS_PASSWORD);
        if (StringUtils.isBlank(password)) {
            password = null;
        }
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMinIdle(CONFIGURATION.getInt(ConfigurationKeys.CLIENT_UNDO_REDIS_MIN_CONN, MINCONN));
        poolConfig.setMaxIdle(CONFIGURATION.getInt(ConfigurationKeys.CLIENT_UNDO_REDIS_MAX_CONN, MAXCONN));
        JedisPool jedisPool =
            new JedisPool(poolConfig, CONFIGURATION.getConfig(ConfigurationKeys.CLIENT_UNDO_REDIS_HOST, HOST),
                CONFIGURATION.getInt(ConfigurationKeys.CLIENT_UNDO_REDIS_PORT, PORT), 60000, password,
                CONFIGURATION.getInt(ConfigurationKeys.CLIENT_UNDO_REDIS_DATABASE, DATABASE));
        return jedisPool;
    }
}
