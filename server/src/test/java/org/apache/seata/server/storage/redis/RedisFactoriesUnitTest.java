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
package org.apache.seata.server.storage.redis;

import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.config.ConfigurationCache;
import org.apache.seata.core.lock.Locker;
import org.apache.seata.server.BaseSpringBootTest;
import org.apache.seata.server.lock.AbstractLockManager;
import org.apache.seata.server.session.*;
import org.apache.seata.server.storage.db.lock.DataBaseLockManager;
import org.apache.seata.server.storage.redis.lock.*;
import org.apache.seata.server.storage.redis.store.*;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RedisFactoriesUnitTest extends BaseSpringBootTest {
    @Test
    void modeSelectsAndCachesPipelineOrLuaImplementations() {
        Object oldLocker = ReflectionTestUtils.getField(RedisLockerFactory.class, "locker"),
                oldStore = ReflectionTestUtils.getField(RedisTransactionStoreManagerFactory.class, "instance");
        String oldMode = System.getProperty(ConfigurationKeys.STORE_REDIS_TYPE);
        try (MockedConstruction<RedisLocker> lockers = mockConstruction(RedisLocker.class);
                MockedConstruction<RedisLuaLocker> luaLockers = mockConstruction(RedisLuaLocker.class);
                MockedConstruction<RedisTransactionStoreManager> stores =
                        mockConstruction(RedisTransactionStoreManager.class);
                MockedConstruction<RedisLuaTransactionStoreManager> luaStores =
                        mockConstruction(RedisLuaTransactionStoreManager.class)) {
            for (String mode : Arrays.asList("pipeline", "lua")) {
                ReflectionTestUtils.setField(RedisLockerFactory.class, "locker", null);
                ReflectionTestUtils.setField(RedisTransactionStoreManagerFactory.class, "instance", null);
                System.setProperty(ConfigurationKeys.STORE_REDIS_TYPE, mode);
                ConfigurationCache.clear();
                Locker locker = RedisLockerFactory.getLocker();
                RedisTransactionStoreManager store = RedisTransactionStoreManagerFactory.getInstance();
                assertSame(locker, RedisLockerFactory.getLocker());
                assertSame(store, RedisTransactionStoreManagerFactory.getInstance());
                if ("pipeline".equals(mode)) {
                    assertSame(lockers.constructed().get(0), locker);
                    assertSame(stores.constructed().get(0), store);
                } else {
                    assertSame(luaLockers.constructed().get(0), locker);
                    assertSame(luaStores.constructed().get(0), store);
                }
                RedisLockManager manager = new RedisLockManager();
                manager.init();
                assertSame(locker, manager.getLocker(new BranchSession()));
            }
        } finally {
            ReflectionTestUtils.setField(RedisLockerFactory.class, "locker", oldLocker);
            ReflectionTestUtils.setField(RedisTransactionStoreManagerFactory.class, "instance", oldStore);
            if (oldMode == null) {
                System.clearProperty(ConfigurationKeys.STORE_REDIS_TYPE);
            } else {
                System.setProperty(ConfigurationKeys.STORE_REDIS_TYPE, oldMode);
            }
            ConfigurationCache.clear();
        }
    }

    @Test
    void lockManagersReleaseOnlyRequestedOwnerAndContainBackendFailure() throws Exception {
        for (AbstractLockManager manager : Arrays.asList(new RedisLockManager(), new DataBaseLockManager())) {
            Locker locker = mock(Locker.class);
            ReflectionTestUtils.setField(manager, "locker", locker);
            BranchSession branch = new BranchSession();
            branch.setXid("xid");
            branch.setBranchId(7L);
            GlobalSession global = new GlobalSession();
            global.setXid("xid");
            when(locker.releaseLock("xid", 7L)).thenReturn(true);
            when(locker.releaseLock("xid")).thenReturn(true);
            assertTrue(manager.releaseLock(branch));
            assertTrue(manager.releaseGlobalSessionLock(global));
            verify(locker).releaseLock("xid", 7L);
            verify(locker).releaseLock("xid");
            doThrow(new IllegalStateException("offline")).when(locker).releaseLock("xid", 7L);
            doThrow(new IllegalStateException("offline")).when(locker).releaseLock("xid");
            assertFalse(manager.releaseLock(branch));
            assertFalse(manager.releaseGlobalSessionLock(global));
        }
    }
}
