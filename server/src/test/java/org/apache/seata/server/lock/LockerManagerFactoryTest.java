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

import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.common.store.LockMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * LockerManagerFactory Test
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LockerManagerFactory Test")
class LockerManagerFactoryTest {

    @Mock
    private LockManager mockLockManager;

    @BeforeEach
    void setUp() {
        LockerManagerFactory.destroy();
    }

    @AfterEach
    void tearDown() {
        LockerManagerFactory.destroy();
    }

    @Test
    @DisplayName("test init with default lock mode")
    void testInitWithDefaultLockMode() {
        LockerManagerFactory.init();
        // Should not throw exception
    }

    @Test
    @DisplayName("test getLockManager returns not null")
    void testGetLockManagerReturnsNotNull() {
        LockManager lockManager = LockerManagerFactory.getLockManager();
        assertNotNull(lockManager);
    }

    @Test
    @DisplayName("test getLockManager singleton")
    void testGetLockManagerSingleton() {
        LockerManagerFactory.init();
        LockManager lockManager1 = LockerManagerFactory.getLockManager();
        LockManager lockManager2 = LockerManagerFactory.getLockManager();

        assertNotNull(lockManager1);
        assertNotNull(lockManager2);
        assertSame(lockManager1, lockManager2);
    }

    @Test
    @DisplayName("test init with specific lock mode")
    void testInitWithSpecificLockMode() {
        try (MockedStatic<EnhancedServiceLoader> loaderMock = mockStatic(EnhancedServiceLoader.class)) {

            loaderMock
                    .when(() -> EnhancedServiceLoader.load(LockManager.class, "file"))
                    .thenReturn(mockLockManager);

            LockerManagerFactory.init(LockMode.FILE);
            LockManager lockManager = LockerManagerFactory.getLockManager();

            assertNotNull(lockManager);
        }
    }

    @Test
    @DisplayName("test destroy resets lock manager")
    void testDestroyResetsLockManager() {
        LockerManagerFactory.init();
        LockManager lockManager1 = LockerManagerFactory.getLockManager();
        assertNotNull(lockManager1);

        LockerManagerFactory.destroy();

        LockManager lockManager2 = LockerManagerFactory.getLockManager();
        assertNotNull(lockManager2);
    }

    @Test
    @DisplayName("test multiple calls to init")
    void testMultipleCallsToInit() {
        LockerManagerFactory.init();
        LockerManagerFactory.init();
        LockerManagerFactory.init();

        LockManager lockManager = LockerManagerFactory.getLockManager();
        assertNotNull(lockManager);
    }

    @Test
    @DisplayName("test thread safety of initialization")
    void testThreadSafetyOfInitialization() throws InterruptedException {
        Thread thread1 = new Thread(LockerManagerFactory::init);
        Thread thread2 = new Thread(LockerManagerFactory::init);

        thread1.start();
        thread2.start();

        thread1.join();
        thread2.join();

        LockManager lockManager = LockerManagerFactory.getLockManager();
        assertNotNull(lockManager);
    }

    @Test
    @DisplayName("test getLockManager calls init internally")
    void testGetLockManagerCallsInitInternally() {
        LockerManagerFactory.destroy();

        LockManager lockManager = LockerManagerFactory.getLockManager();
        assertNotNull(lockManager);
    }

    @Test
    @DisplayName("test init with FILE lock mode")
    void testInitWithFileLockMode() {
        try (MockedStatic<EnhancedServiceLoader> loaderMock = mockStatic(EnhancedServiceLoader.class)) {

            loaderMock
                    .when(() -> EnhancedServiceLoader.load(LockManager.class, "file"))
                    .thenReturn(mockLockManager);

            LockerManagerFactory.init(LockMode.FILE);
            assertNotNull(LockerManagerFactory.getLockManager());
        }
    }

    @Test
    @DisplayName("test init with DB lock mode")
    void testInitWithDBLockMode() {
        try (MockedStatic<EnhancedServiceLoader> loaderMock = mockStatic(EnhancedServiceLoader.class)) {

            loaderMock
                    .when(() -> EnhancedServiceLoader.load(LockManager.class, "db"))
                    .thenReturn(mockLockManager);

            LockerManagerFactory.init(LockMode.DB);
            assertNotNull(LockerManagerFactory.getLockManager());
        }
    }

    @Test
    @DisplayName("test init with REDIS lock mode")
    void testInitWithRedisLockMode() {
        try (MockedStatic<EnhancedServiceLoader> loaderMock = mockStatic(EnhancedServiceLoader.class)) {

            loaderMock
                    .when(() -> EnhancedServiceLoader.load(LockManager.class, "redis"))
                    .thenReturn(mockLockManager);

            LockerManagerFactory.init(LockMode.REDIS);
            assertNotNull(LockerManagerFactory.getLockManager());
        }
    }
}
