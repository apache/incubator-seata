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
package io.seata.server.storage.db.store;

import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.store.db.DataSourceProvider;
import io.seata.server.store.AbstractTransactionStoreManager;
import io.seata.server.store.TransactionStoreManager;

import javax.sql.DataSource;

/**
 * The type Database transaction store manager.
 *
 * @author zhangsen
 */
public class DataBaseTransactionStoreManager extends AbstractTransactionStoreManager
        implements TransactionStoreManager {

    private static volatile DataBaseTransactionStoreManager instance;

    /**
     * Get the instance.
     */
    public static DataBaseTransactionStoreManager getInstance() {
        if (null == instance) {
            synchronized (DataBaseTransactionStoreManager.class) {
                if (null == instance) {
                    instance = new DataBaseTransactionStoreManager();
                }
            }
        }
        return instance;
    }

    /**
     * Instantiates a new Database transaction store manager.
     */
    private DataBaseTransactionStoreManager() {
        //init logQueryLimit
        super.initLogQueryLimit(ConfigurationKeys.STORE_DB_LOG_QUERY_LIMIT);

        //create dataSource
        String datasourceType = CONFIG.getConfig(ConfigurationKeys.STORE_DB_DATASOURCE_TYPE);
        DataSource logStoreDataSource = EnhancedServiceLoader.load(DataSourceProvider.class, datasourceType).provide();
        //init logStore
        super.logStore = new LogStoreDataBaseDAO(logStoreDataSource);
    }
}
