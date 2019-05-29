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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.lock.LockMode;
import io.seata.core.lock.Locker;
import io.seata.core.store.db.DataSourceGenerator;
import io.seata.server.session.BranchSession;

/**
 * The type Lock manager factory.
 *
 * @author sharajava
 */
public class LockerFactory {

    /**
     * The constant CONFIG.
     */
    protected static final Configuration CONFIG = ConfigurationFactory.getInstance();

    /**
     * The constant locker.
     */
    protected static Locker locker = null;

    /**
     * The constant lockerMap.
     */
    protected static Map<String, Locker> lockerMap = new ConcurrentHashMap<>();

    /**
     * The constant lockManager.
     */
    protected static LockManager lockManager = new DefaultLockManager();

    /**
     * Get lock manager.
     *
     * @return the lock manager
     */
    public static synchronized final LockManager getLockManager() {
        return lockManager;
    }

    /**
     * Get lock manager.
     *
     * @param branchSession the branch session
     * @return the lock manager
     */
    public static synchronized final Locker get(BranchSession branchSession) {
        String lockMode = CONFIG.getConfig(ConfigurationKeys.LOCK_MODE);
        if (LockMode.DB.name().equalsIgnoreCase(lockMode)) {
            if (lockerMap.get(lockMode) != null) {
                return lockerMap.get(lockMode);
            }
            //init dataSource
            String datasourceType = CONFIG.getConfig(ConfigurationKeys.STORE_DB_DATASOURCE_TYPE);
            DataSourceGenerator dataSourceGenerator = EnhancedServiceLoader.load(DataSourceGenerator.class,
                datasourceType);
            DataSource logStoreDataSource = dataSourceGenerator.generateDataSource();
            locker = EnhancedServiceLoader.load(Locker.class, lockMode, new Class[] {DataSource.class},
                new Object[] {logStoreDataSource});
            lockerMap.put(lockMode, locker);
        } else if (LockMode.MEMORY.name().equalsIgnoreCase(lockMode)) {
            if (branchSession == null) {
                throw new IllegalArgumentException("branchSession can be null for memory lockMode.");
            }
            locker = EnhancedServiceLoader.load(Locker.class, lockMode,
                new Class[] {BranchSession.class}, new Object[] {branchSession});
        } else {
            //other locker
            locker = EnhancedServiceLoader.load(Locker.class, lockMode);
        }
        return locker;
    }

}
