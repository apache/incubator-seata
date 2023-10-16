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

import com.google.common.collect.Maps;
import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.common.util.CollectionUtils;

import java.util.Map;

/**
 * the database lock store factory
 *
 * @author zhangchenghui.dev@gmail.com
 * @since 1.2.0
 */
public class LockStoreSqlFactory {

    private static Map<String/*dbType*/, LockStoreSql> LOCK_STORE_SQL_MAP = Maps.newConcurrentMap();

    /**
     * get the lock store sql
     *
     * @param dbType the dbType, support mysql/oracle/h2/postgre/oceanbase/dm
     * @return lock store sql
     */
    public static LockStoreSql getLogStoreSql(String dbType) {
        return CollectionUtils.computeIfAbsent(LOCK_STORE_SQL_MAP, dbType,
            key -> EnhancedServiceLoader.load(LockStoreSql.class, dbType.toLowerCase()));
    }
}
