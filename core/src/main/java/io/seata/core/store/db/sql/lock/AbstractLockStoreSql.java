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

import java.util.ArrayList;
import java.util.List;

import io.seata.common.exception.NotSupportYetException;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.constants.ServerTableColumnsName;


/**
 * the database abstract lock store sql interface
 *
 * @author zhangchenghui.dev@gmail.com
 * @since 1.2.0
 */
public class AbstractLockStoreSql implements LockStoreSql {

    private static final int MAX_IN_SIZE = 1000;

    /**
     * The constant CONFIG.
     */
    protected static final Configuration CONFIG = ConfigurationFactory.getInstance();

    /**
     * The constant LOCK_TABLE_PLACE_HOLD.
     */
    protected static final String LOCK_TABLE_PLACE_HOLD = " #lock_table# ";
    /**
     * The constant WHERE_PLACE_HOLD
     */
    protected static final String WHERE_PLACE_HOLD = " #where# ";
    /**
     * The constant IN_PARAMS_PLACE_HOLD.
     */
    protected static final String IN_PARAMS_PLACE_HOLD = " #in_params# ";

    /**
     * The constant LOCK_TABLE_PK_WHERE_CONDITION_PLACE_HOLD.
     */
    protected static final String LOCK_TABLE_PK_WHERE_CONDITION_PLACE_HOLD = " #lock_table_pk_where_condition# ";

    /**
     * The constant LOCK_TABLE_BRANCH_ID_WHERE_CONDITION_PLACE_HOLD.
     */
    protected static final String LOCK_TABLE_BRANCH_ID_WHERE_CONDITION_PLACE_HOLD = " #lock_table_branch_id_where_condition# ";


    /**
     * The constant ALL_COLUMNS.
     * xid, transaction_id, branch_id, resource_id, table_name, pk, row_key, gmt_create, gmt_modified
     */
    protected static final String ALL_COLUMNS =
        ServerTableColumnsName.LOCK_TABLE_XID + ", " + ServerTableColumnsName.LOCK_TABLE_TRANSACTION_ID + ", "
            + ServerTableColumnsName.LOCK_TABLE_BRANCH_ID + ", " + ServerTableColumnsName.LOCK_TABLE_RESOURCE_ID + ", "
            + ServerTableColumnsName.LOCK_TABLE_TABLE_NAME + ", " + ServerTableColumnsName.LOCK_TABLE_PK + ", "
            + ServerTableColumnsName.LOCK_TABLE_ROW_KEY + ", " + ServerTableColumnsName.LOCK_TABLE_GMT_CREATE + ", "
            + ServerTableColumnsName.LOCK_TABLE_GMT_MODIFIED + "," + ServerTableColumnsName.LOCK_TABLE_STATUS;

    /**
     * The constant DELETE_LOCK_SQL.
     */
    private static final String DELETE_LOCK_SQL = "delete from " + LOCK_TABLE_PLACE_HOLD
        + " where " + ServerTableColumnsName.LOCK_TABLE_ROW_KEY + " = ? and " + ServerTableColumnsName.LOCK_TABLE_XID + " = ?";

    /**
     * The constant BATCH_DELETE_LOCK_SQL.
     */
    private static final String BATCH_DELETE_LOCK_SQL = "delete from " + LOCK_TABLE_PLACE_HOLD
        + " where " + ServerTableColumnsName.LOCK_TABLE_XID + " = ? and (" + LOCK_TABLE_PK_WHERE_CONDITION_PLACE_HOLD + ") ";

    /**
     * The constant BATCH_DELETE_LOCK_BY_BRANCH_SQL.
     */
    private static final String BATCH_DELETE_LOCK_BY_BRANCH_SQL = "delete from " + LOCK_TABLE_PLACE_HOLD
        + " where " + ServerTableColumnsName.LOCK_TABLE_XID + " = ? and " + ServerTableColumnsName.LOCK_TABLE_BRANCH_ID + " = ? ";

    /**
     * The constant BATCH_UPDATE_STATUS_LOCK_BY_GLOBAL_SQL.
     */
    private static final String BATCH_UPDATE_STATUS_LOCK_BY_GLOBAL_SQL = "update " + LOCK_TABLE_PLACE_HOLD + " set "
        + ServerTableColumnsName.LOCK_TABLE_STATUS + " = ? where " + ServerTableColumnsName.LOCK_TABLE_XID + " = ? ";

    /**
     * The constant BATCH_DELETE_LOCK_BY_BRANCHS_SQL.
     */
    private static final String BATCH_DELETE_LOCK_BY_BRANCHS_SQL = "delete from " + LOCK_TABLE_PLACE_HOLD
        + " where " + ServerTableColumnsName.LOCK_TABLE_XID + " = ? ";


    /**
     * The constant QUERY_LOCK_SQL.
     */
    private static final String QUERY_LOCK_SQL = "select " + ALL_COLUMNS + " from " + LOCK_TABLE_PLACE_HOLD
        + " where " + ServerTableColumnsName.LOCK_TABLE_ROW_KEY + " = ? ";

