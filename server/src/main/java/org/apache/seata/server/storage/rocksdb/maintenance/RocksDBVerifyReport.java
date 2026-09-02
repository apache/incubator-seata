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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable report produced by RocksDB consistency verification.
 */
public class RocksDBVerifyReport {

    private final RocksDBVerifyMode mode;
    private final boolean complete;
    private final RocksDBVerifyCursor nextCursor;
    private final boolean truncated;
    private final int scannedRecordCount;
    private final int checkedRecordCount;
    private final int checkedGlobalCount;
    private final int checkedBranchCount;
    private final int checkedLockCount;
    private final int checkedIndexCount;
    private final int staleStatusIndexCount;
    private final int staleTimeoutIndexCount;
    private final int staleTransactionIdIndexCount;
    private final int missingStatusIndexCount;
    private final int missingTimeoutIndexCount;
    private final int missingTransactionIdIndexCount;
    private final int invalidGlobalCount;
    private final int invalidBranchCount;
    private final int orphanBranchCount;
    private final int orphanLockCount;
    private final int staleLockIndexCount;
    private final int invalidMetadataCount;
    private final int inconsistentCount;
    private final int totalErrorCount;
    private final List<String> errorMessages;

    private RocksDBVerifyReport(Builder builder) {
        this.mode = builder.mode;
        this.complete = builder.complete;
        this.nextCursor = builder.nextCursor;
        this.truncated = builder.truncated;
        this.scannedRecordCount = builder.scannedRecordCount;
        this.checkedRecordCount = builder.checkedRecordCount;
        this.checkedGlobalCount = builder.checkedGlobalCount;
        this.checkedBranchCount = builder.checkedBranchCount;
        this.checkedLockCount = builder.checkedLockCount;
        this.checkedIndexCount = builder.checkedIndexCount;
        this.staleStatusIndexCount = builder.staleStatusIndexCount;
        this.staleTimeoutIndexCount = builder.staleTimeoutIndexCount;
        this.staleTransactionIdIndexCount = builder.staleTransactionIdIndexCount;
        this.missingStatusIndexCount = builder.missingStatusIndexCount;
        this.missingTimeoutIndexCount = builder.missingTimeoutIndexCount;
        this.missingTransactionIdIndexCount = builder.missingTransactionIdIndexCount;
        this.invalidGlobalCount = builder.invalidGlobalCount;
        this.invalidBranchCount = builder.invalidBranchCount;
        this.orphanBranchCount = builder.orphanBranchCount;
        this.orphanLockCount = builder.orphanLockCount;
        this.staleLockIndexCount = builder.staleLockIndexCount;
        this.invalidMetadataCount = builder.invalidMetadataCount;
        this.inconsistentCount = builder.inconsistentCount;
        this.totalErrorCount = builder.totalErrorCount;
        this.errorMessages = Collections.unmodifiableList(new ArrayList<>(builder.errorMessages));
    }

    public RocksDBVerifyMode getMode() {
        return mode;
    }

    public boolean isComplete() {
        return complete;
    }

    public RocksDBVerifyCursor getNextCursor() {
        return nextCursor;
    }

    public boolean isTruncated() {
        return truncated;
    }

    public int getScannedRecordCount() {
        return scannedRecordCount;
    }

    public int getCheckedRecordCount() {
        return checkedRecordCount;
    }

    public int getCheckedGlobalCount() {
        return checkedGlobalCount;
    }

    public int getCheckedBranchCount() {
        return checkedBranchCount;
    }

    public int getCheckedLockCount() {
        return checkedLockCount;
    }

    public int getCheckedIndexCount() {
        return checkedIndexCount;
    }

    public int getStaleStatusIndexCount() {
        return staleStatusIndexCount;
    }

    public int getStaleTimeoutIndexCount() {
        return staleTimeoutIndexCount;
    }

    public int getStaleTransactionIdIndexCount() {
        return staleTransactionIdIndexCount;
    }

    public int getMissingStatusIndexCount() {
        return missingStatusIndexCount;
    }

    public int getMissingTimeoutIndexCount() {
        return missingTimeoutIndexCount;
    }

    public int getMissingTransactionIdIndexCount() {
        return missingTransactionIdIndexCount;
    }

    public int getInvalidGlobalCount() {
        return invalidGlobalCount;
    }

    public int getInvalidBranchCount() {
        return invalidBranchCount;
    }

    public int getOrphanBranchCount() {
        return orphanBranchCount;
    }

    public int getOrphanLockCount() {
        return orphanLockCount;
    }

    public int getStaleLockIndexCount() {
        return staleLockIndexCount;
    }

    public int getInvalidMetadataCount() {
        return invalidMetadataCount;
    }

    public int getInconsistentCount() {
        return inconsistentCount;
    }

    public int getTotalErrorCount() {
        return totalErrorCount;
    }

