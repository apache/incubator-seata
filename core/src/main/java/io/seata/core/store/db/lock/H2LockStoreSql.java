package io.seata.core.store.db.lock;

import io.seata.common.loader.LoadLevel;

/**
 * the database lock store H2 sql
 *
 * @author zhangchenghui.dev@gmail.com
 * @since 1.2.0
 */
@LoadLevel(name = "H2")
public class H2LockStoreSql extends AbstractLockStoreSql {

    /**
     * The constant INSERT_LOCK_SQL_H2.
     */
    private static final String INSERT_LOCK_SQL_H2 = "insert into " + LOCK_TABLE_PLACE_HOLD + "(" + ALL_COLUMNS + ")" +
        "values (?, ?, ?, ?, ?, ?, ?, now(), now())";

    @Override
    public String getInsertLockSQL(String lockTable) {
        return INSERT_LOCK_SQL_H2.replace(LOCK_TABLE_PLACE_HOLD, lockTable);
    }

}
