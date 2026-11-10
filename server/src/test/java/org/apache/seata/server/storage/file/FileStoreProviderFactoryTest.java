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
import org.apache.seata.common.holder.ObjectHolder;
import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.common.loader.EnhancedServiceNotFoundException;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.lock.Locker;
import org.apache.seata.server.session.BranchSession;
import org.apache.seata.server.session.GlobalSession;
import org.apache.seata.server.storage.file.spi.FileLockStore;
import org.apache.seata.server.storage.file.spi.FileStoreContext;
import org.apache.seata.server.storage.file.spi.FileStoreProvider;
import org.apache.seata.server.storage.file.spi.FileStoreRuntime;
import org.apache.seata.server.storage.file.spi.SessionRecoveryConsumer;
import org.apache.seata.server.storage.file.spi.TestNamedFileStoreProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.env.MockEnvironment;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

public class FileStoreProviderFactoryTest {

    @TempDir
    java.nio.file.Path tempDir;

    private Object originalEnvironment;

    @BeforeEach
    void unloadProviders() throws Exception {
        originalEnvironment = ObjectHolder.INSTANCE.getObject(Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT);
        ObjectHolder.INSTANCE.setObject(Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT, new MockEnvironment());
        providerCache().clear();
        EnhancedServiceLoader.unload(FileStoreProvider.class);
    }

    @AfterEach
    void cleanupProviders() throws Exception {
        providerCache().clear();
        EnhancedServiceLoader.unload(FileStoreProvider.class);
        restoreEnvironment();
    }

    @Test
    public void testGetProvider_default() {
        FileStoreProvider provider = FileStoreProviderFactory.getProvider("default");

        assertThat(provider).isInstanceOf(DefaultFileStoreProvider.class);
    }

    @Test
    public void testGetProvider_named() {
        FileStoreProvider provider = FileStoreProviderFactory.getProvider("test");

        assertThat(provider).isInstanceOf(TestNamedFileStoreProvider.class);
    }

    @Test
    public void testGetProvider_nullOrBlankName_returnsDefault() {
        FileStoreProvider defaultProvider = FileStoreProviderFactory.getProvider("default");

        assertThat(FileStoreProviderFactory.getProvider(null)).isSameAs(defaultProvider);
        assertThat(FileStoreProviderFactory.getProvider(" ")).isSameAs(defaultProvider);
    }

    @Test
    public void testGetProvider_unknownName_throwsException() {
        assertThatThrownBy(() -> FileStoreProviderFactory.getProvider("unknown"))
                .isInstanceOf(EnhancedServiceNotFoundException.class);
    }

    @Test
    public void testGetProvider_sameNameReturnsCachedInstance() {
        FileStoreProvider first = FileStoreProviderFactory.getProvider("test");
        FileStoreProvider second = FileStoreProviderFactory.getProvider("test");

        assertThat(second).isSameAs(first);
    }

    @Test
    public void testOpen_returnsDistinctRuntimesWithIndependentLifecycle() {
        FileStoreProvider provider = FileStoreProviderFactory.getProvider("default");
        FileStoreRuntime first = provider.open(new FileStoreContext("root", tempDir.resolve("first")));
        FileStoreRuntime second = provider.open(new FileStoreContext("root", tempDir.resolve("second")));

        assertThat(second).isNotSameAs(first);
        first.close();
        second.close();
    }

    @Test
    public void testSpiContracts_areNarrowAndLifecycleAware() {
        assertAll(
                () -> assertThat(AutoCloseable.class.isAssignableFrom(FileStoreRuntime.class))
                        .isTrue(),
                () -> assertThat(SessionRecoveryConsumer.class
                                .getMethod("accept", Collection.class)
                                .getExceptionTypes())
                        .containsExactly(TransactionException.class),
                () -> assertThat(FileLockStore.class.getInterfaces()).isEmpty(),
                () -> assertThat(FileLockStore.class
                                .getMethod("getLocker", BranchSession.class)
                                .getReturnType())
                        .isEqualTo(Locker.class),
                () -> assertThat(FileLockStore.class
                                .getMethod("releaseBranchLock", BranchSession.class)
                                .getExceptionTypes())
                        .containsExactly(TransactionException.class),
                () -> assertThat(FileLockStore.class
                                .getMethod("releaseGlobalLock", GlobalSession.class)
                                .getExceptionTypes())
                        .containsExactly(TransactionException.class));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, FileStoreProvider> providerCache() throws Exception {
        Field field = FileStoreProviderFactory.class.getDeclaredField("INSTANCES");
        field.setAccessible(true);
        return (Map<String, FileStoreProvider>) field.get(null);
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
