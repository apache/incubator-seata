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
package com.alibaba.fescar.server.session;

import com.alibaba.fescar.core.exception.TransactionException;
import com.alibaba.fescar.core.model.BranchStatus;
import com.alibaba.fescar.core.model.GlobalStatus;

/**
 * The interface Session lifecycle.
 */
public interface SessionLifecycle {

    /**
     * Begin.
     *
     * @throws TransactionException the transaction exception
     */
    void begin() throws TransactionException;

    /**
     * Change status.
     *
     * @param status the status
     * @throws TransactionException the transaction exception
     */
    void changeStatus(GlobalStatus status) throws TransactionException;

    /**
     * Change branch status.
     *
     * @param branchSession the branch session
     * @param status        the status
     * @throws TransactionException the transaction exception
     */
    void changeBranchStatus(BranchSession branchSession, BranchStatus status) throws TransactionException;

    /**
     * Add branch.
     *
     * @param branchSession the branch session
     * @throws TransactionException the transaction exception
     */
    void addBranch(BranchSession branchSession) throws TransactionException;

    /**
     * Remove branch.
     *
     * @param branchSession the branch session
     * @throws TransactionException the transaction exception
     */
    void removeBranch(BranchSession branchSession) throws TransactionException;

    /**
     * Is active boolean.
     *
     * @return the boolean
     */
    boolean isActive();

    /**
     * Close.
     *
     * @throws TransactionException the transaction exception
     */
    void close() throws TransactionException;

    /**
     * End.
     *
     * @throws TransactionException the transaction exception
     */
    void end() throws TransactionException;
}
