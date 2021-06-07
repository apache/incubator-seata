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
package io.seata.server.raft.execute.lock;

import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.core.store.StoreMode;
import io.seata.server.lock.LockManager;
import io.seata.server.raft.execute.AbstractRaftMsgExecute;
import io.seata.server.session.GlobalSession;
import io.seata.server.session.SessionHolder;
import io.seata.server.storage.file.lock.FileLockManager;
import io.seata.server.storage.raft.RaftSessionSyncMsg;

/**
 * @author jianbin.chen
 */
public class ReleaseLockExecute extends AbstractRaftMsgExecute {

    private static final FileLockManager FILE_LOCK_MANAGER =
        (FileLockManager)EnhancedServiceLoader.load(LockManager.class, StoreMode.FILE.getName());

    public ReleaseLockExecute(RaftSessionSyncMsg sessionSyncMsg) {
        super(sessionSyncMsg);
    }

    @Override
    public Boolean execute(Object... args) throws Throwable {
        GlobalSession globalSession =
            SessionHolder.getRootSessionManager().findGlobalSession(sessionSyncMsg.getGlobalSession().getXid());
        if (globalSession != null) {
            logger.info("releaseGlobalSessionLock xid: {}", globalSession.getXid());
            return FILE_LOCK_MANAGER.releaseGlobalSessionLock(globalSession);
        }
        return false;
    }

}
