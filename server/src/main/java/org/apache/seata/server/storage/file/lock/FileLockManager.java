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
package org.apache.seata.server.storage.file.lock;

import org.apache.seata.common.loader.LoadLevel;
import org.apache.seata.common.loader.Scope;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.lock.Locker;
import org.apache.seata.server.lock.AbstractLockManager;
import org.apache.seata.server.session.BranchSession;
import org.apache.seata.server.session.GlobalSession;
import org.apache.seata.server.storage.file.spi.FileLockStore;

/**
 * The type file lock manager.
 *
 */
@LoadLevel(name = "file", scope = Scope.PROTOTYPE)
public class FileLockManager extends AbstractLockManager {

    private final FileLockStore lockStore;

    protected FileLockManager() {
        this(new DefaultFileLockStore());
    }

    public FileLockManager(FileLockStore lockStore) {
        if (lockStore == null) {
            throw new IllegalArgumentException("lockStore must not be null");
        }
        this.lockStore = lockStore;
    }

    @Override
    public Locker getLocker(BranchSession branchSession) {
        return lockStore.getLocker(branchSession);
    }

    @Override
    public boolean releaseLock(BranchSession branchSession) throws TransactionException {
        return lockStore.releaseBranchLock(branchSession);
    }

    @Override
    public boolean releaseGlobalSessionLock(GlobalSession globalSession) throws TransactionException {
        return lockStore.releaseGlobalLock(globalSession);
    }
}
