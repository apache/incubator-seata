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
package io.seata.server.storage.raft.lock;

import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.common.loader.LoadLevel;
import io.seata.core.exception.TransactionException;
import io.seata.core.lock.Locker;
import io.seata.core.store.BranchTransactionDO;
import io.seata.core.store.GlobalTransactionDO;
import io.seata.core.store.StoreMode;
import io.seata.server.lock.AbstractLockManager;
import io.seata.server.lock.LockManager;
import io.seata.server.raft.RaftServerFactory;
import io.seata.server.session.BranchSession;
import io.seata.server.session.GlobalSession;
import io.seata.server.storage.SessionConverter;
import io.seata.server.storage.file.lock.FileLocker;
import io.seata.server.storage.raft.RaftSyncMsg;
import io.seata.server.storage.raft.RaftTaskUtil;

import static io.seata.server.storage.raft.RaftSyncMsg.MsgType.ACQUIRE_LOCK;
import static io.seata.server.storage.raft.RaftSyncMsg.MsgType.RELEASE_GLOBAL_SESSION_LOCK;

/**
 * @author funkye
 */
@LoadLevel(name = "raft")
public class RaftLockManager extends AbstractLockManager {

    public static final LockManager LOCK_MANAGER =
        EnhancedServiceLoader.load(LockManager.class, StoreMode.FILE.getName());

    @Override
    public Locker getLocker(BranchSession branchSession) {
        return new FileLocker(branchSession);
    }

    @Override
    public boolean acquireLock(BranchSession branchSession) throws TransactionException {
        if (!RaftServerFactory.getInstance().isLeader()) {
            throw new TransactionException("this node is not a leader node, so requests are not allowed");
        }
        BranchTransactionDO branchTransactionDO = SessionConverter.convertBranchTransactionDO(branchSession);
        RaftSyncMsg raftSyncMsg = new RaftSyncMsg(ACQUIRE_LOCK, branchTransactionDO);
        RaftTaskUtil.createTask(raftSyncMsg);
        return LOCK_MANAGER.acquireLock(branchSession);
    }

    @Override
    public boolean releaseGlobalSessionLock(GlobalSession globalSession) throws TransactionException {
        if (!RaftServerFactory.getInstance().isLeader()) {
            throw new TransactionException("this node is not a leader node, so requests are not allowed");
        }
        GlobalTransactionDO globalTransactionDO = SessionConverter.convertGlobalTransactionDO(globalSession);
        RaftSyncMsg raftSyncMsg = new RaftSyncMsg(RELEASE_GLOBAL_SESSION_LOCK, globalTransactionDO);
        RaftTaskUtil.createTask(raftSyncMsg);
        return LOCK_MANAGER.releaseGlobalSessionLock(globalSession);
    }
}
