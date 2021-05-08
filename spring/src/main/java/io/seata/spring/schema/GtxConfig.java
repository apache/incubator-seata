package io.seata.spring.schema;

import java.util.Arrays;
import java.util.Objects;

import io.seata.tm.api.transaction.Propagation;

/**
 * The type gtx config
 *
 * @author xingfudeshi@gmail.com
 */
public class GtxConfig {
    private int timeoutMills;
    private String name;
    private Class<? extends Throwable>[] rollbackFor = new Class[0];
    private String[] rollbackForClassName = {};
    private Class<? extends Throwable>[] noRollbackFor = new Class[0];
    private String[] noRollbackForClassName = {};
    private Propagation propagation;
    private int lockRetryInternal;
    private int lockRetryTimes;
    private String scanPackage;
    private String pattern;

    public int getTimeoutMills() {
        return timeoutMills;
    }

    public void setTimeoutMills(int timeoutMills) {
        this.timeoutMills = timeoutMills;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Class<? extends Throwable>[] getRollbackFor() {
        return rollbackFor;
    }

    public void setRollbackFor(Class<? extends Throwable>[] rollbackFor) {
        this.rollbackFor = rollbackFor;
    }

    public String[] getRollbackForClassName() {
        return rollbackForClassName;
    }

    public void setRollbackForClassName(String[] rollbackForClassName) {
        this.rollbackForClassName = rollbackForClassName;
    }

    public Class<? extends Throwable>[] getNoRollbackFor() {
        return noRollbackFor;
    }

    public void setNoRollbackFor(Class<? extends Throwable>[] noRollbackFor) {
        this.noRollbackFor = noRollbackFor;
    }

    public String[] getNoRollbackForClassName() {
        return noRollbackForClassName;
    }

    public void setNoRollbackForClassName(String[] noRollbackForClassName) {
        this.noRollbackForClassName = noRollbackForClassName;
    }

    public Propagation getPropagation() {
        return propagation;
    }

    public void setPropagation(Propagation propagation) {
        this.propagation = propagation;
    }

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
        return "GtxConfig{" +
            "timeoutMills=" + timeoutMills +
            ", name='" + name + '\'' +
            ", rollbackFor=" + Arrays.toString(rollbackFor) +
            ", rollbackForClassName=" + Arrays.toString(rollbackForClassName) +
            ", noRollbackFor=" + Arrays.toString(noRollbackFor) +
            ", noRollbackForClassName=" + Arrays.toString(noRollbackForClassName) +
            ", propagation=" + propagation +
            ", lockRetryInternal=" + lockRetryInternal +
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
        GtxConfig gtxConfig = (GtxConfig) o;
        return Objects.equals(scanPackage, gtxConfig.scanPackage) && Objects.equals(pattern, gtxConfig.pattern);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scanPackage, pattern);
    }
}
