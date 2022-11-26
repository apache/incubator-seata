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
package io.seata.server.storage.db;

import javax.sql.DataSource;
import io.seata.common.ConfigurationKeys;
import io.seata.common.JdbcConstants;
import io.seata.common.holder.ObjectHolder;
import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.config.ConfigurationFactory;
import io.seata.core.store.DistributedLocker;
import io.seata.core.store.LockStore;
import io.seata.core.store.LogStore;
import io.seata.core.store.db.DataSourceProvider;
import io.seata.server.storage.db.jdbc.lock.DataBaseDistributedLockerDAO;
import io.seata.server.storage.db.jdbc.lock.LockStoreDataBaseDAO;
import io.seata.server.storage.db.jdbc.store.LogStoreDataBaseDAO;
import io.seata.server.storage.db.r2dbc.lock.R2dbcDistributedLockerDAO;
import io.seata.server.storage.db.r2dbc.lock.R2dbcLockStoreDataBaseDAO;
import io.seata.server.storage.db.r2dbc.store.R2dbcLogStoreDataBaseDAO;
import org.springframework.context.ApplicationContext;

import static io.seata.common.Constants.OBJECT_KEY_SPRING_APPLICATION_CONTEXT;

public class DataBaseStoreFactory {

    public static LogStore getLogStore(String dbType) {
        if (JdbcConstants.MYSQL.equalsIgnoreCase(dbType)) {
            ApplicationContext applicationContext =
                (ApplicationContext)ObjectHolder.INSTANCE.getObject(OBJECT_KEY_SPRING_APPLICATION_CONTEXT);
            try {
                return applicationContext.getBean(R2dbcLogStoreDataBaseDAO.class);
            } catch (Exception ignored) {
            }
        }
        String datasourceType =
                ConfigurationFactory.getInstance().getConfig(ConfigurationKeys.STORE_DB_DATASOURCE_TYPE);
        // init dataSource
        DataSource logStoreDataSource =
                EnhancedServiceLoader.load(DataSourceProvider.class, datasourceType).provide();
        return new LogStoreDataBaseDAO(logStoreDataSource);
    }

    public static LockStore getLockStore(String dbType) {
        if (JdbcConstants.MYSQL.equalsIgnoreCase(dbType)) {
            ApplicationContext applicationContext =
                (ApplicationContext)ObjectHolder.INSTANCE.getObject(OBJECT_KEY_SPRING_APPLICATION_CONTEXT);
            try {
                return applicationContext.getBean(R2dbcLockStoreDataBaseDAO.class);
            } catch (Exception ignored) {
            }
        }
        String datasourceType =
            ConfigurationFactory.getInstance().getConfig(ConfigurationKeys.STORE_DB_DATASOURCE_TYPE);
        // init dataSource
        DataSource logStoreDataSource = EnhancedServiceLoader.load(DataSourceProvider.class, datasourceType).provide();
        return new LockStoreDataBaseDAO(logStoreDataSource);
    }

    public static DistributedLocker getDistributedLocker(String dbType) {
        if (JdbcConstants.MYSQL.equalsIgnoreCase(dbType)) {
            ApplicationContext applicationContext =
                (ApplicationContext)ObjectHolder.INSTANCE.getObject(OBJECT_KEY_SPRING_APPLICATION_CONTEXT);
            try {
                return applicationContext.getBean(R2dbcDistributedLockerDAO.class);
            } catch (Exception ignored) {
            }
        }
        return new DataBaseDistributedLockerDAO();
    }

}
