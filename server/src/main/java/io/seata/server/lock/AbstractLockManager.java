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

import io.seata.common.util.StringUtils;
import io.seata.core.exception.TransactionException;
import io.seata.core.store.LockDO;
import io.seata.server.session.BranchSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * The type Abstract lock manager.
 *
 * @author zhangsen
 * @data 2019 /4/25
 */
public abstract class AbstractLockManager implements LockManager {

    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractLockManager.class);


    /**
     * The constant LOCK_SPLIT.
     */
    protected static final String LOCK_SPLIT = "^^^";

    /**
     * Collect row locks list.`
     *
     * @param branchSession the branch session
     * @return the list
     */
    protected List<LockDO> collectRowLocks(BranchSession branchSession){
        List<LockDO> locks = new ArrayList<>();
        if(branchSession == null || StringUtils.isBlank(branchSession.getLockKey())){
            return locks;
        }
        String xid = branchSession.getXid();
        String resourceId = branchSession.getResourceId();
        long transactionId = branchSession.getTransactionId();

        String lockKey = branchSession.getLockKey();

        return collectRowLocks(lockKey, resourceId, xid, transactionId, branchSession.getBranchId());
    }

    protected List<LockDO> collectRowLocks(String lockKey, String resourceId, String xid) {
        return collectRowLocks(lockKey, resourceId, xid, null, null);
    }

    protected List<LockDO> collectRowLocks(String lockKey, String resourceId, String xid, Long transactionId, Long branchID){
        List<LockDO> locks = new ArrayList<LockDO>();

        String[] tableGroupedLockKeys = lockKey.split(";");
        for (String tableGroupedLockKey : tableGroupedLockKeys) {
            int idx = tableGroupedLockKey.indexOf(":");
            if (idx < 0) {
                return locks;
            }
            String tableName = tableGroupedLockKey.substring(0, idx);
            String mergedPKs = tableGroupedLockKey.substring(idx + 1);
            if(StringUtils.isBlank(mergedPKs)){
                return locks;
            }
            String[] pks = mergedPKs.split(",");
            if(pks == null || pks.length == 0){
                return locks;
            }
            for(String pk : pks){
                if(StringUtils.isNotBlank(pk)){
                    LockDO lockDO = new LockDO();
                    lockDO.setXid(xid);
                    lockDO.setTransactionId(transactionId);
                    lockDO.setBranchId(branchID);
                    lockDO.setTableName(tableName);
                    lockDO.setPk(pk);
                    lockDO.setResourceId(resourceId);
                    lockDO.setRowKey(getRowKey(resourceId, tableName, pk));
                    locks.add(lockDO);
                }
            }
        }
        return locks;
    }

    /**
     * Get row key string.
     *
     * @param resourceId the resource id
     * @param tableName  the table name
     * @param pk         the pk
     * @return the string
     */
    protected String getRowKey(String resourceId, String tableName, String pk){
        return new StringBuilder().append(resourceId).append(LOCK_SPLIT).append(tableName).append(LOCK_SPLIT).append(pk).toString();
    }

    @Override
    public void cleanAllLocks() throws TransactionException {

    }
}
