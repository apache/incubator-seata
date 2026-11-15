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
package org.apache.seata.server.storage.file;

import org.apache.seata.common.Constants;
import org.apache.seata.common.exception.StoreException;
import org.apache.seata.common.holder.ObjectHolder;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.server.session.BranchSession;
import org.apache.seata.server.session.GlobalSession;
import org.apache.seata.server.storage.file.lock.FileLockManager;
import org.apache.seata.server.storage.file.session.FileSessionManager;
import org.apache.seata.server.storage.file.spi.FileLockStore;
import org.apache.seata.server.storage.file.spi.FileStoreContext;
import org.apache.seata.server.storage.file.spi.FileStoreRuntime;
import org.apache.seata.server.storage.file.spi.SessionRecoveryConsumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.mock.env.MockEnvironment;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

class DefaultFileStoreRuntimeTest {

    @TempDir
    Path tempDir;

    private Object originalEnvironment;

    @BeforeEach
    void setUp() {
        originalEnvironment = ObjectHolder.INSTANCE.getObject(Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT);
        ObjectHolder.INSTANCE.setObject(Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT, new MockEnvironment());
    }

    @AfterEach
    void tearDown() throws Exception {
        restoreEnvironment();
    }

    @Test
    void testRecoverReloadsAppendLogBeforePassingSessionsToConsumer() throws Exception {
        FileSessionManager sessionManager = Mockito.mock(FileSessionManager.class);
        FileLockStore lockStore = Mockito.mock(FileLockStore.class);
        SessionRecoveryConsumer consumer = Mockito.mock(SessionRecoveryConsumer.class);
        GlobalSession session = Mockito.mock(GlobalSession.class);
        Mockito.when(sessionManager.allSessions()).thenReturn(Collections.singletonList(session));
        DefaultFileStoreRuntime runtime = new DefaultFileStoreRuntime(sessionManager, lockStore);

        runtime.recover(consumer);

        InOrder order = Mockito.inOrder(sessionManager, consumer);
        order.verify(sessionManager).reload();
        order.verify(sessionManager).allSessions();
        order.verify(consumer).accept(Collections.singletonList(session));
    }

    @Test
    void testBackgroundStartRequiresRecoveryAndCloseIsIdempotent() {
        FileSessionManager sessionManager = Mockito.mock(FileSessionManager.class);
        DefaultFileStoreRuntime runtime =
                new DefaultFileStoreRuntime(sessionManager, Mockito.mock(FileLockStore.class));

        Assertions.assertThrows(IllegalStateException.class, runtime::startBackgroundServices);
        runtime.recover(sessions -> {});
        runtime.startBackgroundServices();
        runtime.close();
        runtime.close();

        Mockito.verify(sessionManager).destroy();
    }

    @Test
    void testProviderFailureClosesCreatedSessionManagerAndSuppressesCleanupFailure() throws Exception {
        FileSessionManager sessionManager = Mockito.mock(FileSessionManager.class);
        RuntimeException openFailure = new RuntimeException("lock store");
        RuntimeException cleanupFailure = new RuntimeException("session close");
        Mockito.doThrow(cleanupFailure).when(sessionManager).destroy();
        DefaultFileStoreProvider provider = new DefaultFileStoreProvider() {
            @Override
            protected FileSessionManager createSessionManager(FileStoreContext context) {
                return sessionManager;
            }

            @Override
            protected FileLockStore createLockStore() {
                throw openFailure;
            }
        };

        RuntimeException thrown = Assertions.assertThrows(
                RuntimeException.class, () -> provider.open(new FileStoreContext("root.data", tempDir)));

        Assertions.assertSame(openFailure, thrown);
        Assertions.assertArrayEquals(new Throwable[] {cleanupFailure}, thrown.getSuppressed());
        Mockito.verify(sessionManager).destroy();
    }

    @Test
    void testCloseReopenReleasesOnlyOwnedLocks() throws Exception {
        FileLockManager manager = new FileLockManager();
        BranchSession unrelated = branch(8002, "t:2");
        Assertions.assertTrue(manager.acquireLock(unrelated));
        DefaultFileStoreProvider provider = new DefaultFileStoreProvider();
        FileStoreContext context = new FileStoreContext("root.data", tempDir);
        FileStoreRuntime first = provider.open(context);
        FileStoreRuntime second = null;
        try {
            GlobalSession global = new GlobalSession("app", "group", "tx", 60000);
            BranchSession oldBranch = branch(8001, "t:1");
            global.add(oldBranch);
            first.sessionManager().addGlobalSession(global);
            Assertions.assertTrue(manager.acquireLock(oldBranch));
            first.close();
            Assertions.assertTrue(oldBranch.getLockHolder().isEmpty());
            Assertions.assertFalse(manager.isLockable("127.0.0.1:8091:9000", "runtime-close", "t:2"));

            second = provider.open(context);
            BranchSession restored = branch(8001, "t:1");
            Assertions.assertTrue(manager.acquireLock(restored));
            Assertions.assertFalse(restored.getLockHolder().isEmpty());
            Assertions.assertTrue(manager.releaseLock(restored));
            Assertions.assertTrue(manager.isLockable("127.0.0.1:8091:9000", "runtime-close", "t:1"));
        } finally {
            first.close();
            if (second != null) {
                second.close();
            }
            manager.releaseLock(unrelated);
        }
    }

    @Test
    void testCloseAttemptsRemainingLocksAndSessionCleanupAfterReleaseFailure() throws Exception {
        FileSessionManager sessions = Mockito.mock(FileSessionManager.class);
        FileLockStore locks = Mockito.mock(FileLockStore.class);
        GlobalSession first = Mockito.mock(GlobalSession.class);
        GlobalSession second = Mockito.mock(GlobalSession.class);
        Mockito.when(sessions.allSessions()).thenReturn(Arrays.asList(first, second));
        Mockito.when(locks.releaseGlobalLock(first)).thenReturn(false);
        Mockito.when(locks.releaseGlobalLock(second)).thenReturn(true);
        DefaultFileStoreRuntime runtime = new DefaultFileStoreRuntime(sessions, locks);
        Assertions.assertThrows(StoreException.class, runtime::close);
        Mockito.verify(locks).releaseGlobalLock(second);
        Mockito.verify(sessions).destroy();
        Assertions.assertDoesNotThrow(runtime::close);
        Mockito.verify(sessions, Mockito.times(1)).destroy();
    }

    private static BranchSession branch(long transaction, String key) {
        BranchSession branch = new BranchSession(BranchType.AT);
        branch.setTransactionId(transaction);
        branch.setXid("127.0.0.1:8091:" + transaction);
        branch.setBranchId(transaction);
        branch.setResourceId("runtime-close");
        branch.setLockKey(key);
        return branch;
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
