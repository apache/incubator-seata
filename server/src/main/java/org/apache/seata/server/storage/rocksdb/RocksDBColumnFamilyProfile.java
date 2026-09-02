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

/**
 * Operational profiles used to group RocksDB column families with similar workloads.
 */
public enum RocksDBColumnFamilyProfile {
    GLOBAL,
    BRANCH,
    LOCK,
    INDEX,
    METADATA;

    public static RocksDBColumnFamilyProfile of(RocksDBColumnFamily columnFamily) {
        switch (columnFamily) {
            case GLOBAL_SESSION:
                return GLOBAL;
            case BRANCH_SESSION:
                return BRANCH;
            case LOCK:
                return LOCK;
            case LOCK_BRANCH_INDEX:
            case GLOBAL_STATUS_INDEX:
            case GLOBAL_TIMEOUT_INDEX:
            case TRANSACTION_ID_INDEX:
                return INDEX;
            case DEFAULT:
            case METADATA:
            default:
                return METADATA;
        }
    }
}
