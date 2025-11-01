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

import org.apache.seata.core.lock.Locker;
import org.apache.seata.server.BaseSpringBootTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

@EnabledIfSystemProperty(named = "redisCaseEnabled", matches = "true")
public class RedisLockerFactoryTest extends BaseSpringBootTest {

    @Test
    public void testGetLocker() {
        Locker locker = RedisLockerFactory.getLocker();
        Assertions.assertNotNull(locker);
    }

    @Test
    public void testGetLockerSingleton() {
        Locker locker1 = RedisLockerFactory.getLocker();
        Locker locker2 = RedisLockerFactory.getLocker();

        Assertions.assertNotNull(locker1);
        Assertions.assertNotNull(locker2);
        Assertions.assertEquals(locker1, locker2);
    }

    @Test
    public void testGetLockerType() {
        Locker locker = RedisLockerFactory.getLocker();
        Assertions.assertTrue(locker instanceof RedisLocker || locker instanceof RedisLuaLocker);
    }
}