    public List<String> getErrorMessages() {
        return errorMessages;
    }

    public boolean isClean() {
        return inconsistentCount == 0;
    }

    @Override
    public String toString() {
        return "RocksDBVerifyReport{" + "mode=" + mode + ", complete=" + complete + ", truncated=" + truncated
                + ", scannedRecords=" + scannedRecordCount + ", checkedRecords=" + checkedRecordCount + ", globals="
                + checkedGlobalCount + ", branches=" + checkedBranchCount
                + ", locks=" + checkedLockCount + ", indexes=" + checkedIndexCount + ", inconsistencies="
                + inconsistentCount + ", errorSamples=" + errorMessages.size() + ", clean=" + isClean() + '}';
    }

    static Builder builder(RocksDBVerifyOptions options) {
        return new Builder(options);
    }

    static final class Builder {
        private final RocksDBVerifyMode mode;
        private final int maxErrorSamples;
        private boolean complete;
        private RocksDBVerifyCursor nextCursor;
        private boolean truncated;
        private int scannedRecordCount;
        private int checkedRecordCount;
        private int checkedGlobalCount;
        private int checkedBranchCount;
        private int checkedLockCount;
        private int checkedIndexCount;
        private int staleStatusIndexCount;
        private int staleTimeoutIndexCount;
        private int staleTransactionIdIndexCount;
        private int missingStatusIndexCount;
        private int missingTimeoutIndexCount;
        private int missingTransactionIdIndexCount;
        private int invalidGlobalCount;
        private int invalidBranchCount;
        private int orphanBranchCount;
        private int orphanLockCount;
        private int staleLockIndexCount;
        private int invalidMetadataCount;
        private int inconsistentCount;
        private int totalErrorCount;
        private final List<String> errorMessages = new ArrayList<>();

        private Builder(RocksDBVerifyOptions options) {
            this.mode = options.getMode();
            this.maxErrorSamples = options.getMaxErrorSamples();
        }

        void checkedRecord(boolean index) {
            checkedRecordCount++;
            if (index) {
                checkedIndexCount++;
            }
        }

        void checkedGlobal() {
            checkedGlobalCount++;
        }

        void checkedBranch() {
            checkedBranchCount++;
        }

        void checkedLock() {
            checkedLockCount++;
        }

        void staleStatusIndex(String message) {
            staleStatusIndexCount++;
            issue(message);
        }

        void staleTimeoutIndex(String message) {
            staleTimeoutIndexCount++;
            issue(message);
        }

        void staleTransactionIdIndex(String message) {
            staleTransactionIdIndexCount++;
            issue(message);
        }

        void missingStatusIndex(String message) {
            missingStatusIndexCount++;
            issue(message);
        }

        void missingTimeoutIndex(String message) {
            missingTimeoutIndexCount++;
            issue(message);
        }

        void missingTransactionIdIndex(String message) {
            missingTransactionIdIndexCount++;
            issue(message);
        }

        void scannedRecords(int scannedRecords) {
            scannedRecordCount += scannedRecords;
        }

        void invalidBranch(String message) {
            invalidBranchCount++;
            issue(message);
        }

        void invalidGlobal(String message) {
            invalidGlobalCount++;
            issue(message);
        }

        void orphanBranch(String message) {
            orphanBranchCount++;
            issue(message);
        }

        void orphanLock(String message) {
            orphanLockCount++;
            issue(message);
        }

        void staleLockIndex(String message) {
            staleLockIndexCount++;
            issue(message);
        }

        void invalidMetadata(String message) {
            invalidMetadataCount++;
            issue(message);
        }

        void error(String message) {
            issue(message);
        }

        private void issue(String message) {
            inconsistentCount++;
            totalErrorCount++;
            if (errorMessages.size() < maxErrorSamples) {
                errorMessages.add(message);
            }
        }

        void complete(boolean complete) {
            this.complete = complete;
        }

        void nextCursor(RocksDBVerifyCursor nextCursor) {
            this.nextCursor = nextCursor;
        }

        void truncated() {
            this.truncated = true;
        }

        boolean isComplete() {
            return complete;
        }

        boolean requiresGlobalSecondaryIndexRebuild() {
            return staleStatusIndexCount > 0
                    || staleTimeoutIndexCount > 0
                    || staleTransactionIdIndexCount > 0
                    || missingStatusIndexCount > 0
                    || missingTimeoutIndexCount > 0
                    || missingTransactionIdIndexCount > 0;
        }

        boolean hasUnrepairableSourceViolation() {
            return invalidGlobalCount > 0
                    || invalidBranchCount > 0
                    || orphanBranchCount > 0
                    || orphanLockCount > 0
                    || invalidMetadataCount > 0;
        }

        RocksDBVerifyReport build() {
            return new RocksDBVerifyReport(this);
        }
    }
}
