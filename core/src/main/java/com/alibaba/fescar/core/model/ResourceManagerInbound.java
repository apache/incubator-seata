/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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

package com.alibaba.fescar.core.model;

import com.alibaba.fescar.core.exception.TransactionException;

/**
 * Resource Manager: receive inbound request from TC.
 */
public interface ResourceManagerInbound {

    /**
     * Commit a branch transaction.
     *
     * @param xid Transaction id.
     * @param branchId Branch id.
     * @param resourceId Resource id.
     * @param applicationData Application data bind with this branch.
     * @return Status of the branch after committing.
     * @throws TransactionException Any exception that fails this will be wrapped with TransactionException and thrown out.
     */
    BranchStatus branchCommit(String xid, long branchId, String resourceId, String applicationData) throws TransactionException;


    /**
     * Rollback a branch transaction.
     *
     * @param xid Transaction id.
     * @param branchId Branch id.
     * @param resourceId Resource id.
     * @param applicationData Application data bind with this branch.
     * @return Status of the branch after rollbacking.
     * @throws TransactionException Any exception that fails this will be wrapped with TransactionException and thrown out.
     */
    BranchStatus branchRollback(String xid, long branchId, String resourceId, String applicationData) throws TransactionException;
}
