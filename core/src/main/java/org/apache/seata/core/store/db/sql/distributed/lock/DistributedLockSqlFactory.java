/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.core.store.db.sql.distributed.lock;


import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.common.loader.EnhancedServiceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DistributedLockSqlFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistributedLockSqlFactory.class);

    protected static Map<String, DistributedLockSql> distributedLockSqlCache = new ConcurrentHashMap<>(4);

    /**
     * get the lock store sql
     *
     * @param dbType the dbType, support mysql/oracle/h2/postgre/oceanbase/dm/sqlserver/oscar ...
     * @return lock store sql
     */
    public static DistributedLockSql getDistributedLogStoreSql(String dbType) {
        return distributedLockSqlCache.computeIfAbsent(dbType, method -> {
            try {
                return EnhancedServiceLoader.load(DistributedLockSql.class, dbType);
            } catch (EnhancedServiceNotFoundException ex) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Can't special implementation of DistributedLockSql for {}", dbType);
                }
            }
            return EnhancedServiceLoader.load(DistributedLockSql.class, "default");
        });
    }
}
