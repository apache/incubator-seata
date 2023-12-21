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
package io.seata.server.session.redis;

import java.io.IOException;

import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.server.storage.redis.session.RedisSessionManager;
import io.seata.server.storage.redis.store.RedisLuaTransactionStoreManager;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

/**
 * test RedisLuaTransactionStoreManager
 *
 * @author conghuhu
 */
@SpringBootTest
public class RedisLuaTransactionStoreManagerTest extends RedisTransactionStoreManagerTest {

    /**
     * because of mock redis server can not run lua script,
     * if you want to test lua mode, please modify application.yaml and config your redis instance info.
     * store.redis.type = lua
     *
     * @param context
     * @throws IOException
     */
    @BeforeAll
    public static void start(ApplicationContext context) throws IOException {
        MockRedisServer.getInstance();
        EnhancedServiceLoader.unloadAll();
        redisTransactionStoreManager = new RedisLuaTransactionStoreManager();
        RedisSessionManager redisSessionManager = new RedisSessionManager();
        redisSessionManager.setTransactionStoreManager(redisTransactionStoreManager);
        sessionManager = redisSessionManager;
    }

}
