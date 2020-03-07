package io.seata.core.store.db.lock;

import io.seata.common.loader.LoadLevel;

/**
 * the database lock store oracle sql
 *
 * @author zhangchenghui.dev@gmail.com
 * @since 1.2.0
 */
@LoadLevel(name = "oracle")
public class OracleLockStoreSql extends AbstractLockStoreSql {

    /**
     * The constant INSERT_LOCK_SQL_ORACLE.
     */
    private static final String INSERT_LOCK_SQL_ORACLE = "insert into " + LOCK_TABLE_PLACE_HOLD + "(" + ALL_COLUMNS + ")"
        +
        "values (?, ?, ?, ?, ?, ?, ?, sysdate, sysdate)";

    @Override
    public String getInsertLockSQL(String lockTable) {
        return INSERT_LOCK_SQL_ORACLE.replace(LOCK_TABLE_PLACE_HOLD, lockTable);
    }

}
