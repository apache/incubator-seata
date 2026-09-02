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

import org.rocksdb.RocksDB;

import java.nio.charset.StandardCharsets;

/**
 * RocksDB column families used by file store engine.
 */
public enum RocksDBColumnFamily {
    /**
     * The default column family required by RocksDB.
     */
    DEFAULT("default", RocksDB.DEFAULT_COLUMN_FAMILY),
    /**
     * Metadata column family.
     */
    METADATA("metadata"),
    /**
     * Global session column family.
     */
    GLOBAL_SESSION("global_session"),
    /**
     * Branch session column family.
     */
    BRANCH_SESSION("branch_session"),
    /**
     * Lock holder column family.
     */
    LOCK("lock"),
    /**
     * Branch to lock index column family.
     */
    LOCK_BRANCH_INDEX("lock_branch_index"),
    /**
     * Global status query index column family.
     */
    GLOBAL_STATUS_INDEX("global_status_index"),
    /**
     * Global timeout deadline query index column family.
     */
    GLOBAL_TIMEOUT_INDEX("global_timeout_index"),
    /**
     * Global transaction id query index column family.
     */
    TRANSACTION_ID_INDEX("transaction_id_index");

    private final String name;
    private final byte[] nameBytes;

    RocksDBColumnFamily(String name) {
        this(name, name.getBytes(StandardCharsets.UTF_8));
    }

    RocksDBColumnFamily(String name, byte[] nameBytes) {
        this.name = name;
        this.nameBytes = nameBytes;
    }

    public String getName() {
        return name;
    }

    public byte[] getNameBytes() {
        return nameBytes.clone();
    }
}
