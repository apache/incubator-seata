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
package org.apache.seata.server.store;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class FileStoreEngineTest {

    @Test
    void testGetName() {
        Assertions.assertEquals("file", FileStoreEngine.FILE.getName());
        Assertions.assertEquals("rocksdb", FileStoreEngine.ROCKSDB.getName());
    }

    @Test
    void testGet() {
        Assertions.assertEquals(FileStoreEngine.FILE, FileStoreEngine.get("file"));
        Assertions.assertEquals(FileStoreEngine.FILE, FileStoreEngine.get("FILE"));
        Assertions.assertEquals(FileStoreEngine.ROCKSDB, FileStoreEngine.get("rocksdb"));
        Assertions.assertEquals(FileStoreEngine.ROCKSDB, FileStoreEngine.get("RocksDB"));
    }

    @Test
    void testGetUnknown() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> FileStoreEngine.get("unknown"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> FileStoreEngine.get(""));
        Assertions.assertThrows(IllegalArgumentException.class, () -> FileStoreEngine.get(null));
    }
}
