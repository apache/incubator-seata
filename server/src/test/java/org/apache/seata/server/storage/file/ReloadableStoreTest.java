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

import org.apache.seata.server.BaseSpringBootTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class ReloadableStoreTest extends BaseSpringBootTest {

    @Test
    public void testReloadableStoreImplementation() {
        ReloadableStore reloadableStore = new TestReloadableStore();
        Assertions.assertNotNull(reloadableStore);
    }

    @Test
    public void testReadWriteStore() {
        ReloadableStore reloadableStore = new TestReloadableStore();
        List<TransactionWriteStore> stores = reloadableStore.readWriteStore(10, false);
        Assertions.assertNotNull(stores);
    }

    @Test
    public void testHasRemaining() {
        ReloadableStore reloadableStore = new TestReloadableStore();
        boolean hasRemaining = reloadableStore.hasRemaining(false);
        Assertions.assertFalse(hasRemaining);
    }

    private static class TestReloadableStore implements ReloadableStore {
        @Override
        public List<TransactionWriteStore> readWriteStore(int readSize, boolean isHistory) {
            return new ArrayList<>();
        }

        @Override
        public boolean hasRemaining(boolean isHistory) {
            return false;
        }
    }
}
