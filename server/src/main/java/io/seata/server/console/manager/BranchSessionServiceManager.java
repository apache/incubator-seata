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
import io.seata.server.console.service.BranchSessionService;

import javax.sql.DataSource;

import static io.seata.common.DefaultValues.SERVER_DEFAULT_STORE_MODE;

/**
 * Branch session Service Manager
 * @author: zhongxiang.wang
 */
public class BranchSessionServiceManager {
    /**
     * The branch session data source.
     */
    public static DataSource branchSessionDataSource;

    private static final Configuration CONFIG = ConfigurationFactory.getInstance();

    protected static volatile BranchSessionService BRANCH_SESSION_SERVICE;

    public static BranchSessionService getInstance() {
        return BRANCH_SESSION_SERVICE;
    }

    public static DataSource getDataSource() {
        return branchSessionDataSource;
    }

    public static void init(String sessionMode) {
        if (BRANCH_SESSION_SERVICE == null) {
            synchronized (BranchSessionServiceManager.class) {
                if (BRANCH_SESSION_SERVICE == null) {
                    if (StringUtils.isBlank(sessionMode)) {
                        sessionMode = CONFIG.getConfig(ConfigurationKeys.STORE_SESSION_MODE,
                                CONFIG.getConfig(ConfigurationKeys.STORE_MODE, SERVER_DEFAULT_STORE_MODE));
                    }
                    if (StoreMode.contains(sessionMode)) {
                        BRANCH_SESSION_SERVICE = EnhancedServiceLoader.load(BranchSessionService.class, sessionMode);
                    }
                }
            }
        }

        // init dataSource
        String datasourceType = ConfigurationFactory.getInstance().getConfig(ConfigurationKeys.STORE_DB_DATASOURCE_TYPE);
        branchSessionDataSource = EnhancedServiceLoader.load(DataSourceProvider.class, datasourceType).provide();
    }
}
