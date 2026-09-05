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

import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.common.Constants;
import org.apache.seata.common.exception.StoreException;
import org.apache.seata.common.holder.ObjectHolder;
import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.common.store.LockMode;
import org.apache.seata.common.store.SessionMode;
import org.apache.seata.config.ConfigurationCache;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.server.lock.LockManager;
import org.apache.seata.server.lock.LockerManagerFactory;
import org.apache.seata.server.storage.file.FileStoreProviderFactory;
import org.apache.seata.server.storage.file.session.FileSessionManager;
import org.apache.seata.server.storage.file.spi.FileLockStore;
import org.apache.seata.server.storage.file.spi.FileStoreProvider;
import org.apache.seata.server.storage.file.spi.FileStoreRuntime;
import org.apache.seata.server.storage.file.spi.SessionHolderFaultFileStoreProvider;
import org.apache.seata.server.storage.file.spi.SessionHolderFaultFileStoreProvider.FailurePoint;
import org.apache.seata.server.store.StoreConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Map;

class SessionHolderFileStoreRuntimeTest {

    @TempDir
    Path tempDir;

    private Object originalEnvironment;
    private LockMode originalLockMode;

    @BeforeEach
    void setUp() throws Exception {
        originalLockMode = (LockMode) ReflectionTestUtils.getField(StoreConfig.class, "lockMode");
        ReflectionTestUtils.setField(StoreConfig.class, "lockMode", null);
        originalEnvironment = ObjectHolder.INSTANCE.getObject(Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT);
        ObjectHolder.INSTANCE.setObject(Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT, new MockEnvironment());
        System.setProperty(
                ConfigurationKeys.STORE_FILE_DIR, tempDir.resolve("file").toString());
        System.setProperty(ConfigurationKeys.STORE_FILE_ENGINE, "session-holder-fault");
        System.setProperty(ConfigurationKeys.STORE_LOCK_MODE, LockMode.FILE.getName());
        ConfigurationCache.clear();
        clearProviderCaches();
        SessionHolderFaultFileStoreProvider.reset();
    }

    @AfterEach
    void tearDown() throws Exception {
        SessionHolderFaultFileStoreProvider.reset();
        SessionHolder.destroy();
        LockerManagerFactory.destroy();
        System.clearProperty(ConfigurationKeys.STORE_FILE_DIR);
        System.clearProperty(ConfigurationKeys.STORE_FILE_ENGINE);
        System.clearProperty(ConfigurationKeys.STORE_LOCK_MODE);
        ReflectionTestUtils.setField(StoreConfig.class, "lockMode", originalLockMode);
        ConfigurationCache.clear();
        clearProviderCaches();
        restoreEnvironment();
    }

    @Test
    void testRepeatedInitializationPreservesRunningRuntimeAndItsResources() throws Exception {
        FileSessionManager sessions = new FileSessionManager("root.data");
        SessionHolderFaultFileStoreProvider.configure(
                FailurePoint.NONE, null, null, sessions, Mockito.mock(FileLockStore.class));
        SessionHolder.init(SessionMode.FILE);
        FileStoreRuntime runtime = (FileStoreRuntime) rawField("FILE_STORE_RUNTIME");
        LockManager lockManager = LockerManagerFactory.getLockManager();
        Object mappingManager = rawField("ROOT_VGROUP_MAPPING_MANAGER");
        Object distributedLocker = rawField("DISTRIBUTED_LOCKER");
        GlobalSession session = new GlobalSession("app", "group", "transaction", 60000);
        sessions.addGlobalSession(session);
        try {
            Assertions.assertThrows(StoreException.class, () -> SessionHolder.init(SessionMode.FILE));

            Assertions.assertAll(
                    () -> Assertions.assertSame(runtime, rawField("FILE_STORE_RUNTIME")),
                    () -> Assertions.assertSame(sessions, SessionHolder.getRootSessionManager()),
                    () -> Assertions.assertSame(session, SessionHolder.findGlobalSession(session.getXid())),
                    () -> Assertions.assertSame(lockManager, LockerManagerFactory.getLockManager()),
                    () -> Assertions.assertSame(mappingManager, rawField("ROOT_VGROUP_MAPPING_MANAGER")),
                    () -> Assertions.assertSame(distributedLocker, rawField("DISTRIBUTED_LOCKER")),
                    () -> Assertions.assertEquals(0, SessionHolderFaultFileStoreProvider.closeCount()));

            SessionHolder.destroy();
            Assertions.assertEquals(1, SessionHolderFaultFileStoreProvider.closeCount());
            assertFileModeReferencesCleared();
        } finally {
            runtime.close();
        }
    }

    @Test
    void testRecoveryConsumerRemovesTerminalSession() throws Exception {
        FileSessionManager sessions = new FileSessionManager("root.data");
        GlobalSession terminal = new GlobalSession("app", "group", "transaction", 60000);
        terminal.setStatus(GlobalStatus.Finished);
        sessions.addGlobalSession(terminal);
        SessionHolderFaultFileStoreProvider.configure(
                FailurePoint.NONE, null, null, sessions, Mockito.mock(FileLockStore.class));

        SessionHolder.init(SessionMode.FILE);

        Assertions.assertNull(SessionHolder.findGlobalSession(terminal.getXid()));
    }

