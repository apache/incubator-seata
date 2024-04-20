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


import org.apache.seata.core.constants.DBType;

public class DistributedLockSqlFactory {
    private static final DistributedLockSql DISTRIBUTED_LOCK_SQL = new BaseDistributedLockSql();
    private static final DistributedLockSql DISTRIBUTED_LOCK_SQL_SERVER = new BaseDistributedLockSqlServer();

    /**
     * get the lock store sql
     *
     * @param dbType the dbType, support mysql/oracle/h2/postgre/oceanbase/dm/sqlserver ...
     * @return lock store sql
     */
    public static DistributedLockSql getDistributedLogStoreSql(String dbType) {
        if (DBType.SQLSERVER.name().equalsIgnoreCase(dbType)) {
            return DISTRIBUTED_LOCK_SQL_SERVER;
        }
        return DISTRIBUTED_LOCK_SQL;
    }
}
