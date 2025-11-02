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
package org.apache.seata.server.storage.redis.lock;

import org.apache.seata.core.lock.AbstractLocker;
import org.apache.seata.server.BaseSpringBootTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.ArrayList;

@EnabledIfSystemProperty(named = "redisCaseEnabled", matches = "true")
public class RedisLockerTest extends BaseSpringBootTest {

    private RedisLocker redisLocker;

    @BeforeEach
    public void setUp() {
        redisLocker = new RedisLocker();
    }

    @Test
    public void testInstanceCreation() {
        Assertions.assertNotNull(redisLocker);
    }

    @Test
    public void testExtendsAbstractLocker() {
        Assertions.assertTrue(redisLocker instanceof AbstractLocker);
    }

    @Test
    public void testAcquireLockWithEmptyList() {
        boolean result = redisLocker.acquireLock(new ArrayList<>());
        Assertions.assertTrue(result);
    }

    @Test
    public void testAcquireLockWithAutoCommitAndEmptyList() {
        boolean result = redisLocker.acquireLock(new ArrayList<>(), true, false);
        Assertions.assertTrue(result);
    }
}
