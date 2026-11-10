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
import org.apache.seata.common.loader.LoadLevel;
import org.apache.seata.common.loader.Scope;
import org.apache.seata.common.store.LockMode;
import org.apache.seata.common.store.SessionMode;
import org.apache.seata.config.ConfigurationCache;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.lock.Locker;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.server.lock.LockManager;
import org.apache.seata.server.lock.LockerManagerFactory;
import org.apache.seata.server.lock.distributed.DistributedLockerFactory;
import org.apache.seata.server.storage.file.FileStoreProviderFactory;
import org.apache.seata.server.storage.file.lock.FileLockManager;
import org.apache.seata.server.storage.file.session.FileSessionManager;
import org.apache.seata.server.storage.file.spi.FileLockStore;
import org.apache.seata.server.storage.file.spi.FileStoreProvider;
import org.apache.seata.server.storage.file.spi.FileStoreRuntime;
import org.apache.seata.server.storage.file.spi.SessionHolderFaultFileStoreProvider;
import org.apache.seata.server.storage.file.spi.SessionHolderFaultFileStoreProvider.FailurePoint;
import org.apache.seata.server.store.StoreConfig;
import org.apache.seata.server.store.VGroupMappingStoreManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

class SessionHolderFileStoreRuntimeTest {

    @TempDir
    Path tempDir;

    private Object originalEnvironment;
    private LockMode originalLockMode;

