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
package org.apache.seata.server.lock;

import java.util.List;

import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.lock.RowLock;
import org.apache.seata.core.model.LockStatus;
import org.apache.seata.server.session.BranchSession;
import org.apache.seata.server.session.GlobalSession;

/**
 * The interface Lock manager.
 *
 */
public interface LockManager {

    /**
     * Acquire lock boolean.
     *
     * @param branchSession the branch session
     * @return the boolean
     * @throws TransactionException the transaction exception
     */
    boolean acquireLock(BranchSession branchSession) throws TransactionException;

    /**
     * Acquire lock boolean.
     *
     * @param branchSession the branch session
     * @param autoCommit the auto commit
     * @param skipCheckLock whether skip check lock or not
     * @return the boolean
     * @throws TransactionException the transaction exception
     */
    boolean acquireLock(BranchSession branchSession, boolean autoCommit, boolean skipCheckLock) throws TransactionException;

    /**
     * Un lock boolean.
     *
     * @param branchSession the branch session
     * @return the boolean
     * @throws TransactionException the transaction exception
     */
    boolean releaseLock(BranchSession branchSession) throws TransactionException;

    /**
     * Un lock boolean.
     *
     * @param globalSession the global session
     * @return the boolean
     * @throws TransactionException the transaction exception
     */
    boolean releaseGlobalSessionLock(GlobalSession globalSession) throws TransactionException;

    /**
     * Is lockable boolean.
     *
     * @param xid        the xid
     * @param resourceId the resource id
     * @param lockKey    the lock key
     * @return the boolean
     * @throws TransactionException the transaction exception
     */
    boolean isLockable(String xid, String resourceId, String lockKey) throws TransactionException;

    /**
     * Clean all locks.
     *
     * @throws TransactionException the transaction exception
     */
    void cleanAllLocks() throws TransactionException;

    /**
     * Collect row locks list.`
     *
     * @param branchSession the branch session
     * @return the list
     */
    List<RowLock> collectRowLocks(BranchSession branchSession);

    /**
     * update lock status.
     * @param xid the xid
     * @param lockStatus the lock status
     * @throws TransactionException the transaction exception
     *
     */
    void updateLockStatus(String xid, LockStatus lockStatus) throws TransactionException;

}
