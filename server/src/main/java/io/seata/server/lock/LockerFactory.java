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

import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.lock.LockMode;
import io.seata.core.lock.Locker;
import io.seata.core.store.db.DataSourceGenerator;

import javax.sql.DataSource;

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

    protected static Locker locker = null;

    /**
     * Get lock manager.
     *
     * @return the lock manager
     */
    public static synchronized final Locker get() {
        if(locker != null){
            return locker;
        }
        String lockMode = CONFIG.getConfig(ConfigurationKeys.LOCK_MODE);
        if(LockMode.DB.name().equalsIgnoreCase(lockMode)){
            //init dataSource
            String datasourceType = CONFIG.getConfig(ConfigurationKeys.STORE_DB_DATASOURCE_TYPE);
            DataSourceGenerator dataSourceGenerator = EnhancedServiceLoader.load(DataSourceGenerator.class, datasourceType);
            DataSource logStoreDataSource = dataSourceGenerator.generateDataSource();
            locker = EnhancedServiceLoader.load(Locker.class, lockMode, new Object[]{logStoreDataSource});
        }else if(LockMode.MEMORY.name().equalsIgnoreCase(lockMode)){

        }else {
            locker = EnhancedServiceLoader.load(Locker.class, lockMode);
        }
        return locker;
    }


}
