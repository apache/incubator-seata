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

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.common.util.StringUtils;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.lock.Locker;
import io.seata.core.store.StoreMode;
import io.seata.core.store.db.DataSourceGenerator;
import io.seata.server.lock.db.DataBaseLockManager;
import io.seata.server.session.BranchSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Lock manager factory.
 *
 * @author sharajava
 */
public class LockerFactory {

    /**
     * The constant LOGGER.
     */
    protected static final Logger LOGGER = LoggerFactory.getLogger(LockerFactory.class);

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
    protected static LockManager lockManager;

    /**
     * Get lock manager.
     *
     * @return the lock manager
     */
    public static final LockManager getLockManager() {
        if (lockManager == null) {
            if (StringUtils.equalsIgnoreCase(StoreMode.DB.name(), CONFIG.getConfig(ConfigurationKeys.STORE_MODE))) {
                lockManager = new DataBaseLockManager();
            } else {
                lockManager = new DefaultLockManager();
            }
        }
        return lockManager;
    }

    /**
     * Get lock manager.
     *
     * @param branchSession the branch session
     * @return the lock manager
     */
    public static final Locker get(BranchSession branchSession) {
        String storeMode = CONFIG.getConfig(ConfigurationKeys.STORE_MODE);
        if (StringUtils.equalsIgnoreCase(StoreMode.DB.name(), storeMode)) {
            if (lockerMap.get(storeMode) != null) {
                return lockerMap.get(storeMode);
            }
            //init dataSource
            String datasourceType = CONFIG.getConfig(ConfigurationKeys.STORE_DB_DATASOURCE_TYPE);
            DataSourceGenerator dataSourceGenerator = EnhancedServiceLoader.load(DataSourceGenerator.class,
                datasourceType);
            DataSource logStoreDataSource = dataSourceGenerator.generateDataSource();
            locker = EnhancedServiceLoader.load(Locker.class, storeMode, new Class[] {DataSource.class},
                new Object[] {logStoreDataSource});
            lockerMap.putIfAbsent(storeMode, locker);
        } else if (StringUtils.equalsIgnoreCase(StoreMode.FILE.name(), storeMode)) {
            locker = EnhancedServiceLoader.load(Locker.class, storeMode,
                new Class[] {BranchSession.class}, new Object[] {branchSession});
        } else {
            //other locker
            locker = EnhancedServiceLoader.load(Locker.class, storeMode);
        }
        return locker;
    }

}
