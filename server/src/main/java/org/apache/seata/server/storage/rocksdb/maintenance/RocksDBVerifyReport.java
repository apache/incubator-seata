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
 * Immutable report produced by {@link RocksDBMaintenanceService#verifyCurrentState}.
 * Contains counts of checked entities and any inconsistencies found.
 */
public class RocksDBVerifyReport {

    private final int checkedGlobalCount;
    private final int checkedBranchCount;
    private final int checkedLockCount;
    private final int staleStatusIndexCount;
    private final int staleTransactionIdIndexCount;
    private final int orphanBranchCount;
    private final int orphanLockCount;
    private final int staleLockIndexCount;
    private final List<String> errorMessages;

    private RocksDBVerifyReport(Builder builder) {
        this.checkedGlobalCount = builder.checkedGlobalCount;
        this.checkedBranchCount = builder.checkedBranchCount;
        this.checkedLockCount = builder.checkedLockCount;
        this.staleStatusIndexCount = builder.staleStatusIndexCount;
        this.staleTransactionIdIndexCount = builder.staleTransactionIdIndexCount;
        this.orphanBranchCount = builder.orphanBranchCount;
        this.orphanLockCount = builder.orphanLockCount;
        this.staleLockIndexCount = builder.staleLockIndexCount;
        this.errorMessages = Collections.unmodifiableList(new ArrayList<>(builder.errorMessages));
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

    public int getStaleStatusIndexCount() {
        return staleStatusIndexCount;
    }

    public int getStaleTransactionIdIndexCount() {
        return staleTransactionIdIndexCount;
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

    public List<String> getErrorMessages() {
        return errorMessages;
    }

    /**
     * Returns true if the database state is consistent with no issues found.
     */
    public boolean isClean() {
        return staleStatusIndexCount == 0
                && staleTransactionIdIndexCount == 0
                && orphanBranchCount == 0
                && orphanLockCount == 0
                && staleLockIndexCount == 0
                && errorMessages.isEmpty();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("RocksDBVerifyReport{");
        sb.append("globals=").append(checkedGlobalCount);
        sb.append(", branches=").append(checkedBranchCount);
        sb.append(", locks=").append(checkedLockCount);
        sb.append(", staleStatusIndexes=").append(staleStatusIndexCount);
        sb.append(", staleTxnIdIndexes=").append(staleTransactionIdIndexCount);
        sb.append(", orphanBranches=").append(orphanBranchCount);
        sb.append(", orphanLocks=").append(orphanLockCount);
        sb.append(", staleLockIndexes=").append(staleLockIndexCount);
        if (!errorMessages.isEmpty()) {
            sb.append(", errors=").append(errorMessages.size());
        }
        sb.append(", clean=").append(isClean());
        sb.append('}');
        return sb.toString();
    }

    static Builder builder() {
        return new Builder();
    }

    static class Builder {
        int checkedGlobalCount;
        int checkedBranchCount;
        int checkedLockCount;
        int staleStatusIndexCount;
        int staleTransactionIdIndexCount;
        int orphanBranchCount;
        int orphanLockCount;
        int staleLockIndexCount;
        List<String> errorMessages = new ArrayList<>();

        Builder checkedGlobalCount(int count) {
            this.checkedGlobalCount = count;
            return this;
        }

        Builder checkedBranchCount(int count) {
            this.checkedBranchCount = count;
            return this;
        }

        Builder checkedLockCount(int count) {
            this.checkedLockCount = count;
            return this;
        }

        Builder staleStatusIndexCount(int count) {
            this.staleStatusIndexCount = count;
            return this;
        }

        Builder staleTransactionIdIndexCount(int count) {
            this.staleTransactionIdIndexCount = count;
            return this;
        }

        Builder orphanBranchCount(int count) {
            this.orphanBranchCount = count;
            return this;
        }

        Builder orphanLockCount(int count) {
            this.orphanLockCount = count;
            return this;
        }

        Builder staleLockIndexCount(int count) {
            this.staleLockIndexCount = count;
            return this;
        }

        Builder addError(String message) {
            this.errorMessages.add(message);
            return this;
        }

        RocksDBVerifyReport build() {
            return new RocksDBVerifyReport(this);
        }
    }
}