    @BeforeEach
    void setUp() throws Exception {
        originalEnvironment = ObjectHolder.INSTANCE.getObject(Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT);
        ObjectHolder.INSTANCE.setObject(Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT, new MockEnvironment());
        originalLockMode = (LockMode) ReflectionTestUtils.getField(StoreConfig.class, "lockMode");
        ReflectionTestUtils.setField(StoreConfig.class, "lockMode", null);
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
    void testFileSessionWithDbLocksUsesLegacyManager() throws Exception {
        System.setProperty(ConfigurationKeys.STORE_LOCK_MODE, LockMode.DB.getName());
        ConfigurationCache.clear();
        configure(FailurePoint.NONE, null, null);
        LockManager dbLocks = Mockito.mock(LockManager.class);
        BranchSession branch = new BranchSession(BranchType.AT);
        Mockito.when(dbLocks.acquireLock(branch, true, false)).thenReturn(true);
        try (MockedStatic<EnhancedServiceLoader> loader =
                Mockito.mockStatic(EnhancedServiceLoader.class, Mockito.CALLS_REAL_METHODS)) {
            loader.when(() -> EnhancedServiceLoader.load(LockManager.class, "db"))
                    .thenReturn(dbLocks);
            SessionHolder.init(SessionMode.FILE);
            Assertions.assertTrue(branch.lock());
            branch.unlock();
            Mockito.verify(dbLocks).releaseLock(branch);
            Assertions.assertSame(dbLocks, LockerManagerFactory.getLockManager());
            SessionHolder.destroy();
            assertFileModeReferencesCleared();
        }
    }

    @Test
    void testDbSessionWithFileLocksDoesNotOpenFileRuntime() throws Exception {
        SessionManager sessions = Mockito.mock(SessionManager.class);
        VGroupMappingStoreManager mappings = Mockito.mock(VGroupMappingStoreManager.class);
        try (MockedStatic<EnhancedServiceLoader> loader =
                        Mockito.mockStatic(EnhancedServiceLoader.class, Mockito.CALLS_REAL_METHODS);
                MockedStatic<DistributedLockerFactory> distributed =
                        Mockito.mockStatic(DistributedLockerFactory.class)) {
            loader.when(() -> EnhancedServiceLoader.load(SessionManager.class, "db"))
                    .thenReturn(sessions);
            loader.when(() -> EnhancedServiceLoader.load(VGroupMappingStoreManager.class, "db"))
                    .thenReturn(mappings);
            SessionHolder.init(SessionMode.DB);
            Assertions.assertSame(sessions, SessionHolder.getRootSessionManager());
            Assertions.assertNull(SessionHolderFaultFileStoreProvider.lastContext());
            LockManager locks = LockerManagerFactory.getLockManager();
            Assertions.assertInstanceOf(FileLockManager.class, locks);
            BranchSession branch = new BranchSession(BranchType.AT);
            branch.setXid("127.0.0.1:8091:901");
            branch.setTransactionId(901);
            branch.setResourceId("mixed-mode");
            branch.setLockKey("t:1");
            Assertions.assertTrue(branch.lock());
            Assertions.assertFalse(locks.isLockable("127.0.0.1:8091:902", "mixed-mode", "t:1"));
            Assertions.assertTrue(branch.unlock());
            Assertions.assertTrue(locks.isLockable("127.0.0.1:8091:902", "mixed-mode", "t:1"));
            SessionHolder.destroy();
            Mockito.verify(sessions).destroy();
            assertFileModeReferencesCleared();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testDbSessionStartupFailureClosesSessionAndRespectsLockOwnership(boolean preinstalled) throws Exception {
        SessionManager sessions = Mockito.mock(SessionManager.class);
        RuntimeException failure = new RuntimeException("vgroup startup");
        RuntimeException closeFailure = new RuntimeException("session close");
        Mockito.doThrow(closeFailure).when(sessions).destroy();
        LockManager existing = preinstalled ? Mockito.mock(LockManager.class) : null;
        lockManagerField().set(null, existing);
        try (MockedStatic<EnhancedServiceLoader> loader =
                        Mockito.mockStatic(EnhancedServiceLoader.class, Mockito.CALLS_REAL_METHODS);
                MockedStatic<DistributedLockerFactory> distributed =
                        Mockito.mockStatic(DistributedLockerFactory.class)) {
            loader.when(() -> EnhancedServiceLoader.load(SessionManager.class, "db"))
                    .thenReturn(sessions);
            loader.when(() -> EnhancedServiceLoader.load(VGroupMappingStoreManager.class, "db"))
                    .thenThrow(failure);

            RuntimeException thrown =
                    Assertions.assertThrows(RuntimeException.class, () -> SessionHolder.init(SessionMode.DB));

            Assertions.assertSame(failure, thrown);
            Assertions.assertArrayEquals(new Throwable[] {closeFailure}, thrown.getSuppressed());
            Mockito.verify(sessions).destroy();
            Assertions.assertSame(existing, lockManagerField().get(null));
            assertSessionReferencesCleared();
            SessionHolder.destroy();
            Mockito.verify(sessions, Mockito.times(1)).destroy();
        }
    }

    @Test
    void testMixedModeFailurePreservesPreinstalledLockManager() throws Exception {
        System.setProperty(ConfigurationKeys.STORE_LOCK_MODE, LockMode.DB.getName());
        ConfigurationCache.clear();
        LockManager existing = Mockito.mock(LockManager.class);
        lockManagerField().set(null, existing);
        RuntimeException failure = new RuntimeException("recovery");
        configure(FailurePoint.RECOVERY, failure, null);

        Assertions.assertSame(
                failure, Assertions.assertThrows(RuntimeException.class, () -> SessionHolder.init(SessionMode.FILE)));

        Assertions.assertSame(existing, lockManagerField().get(null));
        Assertions.assertEquals(1, SessionHolderFaultFileStoreProvider.closeCount());
        assertSessionReferencesCleared();
    }

    @Test
    void testFalseRecoveryLockAbortsStartupAndClosesRuntime() throws Exception {
        FileSessionManager sessions = new FileSessionManager("root.data");
        GlobalSession global = new GlobalSession("app", "group", "tx", 60000);
        global.setStatus(GlobalStatus.Begin);
        BranchSession branch = new BranchSession(BranchType.AT);
        branch.setBranchId(991);
        branch.setXid(global.getXid());
        branch.setTransactionId(global.getTransactionId());
        global.add(branch);
        sessions.addGlobalSession(global);
        FileLockStore store = Mockito.mock(FileLockStore.class);
        Locker locker = Mockito.mock(Locker.class);
        Mockito.when(store.getLocker(branch)).thenReturn(locker);
        branch.setResourceId("recovery-failure");
        branch.setLockKey("t:1");
        SessionHolderFaultFileStoreProvider.configure(FailurePoint.NONE, null, null, sessions, store);

        StoreException thrown =
                Assertions.assertThrows(StoreException.class, () -> SessionHolder.init(SessionMode.FILE));

        Assertions.assertTrue(thrown.getMessage().contains("Could not recover branch lock"));
        Assertions.assertEquals(1, SessionHolderFaultFileStoreProvider.closeCount());
        Mockito.verify(locker).acquireLock(Mockito.anyList(), Mockito.eq(true), Mockito.eq(false));
        assertFileModeReferencesCleared();
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
    void testRepeatedStartupFailureDuringCloseStillUninstallsLockManager() throws Exception {
        RuntimeException failure = new RuntimeException("background start and close");
        configure(FailurePoint.BACKGROUND, failure, failure);

        RuntimeException thrown =
                Assertions.assertThrows(RuntimeException.class, () -> SessionHolder.init(SessionMode.FILE));

        Assertions.assertSame(failure, thrown);
        Assertions.assertEquals(0, thrown.getSuppressed().length);
        Assertions.assertEquals(1, SessionHolderFaultFileStoreProvider.closeCount());
        assertFileModeReferencesCleared();
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

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testDestroyRetainsLockManagerUntilBackgroundStops(boolean failClose) throws Exception {
        RuntimeException closeFailure = failClose ? new RuntimeException("close") : null;
        configure(FailurePoint.NONE, null, closeFailure);
        SessionHolder.init(SessionMode.FILE);
        LockManager installed = LockerManagerFactory.getLockManager();
        SessionManager sessions = SessionHolder.getRootSessionManager();
        AtomicReference<LockManager> managerDuringStop = new AtomicReference<>();
        SessionHolderFaultFileStoreProvider.onBackgroundStop(() -> {
            managerDuringStop.set(LockerManagerFactory.getLockManager());
            Assertions.assertSame(sessions, SessionHolder.getRootSessionManager());
        });

        if (failClose) {
            Assertions.assertSame(
                    closeFailure, Assertions.assertThrows(RuntimeException.class, SessionHolder::destroy));
        } else {
            Assertions.assertDoesNotThrow(SessionHolder::destroy);
        }

        Assertions.assertSame(installed, managerDuringStop.get());
        Assertions.assertEquals(1, SessionHolderFaultFileStoreProvider.closeCount());
        assertFileModeReferencesCleared();
        Assertions.assertDoesNotThrow(SessionHolder::destroy);
        Assertions.assertEquals(1, SessionHolderFaultFileStoreProvider.closeCount());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testFailedBackgroundStartRetainsLockManagerDuringCleanup(boolean failClose) throws Exception {
        RuntimeException startupFailure = new RuntimeException("partial background start");
        RuntimeException closeFailure = failClose ? new RuntimeException("close") : null;
        SessionManager sessions = Mockito.mock(SessionManager.class);
        FileLockStore lockStore = Mockito.mock(FileLockStore.class);
        BranchSession branch = new BranchSession();
        Locker locker = Mockito.mock(Locker.class);
        Mockito.when(lockStore.getLocker(branch)).thenReturn(locker);
        SessionHolderFaultFileStoreProvider.configure(
                FailurePoint.BACKGROUND, startupFailure, closeFailure, sessions, lockStore);
        SessionHolderFaultFileStoreProvider.onBackgroundStop(() -> {
            FileLockManager manager = (FileLockManager) LockerManagerFactory.getLockManager();
            Assertions.assertSame(locker, manager.getLocker(branch));
            Assertions.assertSame(sessions, SessionHolder.getRootSessionManager());
        });

        RuntimeException thrown =
                Assertions.assertThrows(RuntimeException.class, () -> SessionHolder.init(SessionMode.FILE));

        Assertions.assertSame(startupFailure, thrown);
        Assertions.assertArrayEquals(
                failClose ? new Throwable[] {closeFailure} : new Throwable[0], thrown.getSuppressed());
        Mockito.verify(lockStore).getLocker(branch);
        Assertions.assertEquals(1, SessionHolderFaultFileStoreProvider.closeCount());
        assertFileModeReferencesCleared();
    }

    @Test
    void testDefaultEnginePreservesLegacySessionAndLockProviders() throws Exception {
        System.setProperty(ConfigurationKeys.STORE_FILE_ENGINE, "default");
        ConfigurationCache.clear();
        Path resources = tempDir.resolve("providers");
        Path services = resources.resolve("META-INF/services");
        Files.createDirectories(services);
        Files.write(
                services.resolve(SessionManager.class.getName()),
                LegacySessionManager.class.getName().getBytes(StandardCharsets.UTF_8));
        Files.write(
                services.resolve(LockManager.class.getName()),
                LegacyLockManager.class.getName().getBytes(StandardCharsets.UTF_8));
        EnhancedServiceLoader.unload(SessionManager.class);
        EnhancedServiceLoader.unload(LockManager.class);
        try (URLClassLoader loader = new URLClassLoader(
                new URL[] {resources.toUri().toURL()}, getClass().getClassLoader())) {
            // Prime the real loader's provider definitions from an isolated extension resource.
            EnhancedServiceLoader.load(SessionManager.class, "file", loader).destroy();
            EnhancedServiceLoader.load(LockManager.class, "file", loader);
            SessionHolder.init(SessionMode.FILE);
            LegacySessionManager sessions = (LegacySessionManager) SessionHolder.getRootSessionManager();
            Assertions.assertEquals("root.data", sessions.openedName);
            Assertions.assertTrue(
                    sessions.openedPath.startsWith(tempDir.resolve("file").toString()));
            LegacyLockManager locks = (LegacyLockManager) LockerManagerFactory.getLockManager();
            BranchSession branch = new BranchSession(BranchType.AT);
            Assertions.assertTrue(branch.lock());
            Assertions.assertTrue(branch.unlock());
            Assertions.assertEquals(1, locks.acquired);
            Assertions.assertEquals(1, locks.released);
            SessionHolder.destroy();
            Assertions.assertTrue(sessions.destroyed);
            assertFileModeReferencesCleared();
        } finally {
            EnhancedServiceLoader.unload(SessionManager.class);
            EnhancedServiceLoader.unload(LockManager.class);
        }
    }

    @LoadLevel(name = "file", order = 100, scope = Scope.PROTOTYPE)
    public static class LegacySessionManager extends FileSessionManager {
        private String openedName;
        private String openedPath;
        private boolean destroyed;

        public LegacySessionManager() {
            super("probe");
        }

        public LegacySessionManager(String name, String path) {
            super(name);
            openedName = name;
            openedPath = path;
        }

        @Override
        public void destroy() {
            destroyed = true;
            super.destroy();
        }
    }

    @LoadLevel(name = "file", order = 100, scope = Scope.PROTOTYPE)
    public static class LegacyLockManager extends FileLockManager {
        private int acquired;
        private int released;

        public LegacyLockManager() {}

        @Override
        public boolean acquireLock(BranchSession branch, boolean autoCommit, boolean skipCheckLock) {
            acquired++;
            return true;
        }

        @Override
        public boolean releaseLock(BranchSession branch) {
            released++;
            return true;
        }
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
