package io.seata.core.lock;

import io.seata.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * The type Abstract locker.
 *
 * @author zhangsen
 * @data 2019 -05-15
 */
public abstract class AbstractLocker implements Locker {

    /**
     * The constant LOGGER.
     */
    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractLocker.class);

    /**
     * The constant LOCK_SPLIT.
     */
    protected static final String LOCK_SPLIT = "^^^";

    /**
     * Collect row locks list.
     *
     * @param lockKey       the lock key
     * @param resourceId    the resource id
     * @param xid           the xid
     * @param transactionId the transaction id
     * @param branchID      the branch id
     * @return the list
     */
    protected List<RowLock> collectRowLocks(String lockKey, String resourceId, String xid, Long transactionId, Long branchID){
        List<RowLock> locks = new ArrayList<RowLock>();

        String[] tableGroupedLockKeys = lockKey.split(";");
        for (String tableGroupedLockKey : tableGroupedLockKeys) {
            int idx = tableGroupedLockKey.indexOf(":");
            if (idx < 0) {
                return locks;
            }
            String tableName = tableGroupedLockKey.substring(0, idx);
            String mergedPKs = tableGroupedLockKey.substring(idx + 1);
            if(StringUtils.isBlank(mergedPKs)){
                return locks;
            }
            String[] pks = mergedPKs.split(",");
            if(pks == null || pks.length == 0){
                return locks;
            }
            for(String pk : pks){
                if(StringUtils.isNotBlank(pk)){
                    RowLock rowLock = new RowLock();
                    rowLock.setXid(xid);
                    rowLock.setTransactionId(transactionId);
                    rowLock.setBranchId(branchID);
                    rowLock.setTableName(tableName);
                    rowLock.setPk(pk);
                    rowLock.setResourceId(resourceId);
                    rowLock.setRowKey(getRowKey(resourceId, tableName, pk));
                    locks.add(rowLock);
                }
            }
        }
        return locks;
    }

    /**
     * Get row key string.
     *
     * @param resourceId the resource id
     * @param tableName  the table name
     * @param pk         the pk
     * @return the string
     */
    protected String getRowKey(String resourceId, String tableName, String pk){
        return new StringBuilder().append(resourceId).append(LOCK_SPLIT).append(tableName).append(LOCK_SPLIT).append(pk).toString();
    }

    @Override
    public void cleanAllLocks() {

    }
}
