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
package org.apache.seata.server.store;

import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.common.exception.StoreException;
import org.apache.seata.common.store.LockMode;
import org.apache.seata.config.ConfigurationCache;
import org.apache.seata.server.BaseSpringBootTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

class StoreConfigTest extends BaseSpringBootTest {

    @BeforeEach
    void beforeEach() throws Exception {
        clearConfig();
    }

    @AfterEach
    void afterEach() throws Exception {
        clearConfig();
    }

    @Test
    void testDefaultFileEngine() {
        Assertions.assertEquals(FileStoreEngine.FILE, StoreConfig.getFileEngine());
    }

    @Test
    void testConfiguredFileEngine() {
        System.setProperty(ConfigurationKeys.STORE_FILE_ENGINE, "rocksdb");
        Assertions.assertEquals(FileStoreEngine.ROCKSDB, StoreConfig.getFileEngine());

        System.setProperty(ConfigurationKeys.STORE_FILE_ENGINE, "file");
        ConfigurationCache.clear();
        Assertions.assertEquals(FileStoreEngine.FILE, StoreConfig.getFileEngine());
    }

    @Test
    void testInvalidFileEngine() {
        System.setProperty(ConfigurationKeys.STORE_FILE_ENGINE, "unknown");
        Assertions.assertThrows(StoreException.class, StoreConfig::getFileEngine);
    }

    @Test
    void testDefaultEffectiveLockMode() {
        Assertions.assertEquals(LockMode.FILE, StoreConfig.getEffectiveLockMode());
    }

    @Test
    void testRocksDBEffectiveLockMode() {
        System.setProperty(ConfigurationKeys.STORE_FILE_ENGINE, "rocksdb");
        Assertions.assertEquals(LockMode.ROCKSDB, StoreConfig.getEffectiveLockMode());
    }

    @Test
    void testRocksDBEffectiveLockModeAllowsExplicitRocksDB() {
        System.setProperty(ConfigurationKeys.STORE_FILE_ENGINE, "rocksdb");
        System.setProperty(ConfigurationKeys.STORE_LOCK_MODE, "rocksdb");
        Assertions.assertEquals(LockMode.ROCKSDB, StoreConfig.getEffectiveLockMode());
    }

    @Test
    void testRocksDBEffectiveLockModeRejectsMixedLockMode() {
        System.setProperty(ConfigurationKeys.STORE_FILE_ENGINE, "rocksdb");
        System.setProperty(ConfigurationKeys.STORE_LOCK_MODE, "file");
        Assertions.assertThrows(StoreException.class, StoreConfig::getEffectiveLockMode);
    }

    @Test
    void testRocksDBLockModeRequiresRocksDBFileEngine() {
        System.setProperty(ConfigurationKeys.STORE_LOCK_MODE, "rocksdb");
        Assertions.assertThrows(StoreException.class, StoreConfig::getEffectiveLockMode);
    }

    private void clearConfig() throws Exception {
        System.clearProperty(ConfigurationKeys.STORE_MODE);
        System.clearProperty(ConfigurationKeys.STORE_LOCK_MODE);
        System.clearProperty(ConfigurationKeys.STORE_FILE_ENGINE);
        ConfigurationCache.clear();
        resetStoreConfig("storeMode");
        resetStoreConfig("sessionMode");
        resetStoreConfig("lockMode");
    }

    private void resetStoreConfig(String fieldName) throws Exception {
        Field field = StoreConfig.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(null, null);
    }
}
