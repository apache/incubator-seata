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
package org.apache.seata.server.storage.rocksdb.maintenance;

import org.apache.seata.server.storage.rocksdb.RocksDBColumnFamily;

import java.util.Arrays;
import java.util.Objects;

/**
 * Resume position for page-mode verification.
 */
public final class RocksDBVerifyCursor {

    private final RocksDBColumnFamily columnFamily;
    private final byte[] seekKey;

    public RocksDBVerifyCursor(RocksDBColumnFamily columnFamily, byte[] seekKey) {
        this.columnFamily = Objects.requireNonNull(columnFamily, "columnFamily must not be null");
        this.seekKey = Arrays.copyOf(Objects.requireNonNull(seekKey, "seekKey must not be null"), seekKey.length);
    }

    public RocksDBColumnFamily getColumnFamily() {
        return columnFamily;
    }

    public byte[] getSeekKey() {
        return Arrays.copyOf(seekKey, seekKey.length);
    }
}
