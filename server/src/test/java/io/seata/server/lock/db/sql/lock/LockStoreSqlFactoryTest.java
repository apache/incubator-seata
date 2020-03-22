package io.seata.core.store.db.sql.lock;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * the Lock Store Sql Factory Test
 *
 * @author zhangchenghui.dev@gmail.com
 * @since 1.2.0
 */
public class LockStoreSqlFactoryTest {

    private static LockStoreSql MYSQL_LOCK_STORE = LockStoreSqlFactory.getLogStoreSql("mysql");

    private static LockStoreSql ORACLE_LOCK_STORE = LockStoreSqlFactory.getLogStoreSql("oracle");

    private static LockStoreSql POSTGRESQL_LOCK_STORE = LockStoreSqlFactory.getLogStoreSql("postgresql");

    private static LockStoreSql H2_LOCK_STORE = LockStoreSqlFactory.getLogStoreSql("h2");

    private static LockStoreSql OCEANBASE_LOCK_STORE = LockStoreSqlFactory.getLogStoreSql("oceanbase");

    private static String GLOBAL_TABLE = "global_table";

    private static String BRANCH_TABLE = "branch_table";

    @Test
    public void mysqlLockTest() {
        String sql;
        // Get insert lock sql string.
        sql = MYSQL_LOCK_STORE.getInsertLockSQL(GLOBAL_TABLE);
        Assertions.assertNotNull(sql);
        sql = MYSQL_LOCK_STORE.getInsertLockSQL(BRANCH_TABLE);
        Assertions.assertNotNull(sql);

        // Get delete lock sql string.
        sql = MYSQL_LOCK_STORE.getDeleteLockSql(GLOBAL_TABLE);
        Assertions.assertNotNull(sql);
        sql = MYSQL_LOCK_STORE.getDeleteLockSql(BRANCH_TABLE);
        Assertions.assertNotNull(sql);

        // Get batch delete lock sql string.
        sql = MYSQL_LOCK_STORE.getBatchDeleteLockSql(GLOBAL_TABLE, "1");
        Assertions.assertNotNull(sql);
        sql = MYSQL_LOCK_STORE.getBatchDeleteLockSql(BRANCH_TABLE, "1");
        Assertions.assertNotNull(sql);

        // Get batch delete lock sql string.
        sql = MYSQL_LOCK_STORE.getBatchDeleteLockSqlByBranch(GLOBAL_TABLE);
        Assertions.assertNotNull(sql);
        sql = MYSQL_LOCK_STORE.getBatchDeleteLockSqlByBranch(BRANCH_TABLE);
        Assertions.assertNotNull(sql);

        // Get batch delete lock sql string.
        sql = MYSQL_LOCK_STORE.getBatchDeleteLockSqlByBranchs(GLOBAL_TABLE, "1");
        Assertions.assertNotNull(sql);
        sql = MYSQL_LOCK_STORE.getBatchDeleteLockSqlByBranchs(GLOBAL_TABLE, "1");
        Assertions.assertNotNull(sql);

        // Get query lock sql string.
        sql = MYSQL_LOCK_STORE.getQueryLockSql(GLOBAL_TABLE);
        Assertions.assertNotNull(sql);
        sql = MYSQL_LOCK_STORE.getQueryLockSql(BRANCH_TABLE);
        Assertions.assertNotNull(sql);

        // Get check lock sql string.
        sql = MYSQL_LOCK_STORE.getCheckLockableSql(GLOBAL_TABLE, "1");
        Assertions.assertNotNull(sql);
        sql = MYSQL_LOCK_STORE.getCheckLockableSql(BRANCH_TABLE, "1");
        Assertions.assertNotNull(sql);

    }

