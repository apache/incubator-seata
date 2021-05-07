package io.seata.spring.schema;

/**
 * @author xingfudeshi@gmail.com
 * @date 2021/05/06
 */
public class GtxBean {
    private int timeoutMills;
    private String name;
    private String rollbackFor;
    private String rollbackForClassName;
    private String noRollbackFor;
    private String noRollbackForClassName;
    private String propagation;
    private int lockRetryInternal;
    private int lockRetryTimes;
    private String scanPackage;
    private String methodMatch;
    private String classMatch;

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

    public String getRollbackFor() {
        return rollbackFor;
    }

    public void setRollbackFor(String rollbackFor) {
        this.rollbackFor = rollbackFor;
    }

    public String getRollbackForClassName() {
        return rollbackForClassName;
    }

    public void setRollbackForClassName(String rollbackForClassName) {
        this.rollbackForClassName = rollbackForClassName;
    }

    public String getNoRollbackFor() {
        return noRollbackFor;
    }

    public void setNoRollbackFor(String noRollbackFor) {
        this.noRollbackFor = noRollbackFor;
    }

    public String getNoRollbackForClassName() {
        return noRollbackForClassName;
    }

    public void setNoRollbackForClassName(String noRollbackForClassName) {
        this.noRollbackForClassName = noRollbackForClassName;
    }

    public String getPropagation() {
        return propagation;
    }

    public void setPropagation(String propagation) {
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

    public String getMethodMatch() {
        return methodMatch;
    }

    public void setMethodMatch(String methodMatch) {
        this.methodMatch = methodMatch;
    }

    public String getClassMatch() {
        return classMatch;
    }

    public void setClassMatch(String classMatch) {
        this.classMatch = classMatch;
    }

    @Override
    public String toString() {
        return "GtxConfig{" +
            "timeoutMills=" + timeoutMills +
            ", name='" + name + '\'' +
            ", rollbackFor='" + rollbackFor + '\'' +
            ", rollbackForClassName='" + rollbackForClassName + '\'' +
            ", noRollbackFor='" + noRollbackFor + '\'' +
            ", noRollbackForClassName='" + noRollbackForClassName + '\'' +
            ", propagation='" + propagation + '\'' +
            ", lockRetryInternal=" + lockRetryInternal +
            ", lockRetryTimes=" + lockRetryTimes +
            ", scanPackage='" + scanPackage + '\'' +
            ", methodMatch='" + methodMatch + '\'' +
            ", classMatch='" + classMatch + '\'' +
            '}';
    }
}
