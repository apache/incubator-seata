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

import org.apache.seata.common.util.StringUtils;

import java.util.Locale;

/**
 * WAL sync strategy for RocksDB file mode.
 */
public enum RocksDBWalSyncMode {

    /**
     * Do not issue extra WAL sync in async file flush mode.
     */
    NONE,

    /**
     * Periodically sync RocksDB WAL from a background thread.
     */
    PERIODIC;

    public static RocksDBWalSyncMode of(String value) {
        if (StringUtils.isBlank(value)) {
            return NONE;
        }
        String normalized = value.trim().replace('-', '_').toUpperCase(Locale.ROOT);
        for (RocksDBWalSyncMode mode : values()) {
            if (mode.name().equals(normalized)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("unknown RocksDB WAL sync mode:" + value);
    }

    public boolean isPeriodic() {
        return this == PERIODIC;
    }

    public String configValue() {
        return name().toLowerCase(Locale.ROOT);
    }
}