    @Test
    public void oracleLockTest() {
        String sql;
        // Get insert lock sql string.
        sql = ORACLE_LOCK_STORE.getInsertLockSQL(GLOBAL_TABLE);
        Assertions.assertNotNull(sql);
        sql = ORACLE_LOCK_STORE.getInsertLockSQL(BRANCH_TABLE);
        Assertions.assertNotNull(sql);

        // Get delete lock sql string.
        sql = ORACLE_LOCK_STORE.getDeleteLockSql(GLOBAL_TABLE);
        Assertions.assertNotNull(sql);
        sql = ORACLE_LOCK_STORE.getDeleteLockSql(BRANCH_TABLE);
        Assertions.assertNotNull(sql);

        // Get batch delete lock sql string.
        sql = ORACLE_LOCK_STORE.getBatchDeleteLockSql(GLOBAL_TABLE, "1");
        Assertions.assertNotNull(sql);
        sql = ORACLE_LOCK_STORE.getBatchDeleteLockSql(BRANCH_TABLE, "1");
        Assertions.assertNotNull(sql);

        // Get batch delete lock sql string.
        sql = ORACLE_LOCK_STORE.getBatchDeleteLockSqlByBranch(GLOBAL_TABLE);
        Assertions.assertNotNull(sql);
        sql = ORACLE_LOCK_STORE.getBatchDeleteLockSqlByBranch(BRANCH_TABLE);
        Assertions.assertNotNull(sql);

        // Get batch delete lock sql string.
        sql = ORACLE_LOCK_STORE.getBatchDeleteLockSqlByBranchs(GLOBAL_TABLE, "1");
        Assertions.assertNotNull(sql);
        sql = ORACLE_LOCK_STORE.getBatchDeleteLockSqlByBranchs(GLOBAL_TABLE, "1");
        Assertions.assertNotNull(sql);

        // Get query lock sql string.
        sql = ORACLE_LOCK_STORE.getQueryLockSql(GLOBAL_TABLE);
        Assertions.assertNotNull(sql);
        sql = ORACLE_LOCK_STORE.getQueryLockSql(BRANCH_TABLE);
        Assertions.assertNotNull(sql);

        // Get check lock sql string.
        sql = ORACLE_LOCK_STORE.getCheckLockableSql(GLOBAL_TABLE, "1");
        Assertions.assertNotNull(sql);
        sql = ORACLE_LOCK_STORE.getCheckLockableSql(BRANCH_TABLE, "1");
        Assertions.assertNotNull(sql);
    }

    @Test
    public void pgLockTest() {
        String sql;
        // Get insert lock sql string.
        sql = POSTGRESQL_LOCK_STORE.getInsertLockSQL(GLOBAL_TABLE);
        Assertions.assertNotNull(sql);
        sql = POSTGRESQL_LOCK_STORE.getInsertLockSQL(BRANCH_TABLE);
        Assertions.assertNotNull(sql);

        // Get delete lock sql string.
        sql = POSTGRESQL_LOCK_STORE.getDeleteLockSql(GLOBAL_TABLE);
        Assertions.assertNotNull(sql);
        sql = POSTGRESQL_LOCK_STORE.getDeleteLockSql(BRANCH_TABLE);
        Assertions.assertNotNull(sql);

        // Get batch delete lock sql string.
        sql = POSTGRESQL_LOCK_STORE.getBatchDeleteLockSql(GLOBAL_TABLE, "1");
        Assertions.assertNotNull(sql);
        sql = POSTGRESQL_LOCK_STORE.getBatchDeleteLockSql(BRANCH_TABLE, "1");
        Assertions.assertNotNull(sql);

        // Get batch delete lock sql string.
        sql = POSTGRESQL_LOCK_STORE.getBatchDeleteLockSqlByBranch(GLOBAL_TABLE);
        Assertions.assertNotNull(sql);
        sql = POSTGRESQL_LOCK_STORE.getBatchDeleteLockSqlByBranch(BRANCH_TABLE);
        Assertions.assertNotNull(sql);

        // Get batch delete lock sql string.
        sql = POSTGRESQL_LOCK_STORE.getBatchDeleteLockSqlByBranchs(GLOBAL_TABLE, "1");
        Assertions.assertNotNull(sql);
        sql = POSTGRESQL_LOCK_STORE.getBatchDeleteLockSqlByBranchs(GLOBAL_TABLE, "1");
        Assertions.assertNotNull(sql);

        // Get query lock sql string.
        sql = POSTGRESQL_LOCK_STORE.getQueryLockSql(GLOBAL_TABLE);
        Assertions.assertNotNull(sql);
        sql = POSTGRESQL_LOCK_STORE.getQueryLockSql(BRANCH_TABLE);
        Assertions.assertNotNull(sql);

        // Get check lock sql string.
        sql = POSTGRESQL_LOCK_STORE.getCheckLockableSql(GLOBAL_TABLE, "1");
        Assertions.assertNotNull(sql);
        sql = POSTGRESQL_LOCK_STORE.getCheckLockableSql(BRANCH_TABLE, "1");
        Assertions.assertNotNull(sql);
    }