    /**
     * The constant CHECK_LOCK_SQL.
     */
    private static final String CHECK_LOCK_SQL = "select " + ALL_COLUMNS + " from " + LOCK_TABLE_PLACE_HOLD
        + " where " + LOCK_TABLE_PK_WHERE_CONDITION_PLACE_HOLD
        + " order by status desc ";

    /**
     * The constant QUERY_ALL_LOCK.
     */
    private static final String QUERY_ALL_LOCK = "select " + ALL_COLUMNS + " from " + LOCK_TABLE_PLACE_HOLD
            + WHERE_PLACE_HOLD + " order by gmt_create desc ";

    @Override
    public String getAllLockSql(String lockTable, String whereCondition) {
        return QUERY_ALL_LOCK.replace(LOCK_TABLE_PLACE_HOLD, lockTable).replace(WHERE_PLACE_HOLD, whereCondition);
    }

    @Override
    public String getInsertLockSQL(String lockTable) {
        throw new NotSupportYetException("unknown dbType:" + CONFIG.getConfig(ConfigurationKeys.STORE_DB_TYPE));
    }

    @Override
    public String getDeleteLockSql(String lockTable) {
        return DELETE_LOCK_SQL.replace(LOCK_TABLE_PLACE_HOLD, lockTable);
    }

    @Override
    public String getBatchDeleteLockSql(String lockTable, int rowSize) {
        List<String> pkNameList = new ArrayList<>();
        pkNameList.add(ServerTableColumnsName.LOCK_TABLE_ROW_KEY);
        String whereCondition = buildWhereConditionByPKs(pkNameList,rowSize,MAX_IN_SIZE);
        return BATCH_DELETE_LOCK_SQL.replace(LOCK_TABLE_PLACE_HOLD, lockTable).replace(LOCK_TABLE_PK_WHERE_CONDITION_PLACE_HOLD, whereCondition);
    }

    @Override
    public String getBatchDeleteLockSqlByBranch(String lockTable) {
        return BATCH_DELETE_LOCK_BY_BRANCH_SQL.replace(LOCK_TABLE_PLACE_HOLD, lockTable);
    }

    @Override
    public String getBatchDeleteLockSqlByXid(String lockTable) {
        return BATCH_DELETE_LOCK_BY_BRANCHS_SQL.replace(LOCK_TABLE_PLACE_HOLD, lockTable);
    }

    @Override
    public String getQueryLockSql(String lockTable) {
        return QUERY_LOCK_SQL.replace(LOCK_TABLE_PLACE_HOLD, lockTable);
    }

    @Override
    public String getCheckLockableSql(String lockTable, int rowSize) {
        List<String> pkNameList = new ArrayList<>();
        pkNameList.add(ServerTableColumnsName.LOCK_TABLE_ROW_KEY);
        String whereCondition = buildWhereConditionByPKs(pkNameList,rowSize,MAX_IN_SIZE);
        return CHECK_LOCK_SQL.replace(LOCK_TABLE_PLACE_HOLD, lockTable).replace(LOCK_TABLE_PK_WHERE_CONDITION_PLACE_HOLD, whereCondition);
    }

    @Override
    public String getBatchUpdateStatusLockByGlobalSql(String lockTable) {
        return BATCH_UPDATE_STATUS_LOCK_BY_GLOBAL_SQL.replace(LOCK_TABLE_PLACE_HOLD, lockTable);
    }

    /**
     * each pk is a condition.the result will like :" (id,userCode) in ((?,?),(?,?)) or (id,userCode) in ((?,?),(?,?)
     * ) or (id,userCode) in ((?,?))"
     * Build where condition by pks string.
     *
     * @param pkNameList pk column name list
     * @param rowSize    the row size of records
     * @param maxInSize  the max in size
     * @return return where condition sql string.the sql can search all related records not just one.
     */
    private String buildWhereConditionByPKs(List<String> pkNameList, int rowSize, int maxInSize) {
        StringBuilder whereStr = new StringBuilder();
        //we must consider the situation of composite primary key
        int batchSize = rowSize % maxInSize == 0 ? rowSize / maxInSize : (rowSize / maxInSize) + 1;
        for (int batch = 0; batch < batchSize; batch++) {
            if (batch > 0) {
                whereStr.append(" or ");
            }
            if (pkNameList.size() > 1) {
                whereStr.append("(");
            }
            for (int i = 0; i < pkNameList.size(); i++) {
                if (i > 0) {
                    whereStr.append(",");
                }
                whereStr.append(pkNameList.get(i));
            }
            if (pkNameList.size() > 1) {
                whereStr.append(")");
            }
            whereStr.append(" in ( ");

            int eachSize = (batch == batchSize - 1) ? (rowSize % maxInSize == 0 ? maxInSize : rowSize % maxInSize)
                : maxInSize;
            for (int i = 0; i < eachSize; i++) {
                //each row is a bracket
                if (i > 0) {
                    whereStr.append(",");
                }
                if (pkNameList.size() > 1) {
                    whereStr.append("(");
                }
                for (int x = 0; x < pkNameList.size(); x++) {
                    if (x > 0) {
                        whereStr.append(",");
                    }
                    whereStr.append("?");
                }
                if (pkNameList.size() > 1) {
                    whereStr.append(")");
                }
            }
            whereStr.append(" )");
        }

        return whereStr.toString();
    }


}
