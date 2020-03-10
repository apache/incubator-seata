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
package io.seata.core.store.db.sql.lock;

import io.seata.common.exception.StoreException;
import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;

/**
 * the database lock store factory
 *
 * @author zhangchenghui.dev@gmail.com
 * @since 1.2.0
 */
public class LockStoreSqlFactory {

    private static final LockStoreSql LOCK_STORE_SQL = EnhancedServiceLoader.load(LockStoreSql.class,
        ConfigurationFactory.getInstance().getConfig(ConfigurationKeys.STORE_DB_TYPE));

    /**
     * get the lock store sql
     *
     * @return lock store sql, support mysql/oracle/h2/postgre/oceanbase
     */
    public static LockStoreSql getLogStoreSql() {
        if (LOCK_STORE_SQL == null) {
            throw new StoreException("there must be db type, support mysql/oracle/h2/postgre/oceanbase database.");
        }
        return LOCK_STORE_SQL;
    }

}
