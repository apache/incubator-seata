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
import org.apache.seata.common.Constants;
import org.apache.seata.common.exception.StoreException;
import org.apache.seata.common.holder.ObjectHolder;
import org.apache.seata.common.loader.EnhancedServiceNotFoundException;
import org.apache.seata.common.store.LockMode;
import org.apache.seata.config.ConfigurationCache;
import org.apache.seata.core.lock.Locker;
import org.apache.seata.server.session.BranchSession;
import org.apache.seata.server.storage.db.lock.DataBaseLockManager;
import org.apache.seata.server.storage.file.lock.FileLockManager;
import org.apache.seata.server.storage.file.spi.FileLockStore;
import org.apache.seata.server.storage.raft.lock.RaftLockManager;
import org.apache.seata.server.storage.redis.lock.RedisLockManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.lang.reflect.Field;
import java.util.Map;

import static org.mockito.Mockito.mock;

class LockerManagerFactoryTest {

    private Object originalEnvironment;

    @BeforeEach
    void beforeEach() {
        originalEnvironment = ObjectHolder.INSTANCE.getObject(Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT);
        ObjectHolder.INSTANCE.setObject(Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT, new MockEnvironment());
        clearConfig();
    }

    @AfterEach
    void afterEach() throws Exception {
        clearConfig();
        restoreEnvironment();
    }

    @Test
    void testUntypedFileModeAccessFailsBeforeRuntimeInstallation() {
        StoreException exception = Assertions.assertThrows(StoreException.class, LockerManagerFactory::getLockManager);

        Assertions.assertTrue(exception.getMessage().contains("FileStoreRuntime"));
    }

    @Test
    void testTypedInitInjectsExactFileLockStore() throws Exception {
        FileLockStore lockStore = mock(FileLockStore.class);
        Locker locker = mock(Locker.class);
        BranchSession branchSession = new BranchSession();
        org.mockito.Mockito.when(lockStore.getLocker(branchSession)).thenReturn(locker);

        LockerManagerFactory.init(LockMode.FILE, new Class<?>[] {FileLockStore.class}, new Object[] {lockStore});

        FileLockManager lockManager = (FileLockManager) LockerManagerFactory.getLockManager();
        Assertions.assertSame(locker, lockManager.getLocker(branchSession));
        lockManager.releaseLock(branchSession);
        org.mockito.Mockito.verify(lockStore).releaseBranchLock(branchSession);
    }

    @Test
    void testUntypedInitIsNoopAfterTypedFileModeInstallation() {
        FileLockStore lockStore = mock(FileLockStore.class);
        LockerManagerFactory.init(LockMode.FILE, new Class<?>[] {FileLockStore.class}, new Object[] {lockStore});
        LockManager installed = LockerManagerFactory.getLockManager();

        LockerManagerFactory.init();

        Assertions.assertSame(installed, LockerManagerFactory.getLockManager());
    }

    @Test
    void testTypedFileModeInitRejectsExistingManager() throws Exception {
        FileLockStore originalStore = mock(FileLockStore.class);
        FileLockStore replacementStore = mock(FileLockStore.class);
        LockerManagerFactory.init(LockMode.FILE, new Class<?>[] {FileLockStore.class}, new Object[] {originalStore});
        LockManager installed = LockerManagerFactory.getLockManager();

        StoreException thrown = Assertions.assertThrows(
                StoreException.class,
                () -> LockerManagerFactory.init(
                        LockMode.FILE, new Class<?>[] {FileLockStore.class}, new Object[] {replacementStore}));

        Assertions.assertTrue(thrown.getMessage().contains("already installed"));
        Assertions.assertSame(installed, lockManagerField().get(null));
    }

    @Test
    void testExplicitDbRedisAndRaftModesRemainLoaderBacked() {
        Assertions.assertTrue(LockerManagerFactory.init(LockMode.DB));
        Assertions.assertFalse(LockerManagerFactory.init(LockMode.DB));
        Assertions.assertInstanceOf(DataBaseLockManager.class, LockerManagerFactory.getLockManager());
        LockerManagerFactory.destroy();
        LockerManagerFactory.init(LockMode.REDIS);
        Assertions.assertInstanceOf(RedisLockManager.class, LockerManagerFactory.getLockManager());
        LockerManagerFactory.destroy();
        LockerManagerFactory.init(LockMode.RAFT);
        Assertions.assertInstanceOf(RaftLockManager.class, LockerManagerFactory.getLockManager());
    }

    @Test
    void testTypedInitDoesNotPublishManagerForUnknownConstructor() throws Exception {
        Assertions.assertThrows(
                EnhancedServiceNotFoundException.class,
                () -> LockerManagerFactory.init(
                        LockMode.FILE, new Class<?>[] {String.class}, new Object[] {"not-a-store"}));
        Assertions.assertNull(lockManagerField().get(null));
    }

    @Test
    void testTypedInitRejectsNullArgumentTypesWithoutPublication() throws Exception {
        assertTypedInitRejected(null, new Object[] {mock(FileLockStore.class)});
    }

    @Test
    void testTypedInitRejectsNullArgumentsWithoutPublication() throws Exception {
        assertTypedInitRejected(new Class<?>[] {FileLockStore.class}, null);
    }

    @Test
    void testTypedInitRejectsUnequalArgumentMetadataWithoutPublication() throws Exception {
        assertTypedInitRejected(new Class<?>[] {FileLockStore.class}, new Object[0]);
    }

    private static void assertTypedInitRejected(Class<?>[] argumentTypes, Object[] arguments) throws Exception {
        Field field = lockManagerField();
        Assertions.assertAll(
                () -> Assertions.assertThrows(
                        IllegalArgumentException.class,
                        () -> LockerManagerFactory.init(LockMode.FILE, argumentTypes, arguments)),
                () -> Assertions.assertNull(field.get(null)));
    }

    private static Field lockManagerField() throws NoSuchFieldException {
        Field field = LockerManagerFactory.class.getDeclaredField("LOCK_MANAGER");
        field.setAccessible(true);
        return field;
    }

    private void clearConfig() {
        LockerManagerFactory.destroy();
        System.clearProperty(ConfigurationKeys.STORE_LOCK_MODE);
        System.clearProperty(ConfigurationKeys.STORE_FILE_ENGINE);
        ConfigurationCache.clear();
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
}
