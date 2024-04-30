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
package org.apache.seata.core.store.db.sql.lock;

import org.apache.seata.common.loader.LoadLevel;

/**
 * the database lock store ms-sqlserver sql
 *
 */
@LoadLevel(name = "sqlserver")
public class SqlServerLockStoreSql extends AbstractLockStoreSql {

    /**
     * The constant INSERT_LOCK_SQL_MYSQL.
     */
    private static final String INSERT_LOCK_SQL_SQLSERVER = "insert into " + LOCK_TABLE_PLACE_HOLD + "(" + ALL_COLUMNS + ")"
            + " values (?, ?, ?, ?, ?, ?, ?, SYSDATETIME(), SYSDATETIME(), ?)";

    /**
     * The constant QUERY_ALL_LOCK.
     */
    private static final String QUERY_ALL_LOCK_SQLSERVER = "select " + ALL_COLUMNS + " from " + LOCK_TABLE_PLACE_HOLD
            + WHERE_PLACE_HOLD;

    @Override
    public String getInsertLockSQL(String lockTable) {
        return INSERT_LOCK_SQL_SQLSERVER.replace(LOCK_TABLE_PLACE_HOLD, lockTable);
    }

    @Override
    public String getAllLockSql(String lockTable, String whereCondition) {
        return QUERY_ALL_LOCK_SQLSERVER.replace(LOCK_TABLE_PLACE_HOLD, lockTable).replace(WHERE_PLACE_HOLD, whereCondition);
    }
}
