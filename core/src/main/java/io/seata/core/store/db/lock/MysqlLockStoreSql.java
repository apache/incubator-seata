package io.seata.core.store.db.lock;

import io.seata.common.loader.LoadLevel;

/**
 * the database lock store mysql sql
 *
 * @author zhangchenghui.dev@gmail.com
 * @since 1.2.0
 */
@LoadLevel(name = "mysql")
public class MysqlLockStoreSql extends AbstractLockStoreSql {

    /**
     * The constant INSERT_LOCK_SQL_MYSQL.
     */
    private static final String INSERT_LOCK_SQL_MYSQL = "insert into " + LOCK_TABLE_PLACE_HOLD + "(" + ALL_COLUMNS + ")" +
        "values (?, ?, ?, ?, ?, ?, ?, now(), now())";

    @Override
    public String getInsertLockSQL(String lockTable) {
        return INSERT_LOCK_SQL_MYSQL.replace(LOCK_TABLE_PLACE_HOLD, lockTable);
    }

}