    @Test
    void testRecoveryConsumerFailureRollsBackRuntime() throws Exception {
        TransactionException recoveryFailure = new TransactionException("remove recovered session failed");
        FileSessionManager sessions = new FileSessionManager("root.data") {
            @Override
            public void removeGlobalSession(GlobalSession session) throws TransactionException {
                throw recoveryFailure;
            }
        };
        GlobalSession terminal = new GlobalSession("app", "group", "transaction", 60000);
        terminal.setStatus(GlobalStatus.Finished);
        sessions.addGlobalSession(terminal);
        SessionHolderFaultFileStoreProvider.configure(
                FailurePoint.NONE, null, null, sessions, Mockito.mock(FileLockStore.class));

        StoreException thrown =
                Assertions.assertThrows(StoreException.class, () -> SessionHolder.init(SessionMode.FILE));

        Assertions.assertSame(recoveryFailure, thrown.getCause());
        Assertions.assertEquals(1, SessionHolderFaultFileStoreProvider.closeCount());
        assertFileModeReferencesCleared();
    }

    @Test
    void testProviderOpenFailureLeavesNoPublishedReferences() throws Exception {
        RuntimeException startupFailure = new RuntimeException("open");
        configure(FailurePoint.OPEN, startupFailure, null);

        Assertions.assertSame(
                startupFailure,
                Assertions.assertThrows(RuntimeException.class, () -> SessionHolder.init(SessionMode.FILE)));

        assertFileModeReferencesCleared();
        Assertions.assertEquals(0, SessionHolderFaultFileStoreProvider.closeCount());
    }

    @Test
    void testLockStoreFailureClosesRuntimeAndClearsReferences() throws Exception {
        RuntimeException startupFailure = new RuntimeException("lock store");
        configure(FailurePoint.LOCK_STORE, startupFailure, null);

        Assertions.assertSame(
                startupFailure,
                Assertions.assertThrows(RuntimeException.class, () -> SessionHolder.init(SessionMode.FILE)));

        assertFileModeReferencesCleared();
        Assertions.assertEquals(1, SessionHolderFaultFileStoreProvider.closeCount());
    }

    @Test
    void testLockInstallFailureClosesRuntimeAndClearsReferences() throws Exception {
        RuntimeException startupFailure = new RuntimeException("lock install");
        configure(FailurePoint.NONE, startupFailure, null);

        try (MockedStatic<LockerManagerFactory> factory =
                Mockito.mockStatic(LockerManagerFactory.class, Mockito.CALLS_REAL_METHODS)) {
            factory.when(() -> LockerManagerFactory.init(
                            Mockito.eq(LockMode.FILE), Mockito.any(Class[].class), Mockito.any(Object[].class)))
                    .thenThrow(startupFailure);

            Assertions.assertSame(
                    startupFailure,
                    Assertions.assertThrows(RuntimeException.class, () -> SessionHolder.init(SessionMode.FILE)));
        }

        assertFileModeReferencesCleared();
        Assertions.assertEquals(1, SessionHolderFaultFileStoreProvider.closeCount());
    }

    @Test
    void testExistingFileLockManagerRollsBackNewRuntimeWithoutPublication() throws Exception {
        FileLockStore originalStore = Mockito.mock(FileLockStore.class);
        FileLockStore replacementStore = Mockito.mock(FileLockStore.class);
        SessionManager replacementSessionManager = Mockito.mock(SessionManager.class);
        LockerManagerFactory.init(LockMode.FILE, new Class<?>[] {FileLockStore.class}, new Object[] {originalStore});
        LockManager originalManager = LockerManagerFactory.getLockManager();
        SessionHolderFaultFileStoreProvider.configure(
                FailurePoint.NONE, null, null, replacementSessionManager, replacementStore);

        StoreException thrown =
                Assertions.assertThrows(StoreException.class, () -> SessionHolder.init(SessionMode.FILE));

        Assertions.assertTrue(thrown.getMessage().contains("already installed"));
        Mockito.verifyNoInteractions(replacementSessionManager);
        assertSessionReferencesCleared();
        Assertions.assertSame(originalManager, lockManagerField().get(null));
        Assertions.assertEquals(1, SessionHolderFaultFileStoreProvider.closeCount());
    }

    @Test
    void testFileSessionWithDbLockFailsBeforeOpeningFileStoreRuntime() throws Exception {
        System.setProperty(ConfigurationKeys.STORE_LOCK_MODE, LockMode.DB.getName());
        ConfigurationCache.clear();
        configure(FailurePoint.NONE, null, null);

        StoreException thrown =
                Assertions.assertThrows(StoreException.class, () -> SessionHolder.init(SessionMode.FILE));

        Assertions.assertTrue(thrown.getMessage().contains("must use file lock mode"));

        Assertions.assertNull(SessionHolderFaultFileStoreProvider.lastContext());
    }

