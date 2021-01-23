package io.seata.core.store.db.sql.distribute.lock;

/**
 * @author chd
 * @since 1.5.0
 */
public interface DistributeLockSql {
    /**
     * Get the select distribute lock sql
     * @param distributeLockTable the table name of the distribute lock table
     * @return the sql
     */
    String getSelectDistributeForUpdateSql(String distributeLockTable);

    /**
     * Get insert on duplicate key update distribute lock sql
     * @param distributeLockTable the table name of the distribute lock table
     * @return the sql
     */
    String getInsertOnDuplicateKeySql(String distributeLockTable);
}
