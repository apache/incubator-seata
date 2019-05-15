package io.seata.core.lock;

import io.seata.core.exception.TransactionException;

/**
 * The interface Locker.
 *
 * @author zhangsen
 * @data 2019 -05-15
 */
public interface Locker {

    /**
     * Acquire lock boolean.
     *
     * @param rowLock the row lock
     * @return the boolean
     */
    boolean acquireLock(RowLock... rowLock) ;

    /**
     * Un lock boolean.
     *
     * @param rowLock the row lock
     * @return the boolean
     */
    boolean unLock(RowLock... rowLock);

    /**
     * Is lockable boolean.
     *
     * @param xid        the xid
     * @param resourceId the resource id
     * @param lockKey    the lock key
     * @return the boolean
     */
    boolean isLockable(String xid, String resourceId, String lockKey);

    /**
     * Clean all locks boolean.
     *
     * @return the boolean
     */
    void cleanAllLocks();
}

