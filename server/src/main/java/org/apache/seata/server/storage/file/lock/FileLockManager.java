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

import java.util.List;

import org.apache.seata.common.loader.LoadLevel;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.lock.Locker;
import org.apache.seata.server.lock.AbstractLockManager;
import org.apache.seata.server.session.BranchSession;
import org.apache.seata.server.session.GlobalSession;
import org.apache.seata.server.storage.raft.lock.RaftLockManager;
import org.slf4j.MDC;

import static org.apache.seata.core.context.RootContext.MDC_KEY_BRANCH_ID;

/**
 * The type file lock manager.
 *
 */
@LoadLevel(name = "file")
public class FileLockManager extends AbstractLockManager {

    @Override
    public Locker getLocker(BranchSession branchSession) {
        return new FileLocker(branchSession);
    }

    @Override
    public boolean releaseGlobalSessionLock(GlobalSession globalSession) throws TransactionException {
        List<BranchSession> branchSessions = globalSession.getBranchSessions();
        boolean releaseLockResult = true;
        for (BranchSession branchSession : branchSessions) {
            try {
                MDC.put(MDC_KEY_BRANCH_ID, String.valueOf(branchSession.getBranchId()));
                releaseLockResult = this instanceof RaftLockManager ? super.releaseLock(branchSession)
                    : this.releaseLock(branchSession);
            } finally {
                MDC.remove(MDC_KEY_BRANCH_ID);
            }
        }
        return releaseLockResult;
    }

}
