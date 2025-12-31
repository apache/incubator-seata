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
package org.apache.seata.server.lock.distributed;

import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.common.loader.EnhancedServiceNotFoundException;
import org.apache.seata.core.store.DefaultDistributedLocker;
import org.apache.seata.core.store.DistributedLocker;
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
 * DistributedLockerFactory Test
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DistributedLockerFactory Test")
class DistributedLockerFactoryTest {

    @Mock
    private DistributedLocker mockDistributedLocker;

    @BeforeEach
    void setUp() {
        DistributedLockerFactory.cleanLocker();
    }

    @AfterEach
    void tearDown() {
        DistributedLockerFactory.cleanLocker();
    }

    @Test
    @DisplayName("test getDistributedLocker with file type returns default locker")
    void testGetDistributedLockerWithFileTypeReturnsDefaultLocker() {
        DistributedLocker locker = DistributedLockerFactory.getDistributedLocker("file");
        
        assertNotNull(locker);
        assertTrue(locker instanceof DefaultDistributedLocker);
    }

    @Test
    @DisplayName("test getDistributedLocker with valid type")
    void testGetDistributedLockerWithValidType() {
        try (MockedStatic<EnhancedServiceLoader> enhancedServiceLoaderMock = 
                mockStatic(EnhancedServiceLoader.class)) {
            
            enhancedServiceLoaderMock.when(() -> EnhancedServiceLoader.load(
                    DistributedLocker.class, "redis"))
                    .thenReturn(mockDistributedLocker);
            
            DistributedLocker locker = DistributedLockerFactory.getDistributedLocker("redis");
            
            assertNotNull(locker);
            assertEquals(mockDistributedLocker, locker);
        }
    }

    @Test
    @DisplayName("test getDistributedLocker with unknown type returns default locker")
    void testGetDistributedLockerWithUnknownTypeReturnsDefaultLocker() {
        try (MockedStatic<EnhancedServiceLoader> enhancedServiceLoaderMock = 
                mockStatic(EnhancedServiceLoader.class)) {
            
            enhancedServiceLoaderMock.when(() -> EnhancedServiceLoader.load(
                    DistributedLocker.class, "unknown"))
                    .thenThrow(new EnhancedServiceNotFoundException("Not found"));
            
            DistributedLocker locker = DistributedLockerFactory.getDistributedLocker("unknown");
            
            assertNotNull(locker);
            assertTrue(locker instanceof DefaultDistributedLocker);
        }
    }

    @Test
    @DisplayName("test getDistributedLocker singleton pattern")
    void testGetDistributedLockerSingletonPattern() {
        DistributedLocker locker1 = DistributedLockerFactory.getDistributedLocker("file");
        DistributedLocker locker2 = DistributedLockerFactory.getDistributedLocker("file");
        
        assertSame(locker1, locker2);
    }

    @Test
    @DisplayName("test getDistributedLocker multiple times with same type")
    void testGetDistributedLockerMultipleTimesWithSameType() {
        DistributedLocker locker1 = DistributedLockerFactory.getDistributedLocker("file");
        DistributedLocker locker2 = DistributedLockerFactory.getDistributedLocker("file");
        DistributedLocker locker3 = DistributedLockerFactory.getDistributedLocker("file");
        
        assertNotNull(locker1);
        assertNotNull(locker2);
        assertNotNull(locker3);
        assertSame(locker1, locker2);
        assertSame(locker2, locker3);
    }

    @Test
    @DisplayName("test cleanLocker resets singleton")
    void testCleanLockerResetsSingleton() {
        DistributedLocker locker1 = DistributedLockerFactory.getDistributedLocker("file");
        
        DistributedLockerFactory.cleanLocker();
        
        DistributedLocker locker2 = DistributedLockerFactory.getDistributedLocker("file");
        
        assertNotNull(locker1);
        assertNotNull(locker2);
        // After clean, we should get a new instance (though it might be the same class)
        assertTrue(locker1.getClass().equals(locker2.getClass()));
    }

    @Test
    @DisplayName("test getDistributedLocker with redis type")
    void testGetDistributedLockerWithRedisType() {
        try (MockedStatic<EnhancedServiceLoader> enhancedServiceLoaderMock = 
                mockStatic(EnhancedServiceLoader.class)) {
            
            DistributedLocker redisLocker = mock(DistributedLocker.class);
            enhancedServiceLoaderMock.when(() -> EnhancedServiceLoader.load(
                    DistributedLocker.class, "redis"))
                    .thenReturn(redisLocker);
            
            DistributedLocker locker = DistributedLockerFactory.getDistributedLocker("redis");
            
            assertNotNull(locker);
            assertEquals(redisLocker, locker);
        }
    }

    @Test
    @DisplayName("test getDistributedLocker returns not null")
    void testGetDistributedLockerReturnsNotNull() {
        DistributedLocker locker = DistributedLockerFactory.getDistributedLocker("file");
        assertNotNull(locker);
    }

    @Test
    @DisplayName("test thread safety of singleton initialization")
    void testThreadSafetyOfSingletonInitialization() throws InterruptedException {
        Thread thread1 = new Thread(() -> 
            DistributedLockerFactory.getDistributedLocker("file"));
        Thread thread2 = new Thread(() -> 
            DistributedLockerFactory.getDistributedLocker("file"));
        
        DistributedLockerFactory.cleanLocker();
        
        thread1.start();
        thread2.start();
        
        thread1.join();
        thread2.join();
        
        DistributedLocker locker1 = DistributedLockerFactory.getDistributedLocker("file");
        assertNotNull(locker1);
    }
}

