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
package org.apache.seata.common.lock;

import org.apache.seata.common.util.CollectionUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class ResourceLockTest {

    @Test
    public void testObtainAndClose() {
        ResourceLock resourceLock = new ResourceLock();

        // Test obtaining the lock
        try (ResourceLock lock = resourceLock.obtain()) {
            assertTrue(resourceLock.isHeldByCurrentThread(), "Lock should be held by current thread");
        }

        // After try-with-resources, lock should be released
        assertFalse(resourceLock.isHeldByCurrentThread(), "Lock should be released after try-with-resources");
    }

    @Test
    public void testMultipleObtainAndClose() {
        ResourceLock resourceLock = new ResourceLock();

        // First obtain and release
        try (ResourceLock lock = resourceLock.obtain()) {
            assertTrue(resourceLock.isHeldByCurrentThread(), "Lock should be held by current thread");
        }
        assertFalse(resourceLock.isHeldByCurrentThread(), "Lock should be released after first try-with-resources");

        // Second obtain and release
        try (ResourceLock lock = resourceLock.obtain()) {
            assertTrue(resourceLock.isHeldByCurrentThread(), "Lock should be held by current thread");
        }
        assertFalse(resourceLock.isHeldByCurrentThread(), "Lock should be released after second try-with-resources");
    }

    @Test
    public void testResourceLockAutoRemovalFromMap() {
        ConcurrentHashMap<String, ResourceLock> lockMap = new ConcurrentHashMap<>();
        String key = "testKey";
        // Use try-with-resources to obtain and release the lock
        try (ResourceLock ignored = CollectionUtils.computeIfAbsent(lockMap, key, k -> new ResourceLock()).obtain()) {
            // Do something while holding the lock
            assertTrue(lockMap.containsKey(key));
            assertTrue(lockMap.get(key).isHeldByCurrentThread());
        } finally {
            assertFalse(lockMap.get(key).isHeldByCurrentThread());
            assertTrue(lockMap.containsKey(key));
            // Remove the lock from the map
            lockMap.remove(key);
            assertFalse(lockMap.containsKey(key));
        }
        // Ensure the lock is removed from the map
        assertFalse(lockMap.containsKey(key));
    }

    @Test
    public void testConcurrentLocking() throws InterruptedException {
        ResourceLock resourceLock = new ResourceLock();

        Thread t1 = new Thread(() -> {
            try (ResourceLock lock = resourceLock.obtain()) {
                try {
                    Thread.sleep(100); // Hold the lock for 100ms
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        Thread t2 = new Thread(() -> {
            assertFalse(resourceLock.isHeldByCurrentThread(), "Lock should not be held by current thread before t1 releases it");
            try (ResourceLock lock = resourceLock.obtain()) {
                assertTrue(resourceLock.isHeldByCurrentThread(), "Lock should be held by current thread after t1 releases it");
            }
        });

        t1.start();
        t2.start();

        t1.join();
        t2.join();

        assertFalse(resourceLock.isHeldByCurrentThread(), "Lock should be released after both threads complete");
    }

    @Test
    public void testLockInterruptibly() throws InterruptedException {
        ResourceLock resourceLock = new ResourceLock();

        Thread t1 = new Thread(() -> {
            try (ResourceLock lock = resourceLock.obtain()) {
                try {
                    Thread.sleep(1000); // Hold the lock for 1000ms
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        t1.start();
        Thread.sleep(50); // Wait for t1 to acquire the lock

        Thread t2 = new Thread(() -> {
            try {
                resourceLock.lockInterruptibly();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        t2.start();
        Thread.sleep(50); // Wait for t2 to attempt to acquire the lock

        t2.interrupt(); // Interrupt t2

        t1.join();
        t2.join();

        assertFalse(resourceLock.isHeldByCurrentThread(), "Lock should be released after t1 completes");
    }
}
