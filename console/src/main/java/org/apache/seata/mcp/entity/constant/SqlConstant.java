package org.apache.seata.mcp.entity.constant;

public class SqlConstant {

    public static final String GET_TABLE_NAME_SQL = "SELECT TABLE_NAME, TABLE_COMMENT FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = ? ";

    public static final String GET_SCHEMA_SQL = "SELECT COLUMN_NAME, DATA_TYPE, COLUMN_COMMENT FROM INFORMATION_SCHEMA.COLUMNS " +
            "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?";

    public static final String GET_UNDO_LOG_SQL = "SELECT rollback_info FROM undo_log WHERE";

    public static final String PARAM_BRANCH_ID_SQL = " branch_id = ?";

    public static final String PARAM_XID_SQL = " xid = ?";

    public static final String UNDO_LOG_STATUS_SQL = " log_status = ?";

    public static final String UNDO_LOG_CREATE_TIME_SQL = " log_created BETWEEN ? AND ?";

    public static final String UNDO_LOG_MODIFY_TIME_SQL = " log_modified BETWEEN ? AND ?";
}
