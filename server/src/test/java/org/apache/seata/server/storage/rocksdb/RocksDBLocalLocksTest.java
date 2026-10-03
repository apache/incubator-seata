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

import java.util.Arrays;

class RocksDBLocalLocksTest {

    @Test
    void testLockAllDeduplicatesSameStripe() {
        RocksDBLocalLocks locks = new RocksDBLocalLocks(1);

        try (RocksDBLocalLocks.LockScope ignored =
                locks.lockAll(Arrays.asList(new byte[] {1}, new byte[] {2}, new byte[] {1}))) {
            Assertions.assertNotNull(ignored);
        }
    }

    @Test
    void testRejectsNullKey() {
        RocksDBLocalLocks locks = new RocksDBLocalLocks(1);

        Assertions.assertThrows(NullPointerException.class, () -> locks.lock(null));
        Assertions.assertThrows(NullPointerException.class, () -> locks.lockAll(Arrays.asList(new byte[] {1}, null)));
    }
}
