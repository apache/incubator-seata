package io.seata.core.store.db.sql.lock;

import io.seata.common.loader.LoadLevel;

/**
 * the database lock store ms-sqlserver sql
 *
 * @author GoodBoyCoder
 */
@LoadLevel(name = "sqlserver")
public class SqlServerLockStoreSql extends AbstractLockStoreSql {

    /**
     * The constant INSERT_LOCK_SQL_MYSQL.
     */
    private static final String INSERT_LOCK_SQL_SQLSERVER = "insert into " + LOCK_TABLE_PLACE_HOLD + "(" + ALL_COLUMNS + ")"
            + " values (?, ?, ?, ?, ?, ?, ?, GETDATE(), GETDATE())";

    @Override
    public String getInsertLockSQL(String lockTable) {
        return INSERT_LOCK_SQL_SQLSERVER.replace(LOCK_TABLE_PLACE_HOLD, lockTable);
    }
}
