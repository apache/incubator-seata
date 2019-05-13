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
package io.seata.server.lock.db;

import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.common.loader.LoadLevel;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.StringUtils;
import io.seata.core.constants.LockMode;
import io.seata.core.exception.TransactionException;
import io.seata.core.store.LockDO;
import io.seata.core.store.LockStore;
import io.seata.server.lock.AbstractLockManager;
import io.seata.server.session.BranchSession;

import javax.sql.DataSource;
import java.util.List;

/**
 * The type Data base lock manager.
 *
 * @author zhangsen
 * @data 2019 /4/25
 */
@LoadLevel(name = "db")
public class DataBaseLockManagerImpl extends AbstractLockManager {

    private LockStore lockStore;

    /**
     * Instantiates a new Data base lock manager.
     */
    public DataBaseLockManagerImpl() {
    }

    /**
     * Instantiates a new Data base lock manager.
     *
     * @param logStoreDataSource the log store data source
     */
    public DataBaseLockManagerImpl(DataSource logStoreDataSource){
        lockStore = EnhancedServiceLoader.load(LockStore.class, LockMode.DB.name(), new Class[]{DataSource.class}, new Object[]{logStoreDataSource});
    }

    /**
     * acquire Lock by store lock in db
     * @param branchSession the branch session
     * @return
     * @throws TransactionException
     */
    @Override
    public boolean acquireLock(BranchSession branchSession) throws TransactionException {
        String lockKey = branchSession.getLockKey();
        if (StringUtils.isNullOrEmpty(lockKey)) {
            //no lock
            return true;
        }
        //get locks of branch
        List<LockDO> locks = collectRowLocks(branchSession);
        if(CollectionUtils.isEmpty(locks)){
            //no lock
            return true;
        }
        try{
            return lockStore.acquireLock(locks);
        }catch(Exception t){
            LOGGER.error("AcquireLock error, branchSession:" + branchSession, t);
            return false;
        }
    }

    /**
     * release lock from db
     * @param branchSession the branch session
     * @return
     * @throws TransactionException
     */
    @Override
    public boolean unLock(BranchSession branchSession) throws TransactionException {
        //get locks of branch
        List<LockDO> locks = collectRowLocks(branchSession);
        if(CollectionUtils.isEmpty(locks)){
            //no lock
            return true;
        }
        try{
            return lockStore.unLock(locks);
        }catch(Exception t){
            LOGGER.error("unLock error, branchSession:" + branchSession, t);
            return false;
        }
    }

    /**
     * check lockable
     * @param xid the xid
     * @param resourceId    the resource id
     * @param lockKey       the lock key
     * @return
     * @throws TransactionException
     */
    @Override
    public boolean isLockable(String xid, String resourceId, String lockKey) throws TransactionException {
        List<LockDO> locks = collectRowLocks(lockKey, resourceId, xid);
        try{
            return lockStore.isLockable(locks);
        }catch(Exception t){
            LOGGER.error("isLockable error, xid:" + xid + ", resourceId:"+resourceId + ", lockKey:"+lockKey, t);
            return false;
        }
    }

    /**
     * Sets lock store.
     *
     * @param lockStore the lock store
     */
    public void setLockStore(LockStore lockStore) {
        this.lockStore = lockStore;
    }
}
