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
package org.apache.seata.integration.tx.api.fence.store.db.sql;

import org.apache.seata.integration.tx.api.fence.constant.CommonFenceConstant;

/**
 * TCC Fence Store Sqls
 *
 */
public class CommonFenceStoreSqls {

    private CommonFenceStoreSqls() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * The constant LOCAL_TCC_LOG_PLACEHOLD.
     */
    public static final String LOCAL_TCC_LOG_PLACEHOLD = " #local_tcc_log# ";

    /**
     * The constant PRAMETER_PLACEHOLD.
     * format: ?, ?, ?
     */
    public static final String PRAMETER_PLACEHOLD = " #PRAMETER_PLACEHOLD# ";

    /**
     * The constant INSERT_LOCAL_TCC_LOG.
     */
    protected static final String INSERT_LOCAL_TCC_LOG = "insert into " + LOCAL_TCC_LOG_PLACEHOLD
            + " (xid, branch_id, action_name, status, gmt_create, gmt_modified) "
            + " values (?, ?, ?, ?, ?, ?) ";

    /**
     * The constant QUERY_BY_BRANCH_ID_AND_XID.
     */
    protected static final String QUERY_BY_BRANCH_ID_AND_XID = "select xid, branch_id, status, gmt_create, gmt_modified "
            + "from " + LOCAL_TCC_LOG_PLACEHOLD
            + " where xid = ? and branch_id = ? for update";

    /**
     * The constant QUERY_END_STATUS_BY_DATE.
     */
    protected static final String QUERY_END_STATUS_BY_DATE = "select xid, branch_id, status, gmt_create, gmt_modified "
            + " from " + LOCAL_TCC_LOG_PLACEHOLD
            + " where  gmt_modified < ? "
            + " and status in (" + CommonFenceConstant.STATUS_COMMITTED + " , " + CommonFenceConstant.STATUS_ROLLBACKED + " , " + CommonFenceConstant.STATUS_SUSPENDED + ")";

    /**
     * used for oracle. eg: and ROWNUM <= 10
     */
    protected static final String ORACLE_QUERY_LIMIT = " and ROWNUM <= ? ";

    /**
     * used for mysql, pgsql and mariadb. eg: limit 10
     */
    protected static final String NONE_ORACLE_QUERY_LIMIT = " limit ? ";

    /**
     * The constant UPDATE_STATUS_BY_BRANCH_ID_AND_XID.
     */
    protected static final String UPDATE_STATUS_BY_BRANCH_ID_AND_XID = "update " + LOCAL_TCC_LOG_PLACEHOLD + " set status = ?, gmt_modified = ?"
            + " where xid = ? and  branch_id = ? and status = ? ";

    /**
     * The constant DELETE_BY_BRANCH_ID_AND_XID.
     */
    protected static final String DELETE_BY_BRANCH_ID_AND_XID = "delete from " + LOCAL_TCC_LOG_PLACEHOLD + " where xid = ? and  branch_id = ? ";

    /**
     * The constant DELETE_BY_BRANCH_ID_AND_XID.
     */
    protected static final String DELETE_BY_BRANCH_XIDS = "delete from " + LOCAL_TCC_LOG_PLACEHOLD + " where xid in (" + PRAMETER_PLACEHOLD + ")";


    /**
     * The constant DELETE_BY_DATE_AND_STATUS.
     */
    protected static final String DELETE_BY_DATE_AND_STATUS = "delete from " + LOCAL_TCC_LOG_PLACEHOLD
            + " where gmt_modified < ? "
            + " and status in (" + CommonFenceConstant.STATUS_COMMITTED + " , " + CommonFenceConstant.STATUS_ROLLBACKED + " , " + CommonFenceConstant.STATUS_SUSPENDED + ")";

    public static String getInsertLocalTCCLogSQL(String localTccTable) {
        return INSERT_LOCAL_TCC_LOG.replace(LOCAL_TCC_LOG_PLACEHOLD, localTccTable);
    }

    public static String getQuerySQLByBranchIdAndXid(String localTccTable) {
        return QUERY_BY_BRANCH_ID_AND_XID.replace(LOCAL_TCC_LOG_PLACEHOLD, localTccTable);
    }

    public static String getQueryEndStatusSQLByDate(String localTccTable, boolean isOracle) {
        StringBuilder querySQLTemplate = new StringBuilder(QUERY_END_STATUS_BY_DATE);
        if (isOracle) {
            querySQLTemplate.append(ORACLE_QUERY_LIMIT);
        } else {
            querySQLTemplate.append(NONE_ORACLE_QUERY_LIMIT);
        }
        return querySQLTemplate.toString().replace(LOCAL_TCC_LOG_PLACEHOLD, localTccTable);
    }

    public static String getUpdateStatusSQLByBranchIdAndXid(String localTccTable) {
        return UPDATE_STATUS_BY_BRANCH_ID_AND_XID.replace(LOCAL_TCC_LOG_PLACEHOLD, localTccTable);
    }

    public static String getDeleteSQLByBranchIdAndXid(String localTccTable) {
        return DELETE_BY_BRANCH_ID_AND_XID.replace(LOCAL_TCC_LOG_PLACEHOLD, localTccTable);
    }

    public static String getDeleteSQLByXids(String localTccTable, String paramsPlaceHolder) {
        return DELETE_BY_BRANCH_XIDS.replace(LOCAL_TCC_LOG_PLACEHOLD, localTccTable)
                .replace(PRAMETER_PLACEHOLD, paramsPlaceHolder);
    }

}
