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
package io.seata.server.storage.db.lock;

import java.util.List;
import java.util.stream.Collectors;
import io.seata.common.exception.DataAccessException;
import io.seata.common.exception.StoreException;
import io.seata.common.holder.ObjectHolder;
import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.LambdaUtils;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.lock.AbstractLocker;
import io.seata.core.lock.RowLock;
import io.seata.core.model.LockStatus;
import io.seata.core.store.LockDO;
import io.seata.core.store.LockStore;
import io.seata.core.store.db.DataSourceProvider;
import io.seata.server.storage.r2dbc.lock.R2dbcLockStoreDataBaseDAO;
import org.springframework.context.ApplicationContext;

import static io.seata.common.Constants.OBJECT_KEY_SPRING_APPLICATION_CONTEXT;

/**
 * The type Data base locker.
 *
 * @author zhangsen
 */
public class DataBaseLocker extends AbstractLocker {

    private LockStore lockStore;


    /**
     * Instantiates a new Data base locker.
     *
     */
    public DataBaseLocker() {
        ApplicationContext applicationContext =
            (ApplicationContext)ObjectHolder.INSTANCE.getObject(OBJECT_KEY_SPRING_APPLICATION_CONTEXT);
        R2dbcLockStoreDataBaseDAO r2dbcLockStoreDataBaseDAO = null;
        try {
            r2dbcLockStoreDataBaseDAO = applicationContext.getBean(R2dbcLockStoreDataBaseDAO.class);
        } catch (Exception ignored) {
        }
        String datasourceType =
            ConfigurationFactory.getInstance().getConfig(ConfigurationKeys.STORE_DB_DATASOURCE_TYPE);
        lockStore = r2dbcLockStoreDataBaseDAO != null ? r2dbcLockStoreDataBaseDAO
            : new LockStoreDataBaseDAO(EnhancedServiceLoader.load(DataSourceProvider.class, datasourceType).provide());
    }

    @Override
    public boolean acquireLock(List<RowLock> locks) {
        return acquireLock(locks, true, false);
    }

    @Override
    public boolean acquireLock(List<RowLock> locks, boolean autoCommit, boolean skipCheckLock) {
        if (CollectionUtils.isEmpty(locks)) {
            // no lock
            return true;
        }
        try {
            List<LockDO> lockDOs = convertToLockDO(locks);
            if (lockDOs.size() > 1) {
                lockDOs =
                    lockDOs.parallelStream().filter(LambdaUtils.distinctByKey(LockDO::getRowKey)).collect(Collectors.toList());
            }
            return lockStore.acquireLock(lockDOs, autoCommit, skipCheckLock);
        } catch (StoreException e) {
            throw e;
        } catch (Exception t) {
            LOGGER.error("AcquireLock error, locks:{}", CollectionUtils.toString(locks), t);
            return false;
        }
    }

    @Override
    public boolean releaseLock(List<RowLock> locks) {
        if (CollectionUtils.isEmpty(locks)) {
            // no lock
            return true;
        }
        try {
            return lockStore.unLock(convertToLockDO(locks));
        } catch (StoreException e) {
            throw e;
        } catch (Exception t) {
            LOGGER.error("unLock error, locks:{}", CollectionUtils.toString(locks), t);
            return false;
        }
    }

    @Override
    public boolean releaseLock(String xid, Long branchId) {
        try {
            return lockStore.unLock(branchId);
        } catch (StoreException e) {
            throw e;
        } catch (Exception t) {
            LOGGER.error("unLock by branchId error, xid {}, branchId:{}", xid, branchId, t);
            return false;
        }
    }

    @Override
    public boolean releaseLock(String xid) {
        try {
            return lockStore.unLock(xid);
        } catch (StoreException e) {
            throw e;
        } catch (Exception t) {
            LOGGER.error("unLock by branchIds error, xid {}", xid, t);
            return false;
        }
    }

    @Override
    public boolean isLockable(List<RowLock> locks) {
        if (CollectionUtils.isEmpty(locks)) {
            // no lock
            return true;
        }
        try {
            return lockStore.isLockable(convertToLockDO(locks));
        } catch (DataAccessException e) {
            throw e;
        } catch (Exception t) {
            LOGGER.error("isLockable error, locks:{}", CollectionUtils.toString(locks), t);
            return false;
        }
    }

    @Override
    public void updateLockStatus(String xid, LockStatus lockStatus) {
        lockStore.updateLockStatus(xid, lockStatus);
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
