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
package org.apache.seata.server.storage.rocksdb;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BooleanSupplier;

class RocksDBLocalLocksTest {

    private static final long TIMEOUT_SECONDS = 5;

    @Test
    void testLockAllOrdersStripesConsistently() throws Exception {
        RocksDBLocalLocks locks = new RocksDBLocalLocks(1024);
        int firstStripe = firstQueuedStripe(locks, Arrays.asList(new byte[] {0}, new byte[] {0, 0}), "31-961");
        int secondStripe = firstQueuedStripe(locks, Arrays.asList(new byte[] {0, 0}, new byte[] {3, 1}), "961-31");

        Assertions.assertEquals(31, firstStripe, "Key set [0], [0,0] must acquire stripe 31 before stripe 961");
        Assertions.assertEquals(
                31,
                secondStripe,
                "Key set [0,0], [3,1] must acquire stripe 31 first; stripe 961 first permits a 961 -> 31 / 31 -> 961 ABBA deadlock");
    }

    @Test
    void testLockAllDeduplicatesSameStripe() throws Exception {
        RocksDBLocalLocks locks = new RocksDBLocalLocks(1);

        try (RocksDBLocalLocks.LockScope ignored =
                locks.lockAll(Arrays.asList(new byte[] {1}, new byte[] {2}, new byte[] {1}))) {
            Assertions.assertEquals(1, lockStripes(locks)[0].getHoldCount());
        }
    }

    @Test
    void testLockAllHandlesEmptyCollection() {
        RocksDBLocalLocks locks = new RocksDBLocalLocks(1);

        try (RocksDBLocalLocks.LockScope ignored = locks.lockAll(Collections.emptyList())) {
            Assertions.assertNotNull(ignored);
        }
    }

    @Test
    void testRejectsNullKey() {
        RocksDBLocalLocks locks = new RocksDBLocalLocks(1);

        Assertions.assertThrows(NullPointerException.class, () -> locks.lock(null));
        Assertions.assertThrows(NullPointerException.class, () -> locks.lockAll(Arrays.asList(new byte[] {1}, null)));
    }

    @Test
    void testLockScopeReleasesLockWhenProtectedWorkThrows() throws Exception {
        RocksDBLocalLocks locks = new RocksDBLocalLocks(1);
        ReentrantLock stripe = lockStripes(locks)[0];

        Assertions.assertThrows(IllegalStateException.class, () -> {
            try (RocksDBLocalLocks.LockScope ignored = locks.lock(new byte[] {1})) {
                throw new IllegalStateException("test failure");
            }
        });

        CountDownLatch acquired = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread worker = new Thread(
                () -> {
                    try (RocksDBLocalLocks.LockScope ignored = locks.lock(new byte[] {1})) {
                        acquired.countDown();
                    } catch (Throwable throwable) {
                        failure.set(throwable);
                    }
                },
                "lock-after-exception");
        worker.setDaemon(true);
        worker.start();

        boolean acquiredInTime = false;
        try {
            acquiredInTime = acquired.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } finally {
            while (stripe.isHeldByCurrentThread()) {
                stripe.unlock();
            }
            worker.join(TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS));
        }
        Assertions.assertTrue(acquiredInTime, "Lock scope did not release its lock after protected work threw");
        Assertions.assertFalse(worker.isAlive(), "Worker did not finish after acquiring the released lock");
        Assertions.assertNull(failure.get(), "Lock worker failed: " + failure.get());
    }

    private static int firstQueuedStripe(RocksDBLocalLocks locks, Collection<byte[]> keys, String name)
            throws Exception {
        ReentrantLock[] stripes = lockStripes(locks);
        RocksDBLocalLocks.LockScope stripe31Blocker = locks.lock(new byte[] {0});
        RocksDBLocalLocks.LockScope stripe961Blocker = locks.lock(new byte[] {0, 0});
        CountDownLatch start = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread worker = lockAllWorker(locks, keys, start, failure, name);
        int firstStripe;

        try {
            worker.start();
            start.countDown();
            awaitCondition(
                    () -> stripes[31].hasQueuedThread(worker) || stripes[961].hasQueuedThread(worker),
                    "Worker did not queue for stripe 31 or 961 in time");
            firstStripe = stripes[31].hasQueuedThread(worker) ? 31 : 961;
        } finally {
            start.countDown();
            stripe961Blocker.close();
            stripe31Blocker.close();
            if (worker.getState() != Thread.State.NEW) {
                worker.join(TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS));
            }
        }

        Assertions.assertFalse(worker.isAlive(), "Lock worker did not finish after blockers were released");
        Assertions.assertNull(failure.get(), "Lock worker failed: " + failure.get());
        return firstStripe;
    }

    private static Thread lockAllWorker(
            RocksDBLocalLocks locks,
            Collection<byte[]> keys,
            CountDownLatch start,
            AtomicReference<Throwable> failure,
            String name) {
        Thread worker = new Thread(
                () -> {
                    try {
                        if (!start.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                            throw new AssertionError("Worker was not started in time");
                        }
                        try (RocksDBLocalLocks.LockScope scope = locks.lockAll(keys)) {
                            Assertions.assertNotNull(scope);
                        }
                    } catch (Throwable throwable) {
                        failure.compareAndSet(null, throwable);
                    }
                },
                "lock-order-" + name);
        worker.setDaemon(true);
        return worker;
    }

    private static ReentrantLock[] lockStripes(RocksDBLocalLocks locks) throws Exception {
        Field field = RocksDBLocalLocks.class.getDeclaredField("locks");
        field.setAccessible(true);
        return (ReentrantLock[]) field.get(locks);
    }

    private static void awaitCondition(BooleanSupplier condition, String message) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(TIMEOUT_SECONDS);
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
        Assertions.assertTrue(condition.getAsBoolean(), message);
    }
}
