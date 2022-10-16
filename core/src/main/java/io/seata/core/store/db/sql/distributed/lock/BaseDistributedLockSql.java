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
package io.seata.core.store.db.sql.distributed.lock;

import io.seata.core.constants.ServerTableColumnsName;

/**
 * @author chd
 */
public class BaseDistributedLockSql implements DistributedLockSql {
    protected static final String DISTRIBUTED_LOCK_TABLE_PLACE_HOLD = " #distributed_lock_table# ";

    protected static final String ALL_COLUMNS = ServerTableColumnsName.DISTRIBUTED_LOCK_KEY + "," +
            ServerTableColumnsName.DISTRIBUTED_LOCK_VALUE + "," + ServerTableColumnsName.DISTRIBUTED_LOCK_EXPIRE;

    protected static final String SELECT_FOR_UPDATE_SQL = "SELECT " + ALL_COLUMNS + " FROM " + DISTRIBUTED_LOCK_TABLE_PLACE_HOLD
            + " WHERE " + ServerTableColumnsName.DISTRIBUTED_LOCK_KEY + " = ? FOR UPDATE";

    protected static final String INSERT_DISTRIBUTED_LOCK_SQL = "INSERT INTO " + DISTRIBUTED_LOCK_TABLE_PLACE_HOLD + "("
            + ALL_COLUMNS + ") VALUES (?, ?, ?)";

    protected static final String UPDATE_DISTRIBUTED_LOCK_SQL = "UPDATE " + DISTRIBUTED_LOCK_TABLE_PLACE_HOLD + " SET "
            + ServerTableColumnsName.DISTRIBUTED_LOCK_VALUE + "=?, " + ServerTableColumnsName.DISTRIBUTED_LOCK_EXPIRE + "=?"
            + " WHERE " + ServerTableColumnsName.DISTRIBUTED_LOCK_KEY + "=?";


    @Override
    public String getSelectDistributeForUpdateSql(String distributedLockTable) {
        return SELECT_FOR_UPDATE_SQL.replace(DISTRIBUTED_LOCK_TABLE_PLACE_HOLD, distributedLockTable);
    }

    @Override
    public String getInsertSql(String distributedLockTable) {
        return INSERT_DISTRIBUTED_LOCK_SQL.replace(DISTRIBUTED_LOCK_TABLE_PLACE_HOLD, distributedLockTable);
    }

    @Override
    public String getUpdateSql(String distributedLockTable) {
        return UPDATE_DISTRIBUTED_LOCK_SQL.replace(DISTRIBUTED_LOCK_TABLE_PLACE_HOLD, distributedLockTable);
    }
}
