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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.StringJoiner;

import io.seata.common.exception.StoreException;
import io.seata.common.loader.LoadLevel;
import io.seata.core.store.LockDO;

/**
 * the database lock store mysql sql
 *
 * @author zhangchenghui.dev@gmail.com
 * @since 1.2.0
 */
@LoadLevel(name = "mysql")
public class MysqlLockStoreSql extends AbstractLockStoreSql {

    /**
     * The constant INSERT_LOCK_SQL_MYSQL_PREFIX.
     */
    private static final String INSERT_LOCK_SQL_MYSQL_PREFIX = "insert into " + LOCK_TABLE_PLACE_HOLD + "(" + ALL_COLUMNS + ")  values";

    /**
     * The constant INSERT_LOCK_SQL_MYSQL_SUFFIX.
     */
    private static final String INSERT_LOCK_SQL_MYSQL_SUFFIX = "  (?, ?, ?, ?, ?, ?, ?, now(), now())";

    @Override
    public String getInsertLockSQL(String lockTable) {
        return (INSERT_LOCK_SQL_MYSQL_PREFIX + INSERT_LOCK_SQL_MYSQL_SUFFIX).replace(LOCK_TABLE_PLACE_HOLD, lockTable);
    }

    @Override
    public String getBatchInsertLockSQL(String lockTable, int size) {
        StringJoiner insertLockSQL = new StringJoiner(",", INSERT_LOCK_SQL_MYSQL_PREFIX, "");
        for (int i = 0; i < size; i++) {
            insertLockSQL.add(INSERT_LOCK_SQL_MYSQL_SUFFIX);
        }
        return insertLockSQL.toString().replace(LOCK_TABLE_PLACE_HOLD, lockTable);
    }

    @Override
    public int executeBatchInsert(String lockTable, Connection conn, List<LockDO> lockDOs) {
        int length = lockDOs.size();
        int n;
        try (PreparedStatement ps = conn.prepareStatement(this.getBatchInsertLockSQL(lockTable, length))) {
            for (int i = 0; i < length; i++) {
                LockDO lockDO = lockDOs.get(i);
                n = i * 7;
                ps.setString(n + 1, lockDO.getXid());
                ps.setLong(n + 2, lockDO.getTransactionId());
                ps.setLong(n + 3, lockDO.getBranchId());
                ps.setString(n + 4, lockDO.getResourceId());
                ps.setString(n + 5, lockDO.getTableName());
                ps.setString(n + 6, lockDO.getPk());
                ps.setString(n + 7, lockDO.getRowKey());
            }
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new StoreException(e);
        }
    }
}
