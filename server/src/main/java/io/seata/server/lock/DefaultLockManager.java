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
package io.seata.server.lock;

import java.util.ArrayList;

import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.common.loader.LoadLevel;
import io.seata.core.exception.TransactionException;
import io.seata.core.lock.Locker;
import io.seata.core.store.StoreMode;
import io.seata.server.session.BranchSession;
import io.seata.server.session.GlobalSession;

/**
 * The type Default lock manager.
 *
 * @author zhangsen
 */
@LoadLevel(name = "file")
public class DefaultLockManager extends AbstractLockManager {

    @Override
    protected Locker getLocker(BranchSession branchSession) {
        locker = EnhancedServiceLoader.load(Locker.class, StoreMode.FILE.name(),
                new Class[] {BranchSession.class}, new Object[] {branchSession});
        return locker;
    }

    @Override
    public boolean releaseGlobalSessionLock(GlobalSession globalSession) throws TransactionException {
        ArrayList<BranchSession> branchSessions = globalSession.getBranchSessions();
        boolean releaseLockResult = true;
        for (BranchSession branchSession : branchSessions) {
            if (!this.releaseLock(branchSession)) {
                releaseLockResult = false;
            }
        }
        return releaseLockResult;
    }

}
