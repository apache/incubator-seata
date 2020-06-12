/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.rm.transaction;

import io.seata.core.model.BranchStatus;
import io.seata.core.model.BranchType;

/**
 * Resource manager transaction hook
 *
 * @author wang.liang
 */
public interface RMTransactionHook {

    /**
     * before branch commit
     */
    default void beforeBranchCommit(BranchType branchType, String xid, long branchId) {
    }

    /**
     * after branch committed
     */
    default void afterBranchCommitted(BranchType branchType, String xid, long branchId, BranchStatus returnBranchStatus) {
    }

    /**
     * after branch commit failed
     */
    default void afterBranchCommitFailed(BranchType branchType, String xid, long branchId, Exception e) {
    }

    /**
     * before branch rollback
     */
    default void beforeBranchRollback(BranchType branchType, String xid, long branchId) {
    }

    /**
     * after branch rollbacked
     */
    default void afterBranchRollbacked(BranchType branchType, String xid, long branchId, BranchStatus returnBranchStatus) {
    }

    /**
     * after branch rollback failed
     */
    default  void afterBranchRollbackFailed(BranchType branchType, String xid, long branchId, Exception e) {
    }
}
