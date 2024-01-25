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
package org.apache.seata.server.transaction.at;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.seata.common.exception.StoreException;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.core.exception.BranchTransactionException;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.core.rpc.RemotingServer;
import org.apache.seata.server.coordinator.AbstractCore;
import org.apache.seata.server.session.BranchSession;
import org.apache.seata.server.session.GlobalSession;


import static org.apache.seata.common.Constants.AUTO_COMMIT;
import static org.apache.seata.common.Constants.SKIP_CHECK_LOCK;
import static org.apache.seata.core.exception.TransactionExceptionCode.LockKeyConflict;

/**
 * The type at core.
 *
 */
public class ATCore extends AbstractCore {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public ATCore(RemotingServer remotingServer) {
        super(remotingServer);
    }

    @Override
    public BranchType getHandleBranchType() {
        return BranchType.AT;
    }

    @Override
    protected void branchSessionLock(GlobalSession globalSession, BranchSession branchSession)
        throws TransactionException {
        String applicationData = branchSession.getApplicationData();
        boolean autoCommit = true;
        boolean skipCheckLock = false;
        if (StringUtils.isNotBlank(applicationData)) {
            try {
                Map<String, Object> data = objectMapper.readValue(applicationData, HashMap.class);
                Object clientAutoCommit = data.get(AUTO_COMMIT);
                if (clientAutoCommit != null && !(boolean)clientAutoCommit) {
                    autoCommit = (boolean)clientAutoCommit;
                }
                Object clientSkipCheckLock = data.get(SKIP_CHECK_LOCK);
                if (clientSkipCheckLock instanceof Boolean) {
                    skipCheckLock = (boolean)clientSkipCheckLock;
                }
            } catch (IOException e) {
                LOGGER.error("failed to get application data: {}", e.getMessage(), e);
            }
        }
        try {
            if (!branchSession.lock(autoCommit, skipCheckLock)) {
                throw new BranchTransactionException(LockKeyConflict,
                    String.format("Global lock acquire failed xid = %s branchId = %s", globalSession.getXid(),
                        branchSession.getBranchId()));
            }
        } catch (StoreException e) {
            Throwable cause = e.getCause();
            if (cause instanceof BranchTransactionException) {
                throw new BranchTransactionException(((BranchTransactionException)cause).getCode(),
                    String.format("Global lock acquire failed xid = %s branchId = %s", globalSession.getXid(),
                        branchSession.getBranchId()));
            }
            throw e;
        }
    }

    @Override
    protected void branchSessionUnlock(BranchSession branchSession) throws TransactionException {
        branchSession.unlock();
    }

    @Override
    public boolean lockQuery(BranchType branchType, String resourceId, String xid, String lockKeys)
            throws TransactionException {
        return lockManager.isLockable(xid, resourceId, lockKeys);
    }

}
