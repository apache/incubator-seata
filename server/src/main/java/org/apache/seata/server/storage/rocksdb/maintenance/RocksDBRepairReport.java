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
 * Result of a controlled RocksDB repair attempt.
 */
public final class RocksDBRepairReport {
    public enum State {
        DRY_RUN,
        COMPLETED,
        PAUSED,
        STOPPED
    }

    private final boolean dryRun;
    private final int executedActionCount;
    private final RocksDBVerifyReport beforeVerifyReport;
    private final RocksDBVerifyReport afterVerifyReport;
    private final State state;
    private final int deletedLockIndexCount;
    private final byte[] nextSeekKey;

    RocksDBRepairReport(
            boolean dryRun,
            int executedActionCount,
            RocksDBVerifyReport beforeVerifyReport,
            RocksDBVerifyReport afterVerifyReport) {
        this.dryRun = dryRun;
        this.executedActionCount = executedActionCount;
        this.beforeVerifyReport = beforeVerifyReport;
        this.afterVerifyReport = afterVerifyReport;
        this.state = dryRun ? State.DRY_RUN : State.COMPLETED;
        this.deletedLockIndexCount = 0;
        this.nextSeekKey = null;
    }

    RocksDBRepairReport(
            State state,
            int executedActionCount,
            int deletedLockIndexCount,
            byte[] nextSeekKey,
            RocksDBVerifyReport beforeVerifyReport,
            RocksDBVerifyReport afterVerifyReport) {
        this.dryRun = false;
        this.executedActionCount = executedActionCount;
        this.beforeVerifyReport = beforeVerifyReport;
        this.afterVerifyReport = afterVerifyReport;
        this.state = state;
        this.deletedLockIndexCount = deletedLockIndexCount;
        this.nextSeekKey = nextSeekKey == null ? null : java.util.Arrays.copyOf(nextSeekKey, nextSeekKey.length);
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public int getExecutedActionCount() {
        return executedActionCount;
    }

    public RocksDBVerifyReport getBeforeVerifyReport() {
        return beforeVerifyReport;
    }

    public RocksDBVerifyReport getAfterVerifyReport() {
        return afterVerifyReport;
    }

    public State getState() {
        return state;
    }

    public int getDeletedLockIndexCount() {
        return deletedLockIndexCount;
    }

    public byte[] getNextSeekKey() {
        return nextSeekKey == null ? null : java.util.Arrays.copyOf(nextSeekKey, nextSeekKey.length);
    }
}
