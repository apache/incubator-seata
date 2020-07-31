package io.seata.core.model;

/**
 * @author selfishlover
 */
public class GlobalLockConfig {

    private int lockRetryInternal;

    private int lockRetryTimes;

    public int getLockRetryInternal() {
        return lockRetryInternal;
    }

    public void setLockRetryInternal(int lockRetryInternal) {
        this.lockRetryInternal = lockRetryInternal;
    }

    public int getLockRetryTimes() {
        return lockRetryTimes;
    }

    public void setLockRetryTimes(int lockRetryTimes) {
        this.lockRetryTimes = lockRetryTimes;
    }
}
