package io.seata.core.store.db.lock;

import io.seata.common.loader.LoadLevel;

/**
 * the database lock store oceanbase sql
 *
 * @author zhangchenghui.dev@gmail.com
 * @since 1.2.0
 */
@LoadLevel(name = "oceanbase")
public class OceanbaseLockStoreSql extends AbstractLockStoreSql {

    /**
     * The constant INSERT_LOCK_SQL_OCEANBASE.
     */
    private static final String INSERT_LOCK_SQL_OCEANBASE = "insert into " + LOCK_TABLE_PLACE_HOLD + "(" + ALL_COLUMNS + ")" +
        "values (?, ?, ?, ?, ?, ?, ?, now(), now())";

    @Override
    public String getInsertLockSQL(String lockTable) {
        return INSERT_LOCK_SQL_OCEANBASE.replace(LOCK_TABLE_PLACE_HOLD, lockTable);
    }
}
