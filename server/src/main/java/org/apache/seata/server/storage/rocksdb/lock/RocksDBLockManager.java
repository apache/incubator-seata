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
package org.apache.seata.server.storage.rocksdb.lock;

import org.apache.seata.common.loader.LoadLevel;
import org.apache.seata.common.loader.Scope;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.lock.Locker;
import org.apache.seata.server.lock.AbstractLockManager;
import org.apache.seata.server.session.BranchSession;
import org.apache.seata.server.session.GlobalSession;
import org.apache.seata.server.storage.rocksdb.RocksDBLocalLocks;
import org.apache.seata.server.storage.rocksdb.RocksDBStoreEngine;
import org.apache.seata.server.storage.rocksdb.RocksDBStoreEngineFactory;

/**
 * RocksDB lock manager for file store engine.
 */
@LoadLevel(name = "rocksdb", scope = Scope.PROTOTYPE)
public class RocksDBLockManager extends AbstractLockManager {

    private final RocksDBLocker locker;

    public RocksDBLockManager() {
        this(RocksDBStoreEngineFactory.getInstance());
    }

    public RocksDBLockManager(RocksDBStoreEngine storeEngine) {
        this.locker = new RocksDBLocker(storeEngine, new RocksDBLocalLocks());
    }

    @Override
    public boolean releaseLock(BranchSession branchSession) throws TransactionException {
        try {
            return getLocker().releaseLock(branchSession.getXid(), branchSession.getBranchId());
        } catch (Exception t) {
            LOGGER.error("unLock error, xid {}, branchId:{}", branchSession.getXid(), branchSession.getBranchId(), t);
            return false;
        }
    }

    @Override
    public boolean releaseGlobalSessionLock(GlobalSession globalSession) throws TransactionException {
        try {
            return getLocker().releaseLock(globalSession.getXid());
        } catch (Exception t) {
            LOGGER.error("unLock globalSession error, xid:{}", globalSession.getXid(), t);
            return false;
        }
    }

    public int cleanOrphanLocks() {
        return locker.cleanOrphanLocks();
    }

    @Override
    protected Locker getLocker(BranchSession branchSession) {
        return locker;
    }
}
