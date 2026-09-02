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

/**
 * Explicit safety controls for RocksDB maintenance repair.
 */
public final class RocksDBRepairOptions {

    private static final long DEFAULT_VERIFY_DEADLINE_MILLIS = 60_000L;
    private static final int DEFAULT_MAX_REPAIR_ENTRIES = 10_000;

    private final boolean dryRun;
    private final boolean confirm;
    private final boolean maintenanceMode;
    private final long verifyDeadlineMillis;
    private final int maxRepairEntries;
    private final String lockIndexRunId;
    private final int lockIndexBatchLimit;
    private final int maxLockIndexBatches;
    private final long lockIndexRoundSleepMillis;

    private RocksDBRepairOptions(Builder builder) {
        this.dryRun = builder.dryRun;
        this.confirm = builder.confirm;
        this.maintenanceMode = builder.maintenanceMode;
        this.verifyDeadlineMillis = builder.verifyDeadlineMillis;
        this.maxRepairEntries = builder.maxRepairEntries;
        this.lockIndexRunId = builder.lockIndexRunId;
        this.lockIndexBatchLimit = builder.lockIndexBatchLimit;
        this.maxLockIndexBatches = builder.maxLockIndexBatches;
        this.lockIndexRoundSleepMillis = builder.lockIndexRoundSleepMillis;
    }

    public static RocksDBRepairOptions defaults() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public boolean isConfirm() {
        return confirm;
    }

    public boolean isMaintenanceMode() {
        return maintenanceMode;
    }

    public long getVerifyDeadlineMillis() {
        return verifyDeadlineMillis;
    }

    public int getMaxRepairEntries() {
        return maxRepairEntries;
    }

    public String getLockIndexRunId() {
        return lockIndexRunId;
    }

    public int getLockIndexBatchLimit() {
        return lockIndexBatchLimit;
    }

    public int getMaxLockIndexBatches() {
        return maxLockIndexBatches;
    }

    public long getLockIndexRoundSleepMillis() {
        return lockIndexRoundSleepMillis;
    }

    public static final class Builder {
        private boolean dryRun = true;
        private boolean confirm;
        private boolean maintenanceMode;
        private long verifyDeadlineMillis = DEFAULT_VERIFY_DEADLINE_MILLIS;
        private int maxRepairEntries = DEFAULT_MAX_REPAIR_ENTRIES;
        private String lockIndexRunId;
        private int lockIndexBatchLimit = 100;
        private int maxLockIndexBatches = 1;
        private long lockIndexRoundSleepMillis;

        private Builder() {}

        public Builder dryRun(boolean dryRun) {
            this.dryRun = dryRun;
            return this;
        }

        public Builder confirm(boolean confirm) {
            this.confirm = confirm;
            return this;
        }

        public Builder maintenanceMode(boolean maintenanceMode) {
            this.maintenanceMode = maintenanceMode;
            return this;
        }

        public Builder verifyDeadlineMillis(long verifyDeadlineMillis) {
            if (verifyDeadlineMillis < 0) {
                throw new IllegalArgumentException("verifyDeadlineMillis must be non-negative");
            }
            this.verifyDeadlineMillis = verifyDeadlineMillis;
            return this;
        }

        public Builder maxRepairEntries(int maxRepairEntries) {
            if (maxRepairEntries <= 0) {
                throw new IllegalArgumentException("maxRepairEntries must be positive");
            }
            this.maxRepairEntries = maxRepairEntries;
            return this;
        }

        public Builder lockIndexRunId(String lockIndexRunId) {
            this.lockIndexRunId = lockIndexRunId;
            return this;
        }

        public Builder lockIndexBatchLimit(int lockIndexBatchLimit) {
            if (lockIndexBatchLimit <= 0) {
                throw new IllegalArgumentException("lockIndexBatchLimit must be positive");
            }
            this.lockIndexBatchLimit = lockIndexBatchLimit;
            return this;
        }

        public Builder maxLockIndexBatches(int maxLockIndexBatches) {
            if (maxLockIndexBatches <= 0) {
                throw new IllegalArgumentException("maxLockIndexBatches must be positive");
            }
            this.maxLockIndexBatches = maxLockIndexBatches;
            return this;
        }

        public Builder lockIndexRoundSleepMillis(long lockIndexRoundSleepMillis) {
            if (lockIndexRoundSleepMillis < 0) {
                throw new IllegalArgumentException("lockIndexRoundSleepMillis must be non-negative");
            }
            this.lockIndexRoundSleepMillis = lockIndexRoundSleepMillis;
            return this;
        }

        public RocksDBRepairOptions build() {
            return new RocksDBRepairOptions(this);
        }
    }
}
