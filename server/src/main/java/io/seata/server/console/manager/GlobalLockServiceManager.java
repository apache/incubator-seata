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
package io.seata.server.console.manager;

import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.common.util.StringUtils;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.store.StoreMode;
import io.seata.core.store.db.DataSourceProvider;
import io.seata.server.console.service.GlobalLockService;

import javax.sql.DataSource;

import static io.seata.common.DefaultValues.SERVER_DEFAULT_STORE_MODE;

/**
 * Global Lock Service Manager
 * @author: zhongxiang.wang
 */
public class GlobalLockServiceManager {
    /**
     * The global lock data source.
     */
    public static DataSource globalLockDataSource;

    private static final Configuration CONFIG = ConfigurationFactory.getInstance();

    protected static volatile GlobalLockService GLOBAL_LOCK_SERVICE;

    public static GlobalLockService getInstance() {
        return GLOBAL_LOCK_SERVICE;
    }

    public static DataSource getDataSource() {
        return globalLockDataSource;
    }

    public static void init(String lockMode) {
        if (GLOBAL_LOCK_SERVICE == null) {
            synchronized (GlobalLockServiceManager.class) {
                if (GLOBAL_LOCK_SERVICE == null) {
                    if (StringUtils.isBlank(lockMode)) {
                        lockMode = CONFIG.getConfig(ConfigurationKeys.STORE_LOCK_MODE,
                                CONFIG.getConfig(ConfigurationKeys.STORE_MODE, SERVER_DEFAULT_STORE_MODE));
                    }
                    if (StoreMode.contains(lockMode)) {
                        GLOBAL_LOCK_SERVICE = EnhancedServiceLoader.load(GlobalLockService.class, lockMode);
                    }
                }
            }
        }

        // init dataSource
        String datasourceType = ConfigurationFactory.getInstance().getConfig(ConfigurationKeys.STORE_DB_DATASOURCE_TYPE);
        globalLockDataSource = EnhancedServiceLoader.load(DataSourceProvider.class, datasourceType).provide();
    }
}
