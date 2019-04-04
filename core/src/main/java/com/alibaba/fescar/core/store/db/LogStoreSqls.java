package com.alibaba.fescar.core.store.db;

import com.alibaba.fescar.common.exception.NotSupportYetException;
import com.alibaba.fescar.core.model.DBType;

/**
 * database log store SQLs
 *
 * @author zhangsen
 * @data 2019 /4/2
 */
public class LogStoreSqls {

    /**
     * The constant GLOBAL_TABLE_PLACEHOLD.
     */
    public static final String GLOBAL_TABLE_PLACEHOLD = " #global_table# ";

    /**
     * The constant ALL_GLOBAL_COLUMNS.
     */
    public static final String ALL_GLOBAL_COLUMNS = "xid, status, application_id, transaction_service_group, transaction_name, timeout, begin_time, active, gmt_create, gmt_modified ";

    /**
     * The constant ALL_BRANCH_COLUMNS.
     */
    protected static final String ALL_BRANCH_COLUMNS = "xid, branch_id, resource_group_id, resource_id, lock_key, branch_type, status, client_id, application_data";

    /**
     * The constant INSERT_GLOBAL_TRANSACTION_MYSQL.
     */
    public static final String INSERT_GLOBAL_TRANSACTION_MYSQL = "insert into " + GLOBAL_TABLE_PLACEHOLD + "("+ ALL_GLOBAL_COLUMNS +")" +
            "values(?, ?, ?, ?, ?, ?, ?, ?, now(), now()) ";

    /**
     * The constant INSERT_GLOBAL_TRANSACTION_ORACLE.
     */
    public static final String INSERT_GLOBAL_TRANSACTION_ORACLE = "insert into " + GLOBAL_TABLE_PLACEHOLD + "("+ ALL_GLOBAL_COLUMNS +")" +
            "values(?, ?, ?, ?, ?, ?, ?, ?, sysdate, sysdate) ";

    /**
     * The constant UPDATE_GLOBAL_TRANSACTION_STATUS_MYSQL.
     */
    public static final String UPDATE_GLOBAL_TRANSACTION_STATUS_MYSQL = "update "+ GLOBAL_TABLE_PLACEHOLD + "set status = ?, gmt_modified = now() where xid = ?";

    /**
     * The constant UPDATE_GLOBAL_TRANSACTION_STATUS_ORACLE.
     */
    public static final String UPDATE_GLOBAL_TRANSACTION_STATUS_ORACLE = "update "+ GLOBAL_TABLE_PLACEHOLD + "set status = ?, gmt_modified = sysdate where xid = ?";

    /**
     * The constant DELETE_GLOBAL_TRANSACTION.
     */
    public static final String DELETE_GLOBAL_TRANSACTION = "delete from " + GLOBAL_TABLE_PLACEHOLD + "where xid = ?";

    /**
     * The constant QUERY_GLOBAL_TRANSACTION.
     */
    public static final String QUERY_GLOBAL_TRANSACTION = "select "+ALL_GLOBAL_COLUMNS+" from " + GLOBAL_TABLE_PLACEHOLD + " where xid = ?";

    /**
     * The constant QUERY_GLOBAL_TRANSACTION_FOR_RECOVERY_MYSQL.
     */
    public static final String QUERY_GLOBAL_TRANSACTION_FOR_RECOVERY_MYSQL = "select "+ALL_GLOBAL_COLUMNS+" from " + GLOBAL_TABLE_PLACEHOLD + " where status in (" +
            "0, 2, 3, 4, 5, 6, 7, 8, 10 ,12, 14) order by gmt_modified limit ?";

    /**
     * The constant QUERY_GLOBAL_TRANSACTION_FOR_RECOVERY_ORACLE.
     */
    public static final String QUERY_GLOBAL_TRANSACTION_FOR_RECOVERY_ORACLE = "select A.* from ( select "+ALL_GLOBAL_COLUMNS+" from " + GLOBAL_TABLE_PLACEHOLD + " where status in (" +
            "0, 2, 3, 4, 5, 6, 7, 8, 10 ,12, 14) order by gmt_modified ) A where ROWNUM <= ?" ;


