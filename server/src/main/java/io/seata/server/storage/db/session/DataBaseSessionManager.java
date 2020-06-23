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
package io.seata.server.storage.db.session;

import io.seata.common.exception.StoreException;
import io.seata.common.loader.LoadLevel;
import io.seata.common.loader.Scope;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.GlobalStatus;
import io.seata.server.session.AbstractSessionManager;
import io.seata.server.session.BranchSession;
import io.seata.server.session.GlobalSession;
import io.seata.server.storage.db.store.DataBaseTransactionStoreManager;
import io.seata.server.store.TransactionStoreManager.LogOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Data base session manager.
 *
 * @author zhangsen
 */
@LoadLevel(name = "db", scope = Scope.PROTOTYPE)
public class DataBaseSessionManager extends AbstractSessionManager {

    /**
     * The constant LOGGER.
     */
    protected static final Logger LOGGER = LoggerFactory.getLogger(DataBaseSessionManager.class);

    /**
     * Instantiates a new Data base session manager.
     */
    public DataBaseSessionManager() {
        super(DataBaseTransactionStoreManager.getInstance());
    }

    //region Override SessionManager

    @Override
    public void updateGlobalSession(GlobalSession session, GlobalStatus status) throws TransactionException {
        //new global session for update
        GlobalSession updateSession = new GlobalSession();
        updateSession.setXid(session.getXid());
        updateSession.setStatus(status);

        boolean ret = transactionStoreManager.writeSession(LogOperation.GLOBAL_UPDATE, updateSession);
        if (!ret) {
            throw new StoreException("updateGlobalSession failed: xid=" + session.getXid());
        }
    }

    @Override
    public void updateBranchSession(BranchSession branchSession, BranchStatus status,
                                    String applicationData) throws TransactionException {
        //new branch session for update
        BranchSession updateBranchSession = new BranchSession();
        updateBranchSession.setXid(branchSession.getXid());
        updateBranchSession.setBranchId(branchSession.getBranchId());
        updateBranchSession.setBranchType(branchSession.getBranchType());
        updateBranchSession.setStatus(status);
        updateBranchSession.setApplicationData(applicationData);

        boolean ret = transactionStoreManager.writeSession(LogOperation.BRANCH_UPDATE, updateBranchSession);
        if (!ret) {
            throw new StoreException("updateBranchSession failed: xid=" + branchSession.getXid() + " branchId=" + branchSession.getBranchId());
        }
    }

    //endregion
}