    @Test
    public void h2LockTest() {
        String sql;
        // Get insert lock sql string.
        sql = H2_LOCK_STORE.getInsertLockSQL(GLOBAL_TABLE);
        Assertions.assertNotNull(sql);
        sql = H2_LOCK_STORE.getInsertLockSQL(BRANCH_TABLE);
        Assertions.assertNotNull(sql);

        // Get delete lock sql string.
        sql = H2_LOCK_STORE.getDeleteLockSql(GLOBAL_TABLE);
        Assertions.assertNotNull(sql);
        sql = H2_LOCK_STORE.getDeleteLockSql(BRANCH_TABLE);
        Assertions.assertNotNull(sql);

        // Get batch delete lock sql string.
        sql = H2_LOCK_STORE.getBatchDeleteLockSql(GLOBAL_TABLE, "1");
        Assertions.assertNotNull(sql);
        sql = H2_LOCK_STORE.getBatchDeleteLockSql(BRANCH_TABLE, "1");
        Assertions.assertNotNull(sql);

        // Get batch delete lock sql string.
        sql = H2_LOCK_STORE.getBatchDeleteLockSqlByBranch(GLOBAL_TABLE);
        Assertions.assertNotNull(sql);
        sql = H2_LOCK_STORE.getBatchDeleteLockSqlByBranch(BRANCH_TABLE);
        Assertions.assertNotNull(sql);

        // Get batch delete lock sql string.
        sql = H2_LOCK_STORE.getBatchDeleteLockSqlByBranchs(GLOBAL_TABLE, "1");
        Assertions.assertNotNull(sql);
        sql = H2_LOCK_STORE.getBatchDeleteLockSqlByBranchs(GLOBAL_TABLE, "1");
        Assertions.assertNotNull(sql);

        // Get query lock sql string.
        sql = H2_LOCK_STORE.getQueryLockSql(GLOBAL_TABLE);
        Assertions.assertNotNull(sql);
        sql = H2_LOCK_STORE.getQueryLockSql(BRANCH_TABLE);
        Assertions.assertNotNull(sql);

        // Get check lock sql string.
        sql = H2_LOCK_STORE.getCheckLockableSql(GLOBAL_TABLE, "1");
        Assertions.assertNotNull(sql);
        sql = H2_LOCK_STORE.getCheckLockableSql(BRANCH_TABLE, "1");
        Assertions.assertNotNull(sql);
    }

    @Test
    public void oceanbaseLockTest() {
        String sql;
        // Get insert lock sql string.
        sql = OCEANBASE_LOCK_STORE.getInsertLockSQL(GLOBAL_TABLE);
        Assertions.assertNotNull(sql);
        sql = OCEANBASE_LOCK_STORE.getInsertLockSQL(BRANCH_TABLE);
        Assertions.assertNotNull(sql);

        // Get delete lock sql string.
        sql = OCEANBASE_LOCK_STORE.getDeleteLockSql(GLOBAL_TABLE);
        Assertions.assertNotNull(sql);
        sql = OCEANBASE_LOCK_STORE.getDeleteLockSql(BRANCH_TABLE);
        Assertions.assertNotNull(sql);

        // Get batch delete lock sql string.
        sql = OCEANBASE_LOCK_STORE.getBatchDeleteLockSql(GLOBAL_TABLE, "1");
        Assertions.assertNotNull(sql);
        sql = OCEANBASE_LOCK_STORE.getBatchDeleteLockSql(BRANCH_TABLE, "1");
        Assertions.assertNotNull(sql);

        // Get batch delete lock sql string.
        sql = OCEANBASE_LOCK_STORE.getBatchDeleteLockSqlByBranch(GLOBAL_TABLE);
        Assertions.assertNotNull(sql);
        sql = OCEANBASE_LOCK_STORE.getBatchDeleteLockSqlByBranch(BRANCH_TABLE);
        Assertions.assertNotNull(sql);

        // Get batch delete lock sql string.
        sql = OCEANBASE_LOCK_STORE.getBatchDeleteLockSqlByBranchs(GLOBAL_TABLE, "1");
        Assertions.assertNotNull(sql);
        sql = OCEANBASE_LOCK_STORE.getBatchDeleteLockSqlByBranchs(GLOBAL_TABLE, "1");
        Assertions.assertNotNull(sql);

        // Get query lock sql string.
        sql = OCEANBASE_LOCK_STORE.getQueryLockSql(GLOBAL_TABLE);
        Assertions.assertNotNull(sql);
        sql = OCEANBASE_LOCK_STORE.getQueryLockSql(BRANCH_TABLE);
        Assertions.assertNotNull(sql);

        // Get check lock sql string.
        sql = OCEANBASE_LOCK_STORE.getCheckLockableSql(GLOBAL_TABLE, "1");
        Assertions.assertNotNull(sql);
        sql = OCEANBASE_LOCK_STORE.getCheckLockableSql(BRANCH_TABLE, "1");
        Assertions.assertNotNull(sql);
    }
}
