package io.seata.core.store.db.lock;

import com.google.common.collect.Maps;
import io.seata.common.loader.EnhancedServiceLoader;

import java.util.Map;

/**
 * the database lock store factory
 *
 * @author zhangchenghui.dev@gmail.com
 * @since 1.2.0
 */
public class LockStoreSqlFactory {

    private static Map<String/*dbType*/, LockStoreSql> LOCK_STORE_SQL_MAP = Maps.newConcurrentMap();

    /**
     * get the lock store sql
     *
     * @param dbType the dbType, support mysql/oracle/h2/postgre/oceanbase
     * @return lock store sql
     */
    public static LockStoreSql getLogStoreSql(String dbType) {
        if (LOCK_STORE_SQL_MAP.containsKey(dbType)) {
            LockStoreSql lockStoreSql = EnhancedServiceLoader.load(LockStoreSql.class, dbType.toLowerCase());
            LOCK_STORE_SQL_MAP.put(dbType, lockStoreSql);
        }
        return LOCK_STORE_SQL_MAP.get(dbType);
    }

}
