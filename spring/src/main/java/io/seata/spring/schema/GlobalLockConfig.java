package io.seata.spring.schema;

import java.util.Objects;

/**
 * The type global lock config
 * @author xingfudeshi@gmail.com
 */
public class GlobalLockConfig {
    private int lockRetryInterval = 0;
    private int lockRetryTimes = -1;
    private String scanPackage;
    private String pattern;

    public int getLockRetryInterval() {
        return lockRetryInterval;
    }

    public void setLockRetryInterval(int lockRetryInterval) {
        this.lockRetryInterval = lockRetryInterval;
    }

    public int getLockRetryTimes() {
        return lockRetryTimes;
    }

    public void setLockRetryTimes(int lockRetryTimes) {
        this.lockRetryTimes = lockRetryTimes;
    }

    public String getScanPackage() {
        return scanPackage;
    }

    public void setScanPackage(String scanPackage) {
        this.scanPackage = scanPackage;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    @Override
    public String toString() {
        return "GlobalLockConfig{" +
            "lockRetryInterval=" + lockRetryInterval +
            ", lockRetryTimes=" + lockRetryTimes +
            ", scanPackage='" + scanPackage + '\'' +
            ", pattern='" + pattern + '\'' +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        GlobalLockConfig that = (GlobalLockConfig) o;
        return lockRetryInterval == that.lockRetryInterval && lockRetryTimes == that.lockRetryTimes && Objects.equals(scanPackage, that.scanPackage) && Objects.equals(pattern, that.pattern);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lockRetryInterval, lockRetryTimes, scanPackage, pattern);
    }
}
