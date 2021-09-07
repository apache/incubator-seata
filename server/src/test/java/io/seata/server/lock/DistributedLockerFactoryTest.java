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
package io.seata.server.lock;

import io.seata.core.store.DefaultDistributedLocker;
import io.seata.core.store.DistributedLocker;
import io.seata.server.lock.distributed.DistributedLockerFactory;
import io.seata.server.storage.redis.lock.RedisDistributedLocker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author zhongxiang.wang
 * @description Distributed locker factory test
 */
@SpringBootTest
public class DistributedLockerFactoryTest {

    @BeforeEach
    public void setUp() {
        DistributedLockerFactory.cleanLocker();
    }

    @Test
    public void testGetDistributedLockerNotSupport() {
        DistributedLocker es = DistributedLockerFactory.getDistributedLocker("es");
        Assertions.assertEquals(es.getClass(), DefaultDistributedLocker.class);
    }

    @Test
    public void testGetDistributedLocker() {
        DistributedLocker redis = DistributedLockerFactory.getDistributedLocker("redis");
        Assertions.assertEquals(redis.getClass(), RedisDistributedLocker.class);
    }

    @AfterEach
    public void tearDown() {
        DistributedLockerFactory.cleanLocker();
    }
}