    /**
     * Get insert global transaction sql string.
     *
     * @param globalTable the global table
     * @param dbType      the db type
     * @return the string
     */
    public static String getInsertGlobalTransactionSQL(String globalTable, String dbType){
        if(DBType.MYSQL.name().equalsIgnoreCase(dbType)
                || DBType.OCEANBASE.name().equalsIgnoreCase(dbType)
                || DBType.H2.name().equalsIgnoreCase(dbType)){
            return INSERT_GLOBAL_TRANSACTION_MYSQL.replace(GLOBAL_TABLE_PLACEHOLD, globalTable);
        }else if(DBType.ORACLE.name().equalsIgnoreCase(dbType)){
            return INSERT_GLOBAL_TRANSACTION_ORACLE.replace(GLOBAL_TABLE_PLACEHOLD, globalTable);
        }else{
            throw new NotSupportYetException("unknown dbType:" + dbType);
        }
    }

    /**
     * Get update global transaction status sql string.
     *
     * @param globalTable the global table
     * @param dbType      the db type
     * @return the string
     */
    public static String getUpdateGlobalTransactionStatusSQL(String globalTable, String dbType){
        if(DBType.MYSQL.name().equalsIgnoreCase(dbType)
                || DBType.OCEANBASE.name().equalsIgnoreCase(dbType)
                || DBType.H2.name().equalsIgnoreCase(dbType)){
            return UPDATE_GLOBAL_TRANSACTION_STATUS_MYSQL.replace(GLOBAL_TABLE_PLACEHOLD, globalTable);
        }else if(DBType.ORACLE.name().equalsIgnoreCase(dbType)){
            return UPDATE_GLOBAL_TRANSACTION_STATUS_ORACLE.replace(GLOBAL_TABLE_PLACEHOLD, globalTable);
        }else{
            throw new NotSupportYetException("unknown dbType:" + dbType);
        }
    }

    /**
     * Get delete global transaction sql string.
     *
     * @param globalTable the global table
     * @param dbType      the db type
     * @return the string
     */
    public static String getDeleteGlobalTransactionSQL(String globalTable, String dbType){
        return DELETE_GLOBAL_TRANSACTION.replace(GLOBAL_TABLE_PLACEHOLD, globalTable );
    }

    /**
     * Get query global transaction sql string.
     *
     * @param globalTable the global table
     * @param dbType      the db type
     * @return the string
     */
    public static String getQueryGlobalTransactionSQL(String globalTable, String dbType){
        return QUERY_GLOBAL_TRANSACTION.replace(GLOBAL_TABLE_PLACEHOLD, globalTable);
    }

    /**
     * Get query global transaction for recovery sql string.
     *
     * @param globalTable the global table
     * @param dbType      the db type
     * @return the string
     */
    public static String getQueryGlobalTransactionForRecoverySQL(String globalTable, String dbType){
        if(DBType.MYSQL.name().equalsIgnoreCase(dbType)
                || DBType.OCEANBASE.name().equalsIgnoreCase(dbType)
                || DBType.H2.name().equalsIgnoreCase(dbType)){
            return QUERY_GLOBAL_TRANSACTION_FOR_RECOVERY_MYSQL.replace(GLOBAL_TABLE_PLACEHOLD, globalTable);
        }else if(DBType.ORACLE.name().equalsIgnoreCase(dbType)){
            return QUERY_GLOBAL_TRANSACTION_FOR_RECOVERY_ORACLE.replace(GLOBAL_TABLE_PLACEHOLD, globalTable);
        }else{
            throw new NotSupportYetException("unknown dbType:" + dbType);
        }
    }


}
