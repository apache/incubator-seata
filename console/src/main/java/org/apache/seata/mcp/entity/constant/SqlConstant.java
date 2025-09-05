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
package org.apache.seata.mcp.entity.constant;

/**
 *  ‘#’ is a placeholder for AND
 *  ‘%’ is a placeholder for Number Params
 */
public class SqlConstant {

    public static final String GET_TABLE_NAME_SQL =
            "SELECT TABLE_NAME, TABLE_COMMENT FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = ? ";

    public static final String GET_SCHEMA_SQL =
            "SELECT COLUMN_NAME, DATA_TYPE, COLUMN_COMMENT FROM INFORMATION_SCHEMA.COLUMNS "
                    + "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?";

    public static final String GET_UNDO_LOG_SQL =
            "SELECT rollback_info,context,log_status,log_created,log_modified FROM undo_log WHERE";

    public static final String PARAM_BRANCH_ID_SQL = " branch_id = ? #";

    public static final String PARAM_XID_SQL = " xid = ? #";

    public static final String UNDO_LOG_STATUS_SQL = " log_status = ? #";

    public static final String UNDO_LOG_CREATE_TIME_SQL = " log_created BETWEEN ? AND ?";

    public static final String UNDO_LOG_MODIFY_TIME_SQL = " log_modified BETWEEN ? AND ?";

    public static final String UNDO_LOG_ORDER = " ORDER BY log_status";

    public static final int MAX_OFFSET_THRESHOLD = 10000;

    public static final int MAX_PAGE_SIZE = 100;

    public static final String PAGE_QUERY = " LIMIT % OFFSET %";

    public static final String WHERE_SQL = " WHERE";
}