    @Test
    void testDbSessionWithFileLockFailsBeforeOpeningFileStoreRuntime() throws Exception {
        System.setProperty(ConfigurationKeys.STORE_LOCK_MODE, LockMode.FILE.getName());
        ConfigurationCache.clear();
        configure(FailurePoint.NONE, null, null);

        StoreException thrown = Assertions.assertThrows(StoreException.class, () -> SessionHolder.init(SessionMode.DB));

        Assertions.assertTrue(thrown.getMessage().contains("must use file session mode"));

        Assertions.assertNull(SessionHolderFaultFileStoreProvider.lastContext());
    }

    @Test
    void testRecoveryFailurePreservesPrimaryAndSuppressesCloseFailure() throws Exception {
        RuntimeException startupFailure = new RuntimeException("recovery");
        RuntimeException closeFailure = new RuntimeException("close");
        configure(FailurePoint.RECOVERY, startupFailure, closeFailure);

        RuntimeException thrown =
                Assertions.assertThrows(RuntimeException.class, () -> SessionHolder.init(SessionMode.FILE));

        Assertions.assertSame(startupFailure, thrown);
        Assertions.assertArrayEquals(new Throwable[] {closeFailure}, thrown.getSuppressed());
        assertFileModeReferencesCleared();
        Assertions.assertEquals(1, SessionHolderFaultFileStoreProvider.closeCount());
    }

    @Test
    void testBackgroundStartFailureClosesRuntimeAndClearsReferences() throws Exception {
        RuntimeException startupFailure = new RuntimeException("background");
        configure(FailurePoint.BACKGROUND, startupFailure, null);

        Assertions.assertSame(
                startupFailure,
                Assertions.assertThrows(RuntimeException.class, () -> SessionHolder.init(SessionMode.FILE)));

        assertFileModeReferencesCleared();
        Assertions.assertEquals(1, SessionHolderFaultFileStoreProvider.closeCount());
    }

    @Test
    void testDestroyClearsReferencesAndRethrowsRuntimeCloseFailure() throws Exception {
        RuntimeException closeFailure = new RuntimeException("close");
        configure(FailurePoint.NONE, null, closeFailure);
        SessionHolder.init(SessionMode.FILE);

        RuntimeException thrown = Assertions.assertThrows(RuntimeException.class, SessionHolder::destroy);

        Assertions.assertSame(closeFailure, thrown);
        assertFileModeReferencesCleared();
        Assertions.assertEquals(1, SessionHolderFaultFileStoreProvider.closeCount());
        Assertions.assertDoesNotThrow(SessionHolder::destroy);
    }

    private void configure(FailurePoint point, RuntimeException startupFailure, RuntimeException closeFailure) {
        SessionHolderFaultFileStoreProvider.configure(
                point,
                startupFailure,
                closeFailure,
                Mockito.mock(SessionManager.class),
                Mockito.mock(FileLockStore.class));
    }

    private void assertFileModeReferencesCleared() throws Exception {
        Assertions.assertAll(
                () -> Assertions.assertNull(rawField("FILE_STORE_RUNTIME")),
                () -> Assertions.assertNull(rawField("ROOT_SESSION_MANAGER")),
                () -> Assertions.assertNull(rawField("ROOT_VGROUP_MAPPING_MANAGER")),
                () -> Assertions.assertNull(rawField("DISTRIBUTED_LOCKER")),
                () -> Assertions.assertNull(rawField("SESSION_MANAGER_MAP")),
                () -> Assertions.assertNull(lockManagerField().get(null)));
    }

    private void assertSessionReferencesCleared() throws Exception {
        Assertions.assertAll(
                () -> Assertions.assertNull(rawField("FILE_STORE_RUNTIME")),
                () -> Assertions.assertNull(rawField("ROOT_SESSION_MANAGER")),
                () -> Assertions.assertNull(rawField("ROOT_VGROUP_MAPPING_MANAGER")),
                () -> Assertions.assertNull(rawField("DISTRIBUTED_LOCKER")),
                () -> Assertions.assertNull(rawField("SESSION_MANAGER_MAP")));
    }

    private Object rawField(String name) throws Exception {
        Field field = SessionHolder.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(null);
    }

    private Field lockManagerField() throws Exception {
        Field field = LockerManagerFactory.class.getDeclaredField("LOCK_MANAGER");
        field.setAccessible(true);
        return field;
    }

    @SuppressWarnings("unchecked")
    private void clearProviderCaches() throws Exception {
        Field field = FileStoreProviderFactory.class.getDeclaredField("INSTANCES");
        field.setAccessible(true);
        ((Map<String, FileStoreProvider>) field.get(null)).clear();
        EnhancedServiceLoader.unload(FileStoreProvider.class);
        EnhancedServiceLoader.unload(LockManager.class);
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
