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
package org.apache.seata.server.session;

import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.model.BranchStatus;
import org.apache.seata.core.model.GlobalStatus;

/**
 * The interface Session lifecycle.
 *
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
    void changeGlobalStatus(GlobalStatus status) throws TransactionException;

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
     * Release the lock of branch.
     *
     * @param branchSession the branch session
     * @throws TransactionException the transaction exception
     */
    void unlockBranch(BranchSession branchSession) throws TransactionException;

    /**
     * Remove branch.
     *
     * @param branchSession the branch session
     * @throws TransactionException the transaction exception
     */
    void removeBranch(BranchSession branchSession) throws TransactionException;

    /**
     * Remove branch and release the lock of branch.
     *
     * @param branchSession the branchSession
     * @throws TransactionException the TransactionException
     */
    void removeAndUnlockBranch(BranchSession branchSession) throws TransactionException;

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
     * end.
     *
     * @throws TransactionException the transaction exception
     */
    void end() throws TransactionException;
}
