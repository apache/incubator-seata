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
package org.apache.seata.server.session;

import org.apache.seata.common.Constants;
import org.apache.seata.common.XID;
import org.apache.seata.common.holder.ObjectHolder;
import org.apache.seata.common.store.SessionMode;
import org.apache.seata.config.ConfigurationCache;
import org.apache.seata.core.constants.ConfigurationKeys;
import org.apache.seata.server.lock.LockerManagerFactory;
import org.apache.seata.server.storage.file.lock.DefaultFileLockStore;
import org.apache.seata.server.storage.file.lock.FileLockManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.env.MockEnvironment;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Map;

import static org.apache.seata.common.Constants.ASYNC_COMMITTING;
import static org.apache.seata.common.Constants.RETRY_COMMITTING;
import static org.apache.seata.common.Constants.RETRY_ROLLBACKING;
import static org.apache.seata.common.Constants.TX_TIMEOUT_CHECK;
import static org.apache.seata.common.Constants.UNDOLOG_DELETE;
import static org.apache.seata.server.session.SessionHolder.ROOT_SESSION_MANAGER_NAME;

/**
 * The type Session holder test.
 *
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SessionHolderTest {
    @TempDir
    Path tempDir;

    private String pathname;
    private Object originalEnvironment;

    @BeforeEach
    public void before() {
        originalEnvironment = ObjectHolder.INSTANCE.getObject(Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT);
        ObjectHolder.INSTANCE.setObject(Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT, new MockEnvironment());
        System.setProperty(
                ConfigurationKeys.STORE_FILE_DIR, tempDir.resolve("file").toString());
        ConfigurationCache.clear();
        String sessionStorePath =
                tempDir.resolve("file").resolve(String.valueOf(XID.getPort())).toString();
        // delete file previously created
        pathname = sessionStorePath + File.separator + ROOT_SESSION_MANAGER_NAME;
        // SessionHolder.init(StoreMode.REDIS.getName());
    }

    @Test
    @Order(1)
    public void testInit() throws Exception {
        File rootSessionFile = new File(pathname);
        if (rootSessionFile.exists()) {
            rootSessionFile.delete();
        }
        SessionHolder.init(SessionMode.FILE);
        try {
            final File actual = new File(pathname);
            Assertions.assertTrue(actual.exists());
            Assertions.assertTrue(actual.isFile());
            Assertions.assertInstanceOf(FileLockManager.class, LockerManagerFactory.getLockManager());
            Assertions.assertInstanceOf(
                    DefaultFileLockStore.class, lockStore((FileLockManager) LockerManagerFactory.getLockManager()));
        } finally {
            SessionHolder.destroy();
        }
    }

    @AfterEach
    public void after() throws Exception {
        SessionHolder.destroy();
        LockerManagerFactory.destroy();
        System.clearProperty(ConfigurationKeys.STORE_FILE_DIR);
        System.clearProperty(ConfigurationKeys.STORE_FILE_ENGINE);
        ConfigurationCache.clear();
        final File actual = new File(pathname);
        if (actual.exists()) {
            actual.delete();
        }
        restoreEnvironment();
    }

    private static Object lockStore(FileLockManager lockManager) throws ReflectiveOperationException {
        Field field = FileLockManager.class.getDeclaredField("lockStore");
        field.setAccessible(true);
        return field.get(lockManager);
    }

    @SuppressWarnings("unchecked")
    private void restoreEnvironment() throws Exception {
        Field field = ObjectHolder.class.getDeclaredField("OBJECT_MAP");
        field.setAccessible(true);
        Map<String, Object> objectMap = (Map<String, Object>) field.get(ObjectHolder.INSTANCE);
        if (originalEnvironment == null) {
            objectMap.remove(Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT);
        } else {
            objectMap.put(Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT, originalEnvironment);
        }
    }

    //    @Test
    @Order(2)
    public void test_retryRollbackingLock() {
        Assertions.assertTrue(SessionHolder.acquireDistributedLock(RETRY_ROLLBACKING));
    }

    //    @Test
    @Order(3)
    public void test_unRetryRollbackingLock() {
        Assertions.assertTrue(SessionHolder.releaseDistributedLock(RETRY_ROLLBACKING));
    }

    //    @Test
    @Order(4)
    public void test_retryCommittingLock() {
        Assertions.assertTrue(SessionHolder.acquireDistributedLock(RETRY_COMMITTING));
    }

    //    @Test
    @Order(5)
    public void test_unRetryCommittingLock() {
        Assertions.assertTrue(SessionHolder.releaseDistributedLock(RETRY_COMMITTING));
    }

    //    @Test
    @Order(6)
    public void test_asyncCommittingLock() {
        Assertions.assertTrue(SessionHolder.acquireDistributedLock(ASYNC_COMMITTING));
    }

    //    @Test
    @Order(7)
    public void test_unAsyncCommittingLock() {
        Assertions.assertTrue(SessionHolder.releaseDistributedLock(ASYNC_COMMITTING));
    }

    //    @Test
    @Order(8)
    public void test_txTimeoutCheckLock() {
        Assertions.assertTrue(SessionHolder.acquireDistributedLock(TX_TIMEOUT_CHECK));
    }

    //    @Test
    @Order(9)
    public void test_unTxTimeoutCheckLock() {
        Assertions.assertTrue(SessionHolder.releaseDistributedLock(TX_TIMEOUT_CHECK));
    }

    //    @Test
    @Order(10)
    public void test_undoLogDeleteLock() {
        Assertions.assertTrue(SessionHolder.acquireDistributedLock(UNDOLOG_DELETE));
    }

    //    @Test
    @Order(11)
    public void test_unUndoLogDeleteLock() {
        Assertions.assertTrue(SessionHolder.releaseDistributedLock(UNDOLOG_DELETE));
    }
}
