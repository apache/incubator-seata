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

import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.lock.RowLock;
import org.apache.seata.core.model.LockStatus;
import org.apache.seata.server.session.BranchSession;
import org.apache.seata.server.session.GlobalSession;

import java.util.List;

/** A compatibility facade that resolves the installed manager on every operation. */
public final class DelegatingLockManager implements LockManager {

    @Override
    public boolean acquireLock(BranchSession branchSession) throws TransactionException {
        return LockerManagerFactory.getLockManager().acquireLock(branchSession);
    }

    @Override
    public boolean acquireLock(BranchSession branchSession, boolean autoCommit, boolean skipCheckLock)
            throws TransactionException {
        return LockerManagerFactory.getLockManager().acquireLock(branchSession, autoCommit, skipCheckLock);
    }

    @Override
    public boolean releaseLock(BranchSession branchSession) throws TransactionException {
        return LockerManagerFactory.getLockManager().releaseLock(branchSession);
    }

    @Override
    public boolean releaseGlobalSessionLock(GlobalSession globalSession) throws TransactionException {
        return LockerManagerFactory.getLockManager().releaseGlobalSessionLock(globalSession);
    }

    @Override
    public boolean isLockable(String xid, String resourceId, String lockKey) throws TransactionException {
        return LockerManagerFactory.getLockManager().isLockable(xid, resourceId, lockKey);
    }

    @Override
    public void cleanAllLocks() throws TransactionException {
        LockerManagerFactory.getLockManager().cleanAllLocks();
    }

    @Override
    public List<RowLock> collectRowLocks(BranchSession branchSession) {
        return LockerManagerFactory.getLockManager().collectRowLocks(branchSession);
    }

    @Override
    public void updateLockStatus(String xid, LockStatus lockStatus) throws TransactionException {
        LockerManagerFactory.getLockManager().updateLockStatus(xid, lockStatus);
    }
}
