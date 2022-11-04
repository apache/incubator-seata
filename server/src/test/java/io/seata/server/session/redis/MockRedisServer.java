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
package io.seata.server.session.redis;

import java.io.IOException;
import com.github.microwww.redis.RedisServer;
import io.seata.server.storage.redis.JedisPooledFactory;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * @author funkye
 */
public class MockRedisServer {

    static {
        RedisServer server = new RedisServer();
        try {
            server.listener("127.0.0.1", 6789);
        } catch (IOException e) {
            e.printStackTrace();
        }
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMinIdle(10);
        poolConfig.setMaxIdle(100);
        JedisPooledFactory.getJedisPoolInstance(new JedisPool(poolConfig, "127.0.0.1", 6789, 2000));
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));
    }

    private static class Instance {
        private static final MockRedisServer mockredis = new MockRedisServer();
    }

    public static MockRedisServer getInstance() {
        return Instance.mockredis;
    }

}
