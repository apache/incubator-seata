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
package io.seata.core.store.db;

import io.seata.common.exception.NotSupportYetException;
import io.seata.core.constants.DBType;
import io.seata.core.constants.ServerTableColumnsName;

/**
 * The type Lock store sqls.
 *
 * @author zhangsen
 * @date 2019 /4/26
 */
public class LockStoreSqls {

    /**
     * The constant LOCK_TABLE_PLACEHOLD.
     */
    public static final String LOCK_TABLE_PLACEHOLD = " #lock_table# ";

    /**
     * The constant IN_PARAMS_PLACEHOLD.
     */
    public static final String IN_PARAMS_PLACEHOLD = " #in_params# ";

    /**
     * The constant ALL_COLUMNS.
     * xid, transaction_id, branch_id, resource_id, table_name, pk, row_key, gmt_create, gmt_modified
     */
    public static final String ALL_COLUMNS
        = ServerTableColumnsName.LOCK_TABLE_XID + ", " + ServerTableColumnsName.LOCK_TABLE_TRANSACTION_ID + ", "
        + ServerTableColumnsName.LOCK_TABLE_BRANCH_ID + ", " + ServerTableColumnsName.LOCK_TABLE_RESOURCE_ID + ", "
        + ServerTableColumnsName.LOCK_TABLE_TABLE_NAME + ", " + ServerTableColumnsName.LOCK_TABLE_PK + ", "
        + ServerTableColumnsName.LOCK_TABLE_ROW_KEY + ", " + ServerTableColumnsName.LOCK_TABLE_GMT_CREATE + ", "
        + ServerTableColumnsName.LOCK_TABLE_GMT_MODIFIED;

    /**
     * The constant INSERT_LOCK_SQL_MYSQL.
     */
    public static final String INSERT_LOCK_SQL_MYSQL = "insert into " + LOCK_TABLE_PLACEHOLD + "(" + ALL_COLUMNS + ")" +
        "values (?, ?, ?, ?, ?, ?, ?, now(), now())";

    /**
     * The constant INSERT_LOCK_SQL_ORACLE.
     */
    public static final String INSERT_LOCK_SQL_ORACLE = "insert into " + LOCK_TABLE_PLACEHOLD + "(" + ALL_COLUMNS + ")"
        +
        "values (?, ?, ?, ?, ?, ?, ?, sysdate, sysdate)";

    /**
     * The constant DELETE_LOCK_SQL.
     */
    public static final String DELETE_LOCK_SQL = "delete from " + LOCK_TABLE_PLACEHOLD
        + " where " + ServerTableColumnsName.LOCK_TABLE_ROW_KEY + " = ? and " + ServerTableColumnsName.LOCK_TABLE_XID + " = ?";

    /**
     * The constant BATCH_DELETE_LOCK_SQL.
     */
    public static final String BATCH_DELETE_LOCK_SQL = "delete from " + LOCK_TABLE_PLACEHOLD
        + " where " + ServerTableColumnsName.LOCK_TABLE_XID + " = ? and " + ServerTableColumnsName.LOCK_TABLE_ROW_KEY + " in (" + IN_PARAMS_PLACEHOLD + ") ";

    /**
     * The constant QUERY_LOCK_SQL.
     */
    public static final String QUERY_LOCK_SQL = "select " + ALL_COLUMNS + " from " + LOCK_TABLE_PLACEHOLD
        + " where " + ServerTableColumnsName.LOCK_TABLE_ROW_KEY + " = ? ";

    /**
     * The constant CHECK_LOCK_SQL.
     */
    public static final String CHECK_LOCK_SQL = "select " + ALL_COLUMNS + " from " + LOCK_TABLE_PLACEHOLD
        + " where " + ServerTableColumnsName.LOCK_TABLE_ROW_KEY + " in (" + IN_PARAMS_PLACEHOLD + ")";

    /**
     * Get insert lock sql string.
     *
     * @param lockTable the lock table
     * @param dbType    the db type
     * @return the string
     */
    public static String getInsertLockSQL(String lockTable, String dbType) {
        if (DBType.MYSQL.name().equalsIgnoreCase(dbType)
            || DBType.OCEANBASE.name().equalsIgnoreCase(dbType)
            || DBType.H2.name().equalsIgnoreCase(dbType)) {
            return INSERT_LOCK_SQL_MYSQL.replace(LOCK_TABLE_PLACEHOLD, lockTable);
        } else if (DBType.ORACLE.name().equalsIgnoreCase(dbType)) {
            return INSERT_LOCK_SQL_ORACLE.replace(LOCK_TABLE_PLACEHOLD, lockTable);
        } else {
            throw new NotSupportYetException("unknown dbType:" + dbType);
        }
    }

    /**
     * Get delete lock sql string.
     *
     * @param lockTable the lock table
     * @param dbType    the db type
     * @return the string
     */
    public static String getDeleteLockSql(String lockTable, String dbType) {
        return DELETE_LOCK_SQL.replace(LOCK_TABLE_PLACEHOLD, lockTable);
    }

    /**
     * Get batch delete lock sql string.
     *
     * @param lockTable      the lock table
     * @param paramPlaceHold the param place hold
     * @param dbType         the db type
     * @return the string
     */
    public static String getBatchDeleteLockSql(String lockTable, String paramPlaceHold, String dbType) {
        return BATCH_DELETE_LOCK_SQL.replace(LOCK_TABLE_PLACEHOLD, lockTable).replace(IN_PARAMS_PLACEHOLD,
            paramPlaceHold);
    }

    /**
     * Get query lock sql string.
     *
     * @param lockTable the lock table
     * @param dbType    the db type
     * @return the string
     */
    public static String getQueryLockSql(String lockTable, String dbType) {
        return QUERY_LOCK_SQL.replace(LOCK_TABLE_PLACEHOLD, lockTable);
    }

    /**
     * Get check lock sql string.
     *
     * @param lockTable      the lock table
     * @param paramPlaceHold the param place hold
     * @param dbType         the db type
     * @return the string
     */
    public static String getCheckLockableSql(String lockTable, String paramPlaceHold, String dbType) {
        return CHECK_LOCK_SQL.replace(LOCK_TABLE_PLACEHOLD, lockTable).replace(IN_PARAMS_PLACEHOLD, paramPlaceHold);
    }

}
