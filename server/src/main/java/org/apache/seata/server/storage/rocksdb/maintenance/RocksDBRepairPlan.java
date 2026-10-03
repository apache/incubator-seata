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

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Immutable repair proposal derived from a consistency verification report.
 */
public final class RocksDBRepairPlan {

    public enum Action {
        REBUILD_GLOBAL_SECONDARY_INDEXES,
        DELETE_STALE_LOCK_BRANCH_INDEXES
    }

    private final boolean dryRun;
    private final Set<Action> actions;
    private final RocksDBVerifyReport beforeVerifyReport;
    private final boolean verificationComplete;
    private final boolean unrepairableSourceViolation;

    RocksDBRepairPlan(
            boolean dryRun,
            Set<Action> actions,
            RocksDBVerifyReport beforeVerifyReport,
            boolean verificationComplete,
            boolean unrepairableSourceViolation) {
        this.dryRun = dryRun;
        this.actions =
                actions.isEmpty() ? Collections.emptySet() : Collections.unmodifiableSet(EnumSet.copyOf(actions));
        this.beforeVerifyReport = beforeVerifyReport;
        this.verificationComplete = verificationComplete;
        this.unrepairableSourceViolation = unrepairableSourceViolation;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public Set<Action> getActions() {
        return actions;
    }

    public boolean hasAction(Action action) {
        return actions.contains(action);
    }

    public RocksDBVerifyReport getBeforeVerifyReport() {
        return beforeVerifyReport;
    }

    public boolean isVerificationComplete() {
        return verificationComplete;
    }

    public boolean hasUnrepairableSourceViolation() {
        return unrepairableSourceViolation;
    }
}
