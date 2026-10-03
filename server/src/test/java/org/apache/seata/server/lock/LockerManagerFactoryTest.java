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
package org.apache.seata.server.lock;

import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.common.exception.StoreException;
import org.apache.seata.config.ConfigurationCache;
import org.apache.seata.server.BaseSpringBootTest;
import org.apache.seata.server.storage.file.lock.FileLockManager;
import org.apache.seata.server.storage.rocksdb.RocksDBStoreEngineFactory;
import org.apache.seata.server.storage.rocksdb.lock.RocksDBLockManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.annotation.DirtiesContext;

import java.nio.file.Path;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class LockerManagerFactoryTest extends BaseSpringBootTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void beforeEach() {
        clearConfig();
    }

    @AfterEach
    void afterEach() {
        clearConfig();
    }

    @Test
    void testDefaultFileLockManager() {
        LockerManagerFactory.init();
        Assertions.assertInstanceOf(FileLockManager.class, LockerManagerFactory.getLockManager());
    }

    @Test
    void testRocksDBEffectiveLockManager() {
        System.setProperty(ConfigurationKeys.STORE_FILE_ENGINE, "rocksdb");
        LockerManagerFactory.init();
        Assertions.assertInstanceOf(RocksDBLockManager.class, LockerManagerFactory.getLockManager());
    }

    @Test
    void testRocksDBLockManagerRecreatedAfterStoreEngineDestroy() {
        System.setProperty(ConfigurationKeys.STORE_FILE_ENGINE, "rocksdb");
        System.setProperty(
                ConfigurationKeys.STORE_FILE_ROCKSDB_DIR,
                tempDir.resolve("rocksdb").toString());
        ConfigurationCache.clear();
        LockerManagerFactory.init();
        LockManager firstLockManager = LockerManagerFactory.getLockManager();

        RocksDBStoreEngineFactory.destroy();
        LockerManagerFactory.destroy();
        LockerManagerFactory.init();

        Assertions.assertNotSame(firstLockManager, LockerManagerFactory.getLockManager());
    }

    @Test
    void testRocksDBEffectiveLockManagerRejectsMixedLockMode() {
        System.setProperty(ConfigurationKeys.STORE_FILE_ENGINE, "rocksdb");
        System.setProperty(ConfigurationKeys.STORE_LOCK_MODE, "file");
        Assertions.assertThrows(StoreException.class, LockerManagerFactory::init);
    }

    private void clearConfig() {
        LockerManagerFactory.destroy();
        RocksDBStoreEngineFactory.destroy();
        System.clearProperty(ConfigurationKeys.STORE_LOCK_MODE);
        System.clearProperty(ConfigurationKeys.STORE_FILE_ENGINE);
        System.clearProperty(ConfigurationKeys.STORE_FILE_ROCKSDB_DIR);
        ConfigurationCache.clear();
    }
}
