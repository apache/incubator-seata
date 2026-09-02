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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Local striped locks used to protect RocksDB read-modify-write sections.
 */
public class RocksDBLocalLocks {

    private static final int DEFAULT_STRIPES = 1024;

    private final ReentrantLock[] locks;

    public RocksDBLocalLocks() {
        this(DEFAULT_STRIPES);
    }

    public RocksDBLocalLocks(int stripes) {
        if (stripes <= 0) {
            throw new IllegalArgumentException("stripes must be greater than 0");
        }
        locks = new ReentrantLock[stripes];
        for (int i = 0; i < locks.length; i++) {
            locks[i] = new ReentrantLock();
        }
    }

    public LockScope lock(byte[] key) {
        return lockAll(Collections.singletonList(key));
    }

    public LockScope lockAll(Collection<byte[]> keys) {
        if (keys == null || keys.isEmpty()) {
            return new LockScope(Collections.emptyList());
        }
        List<byte[]> sortedKeys = new ArrayList<>(keys.size());
        for (byte[] key : keys) {
            sortedKeys.add(Objects.requireNonNull(key, "key must not be null"));
        }
        Collections.sort(sortedKeys, RocksDBLocalLocks::compare);

        List<ReentrantLock> acquiredLocks = new ArrayList<>();
        Set<ReentrantLock> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (byte[] key : sortedKeys) {
            ReentrantLock lock = lockFor(key);
            if (seen.add(lock)) {
                lock.lock();
                acquiredLocks.add(lock);
            }
        }
        return new LockScope(acquiredLocks);
    }

    private ReentrantLock lockFor(byte[] key) {
        int hash = Arrays.hashCode(key);
        return locks[Math.floorMod(hash, locks.length)];
    }

    private static int compare(byte[] left, byte[] right) {
        if (left == right) {
            return 0;
        }
        int length = Math.min(left.length, right.length);
        for (int i = 0; i < length; i++) {
            int result = Byte.compare(left[i], right[i]);
            if (result != 0) {
                return result;
            }
        }
        return Integer.compare(left.length, right.length);
    }

    /**
     * Scope object that releases locks in reverse acquisition order.
     */
    public static class LockScope implements AutoCloseable {
        private final List<ReentrantLock> acquiredLocks;

        private LockScope(List<ReentrantLock> acquiredLocks) {
            this.acquiredLocks = acquiredLocks;
        }

        @Override
        public void close() {
            for (int i = acquiredLocks.size() - 1; i >= 0; i--) {
                acquiredLocks.get(i).unlock();
            }
        }
    }
}
