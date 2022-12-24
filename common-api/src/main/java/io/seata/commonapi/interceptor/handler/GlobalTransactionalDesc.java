package io.seata.commonapi.interceptor.handler;

import io.seata.common.LockStrategyMode;
import io.seata.commonapi.annotation.GlobalTransactional;
import io.seata.tm.api.transaction.Propagation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Optional;

/**
 * @author leezongjie
 * @date 2022/12/23
 */
public class GlobalTransactionalDesc {

    private int timeoutMills;
    private String name;
    private Class<? extends Throwable>[] rollbackFor;
    private String[] rollbackForClassName;
    private Class<? extends Throwable>[] noRollbackFor;
    private String[] noRollbackForClassName;
    private Propagation propagation;
    private int lockRetryInterval;
    private int lockRetryTimes;
    private LockStrategyMode lockStrategyMode;

    private GlobalTransactionalDesc() {
    }

    public static GlobalTransactionalDesc getGlobalTransactionalAnnotationDesc(Method method, Class targetClass) {
        GlobalTransactional globalLockAnnotation = getAnnotation(method, targetClass, GlobalTransactional.class);
        if (globalLockAnnotation != null) {
            GlobalTransactionalDesc globalTransactionalDesc = new GlobalTransactionalDesc();
            globalTransactionalDesc.setTimeoutMills(globalLockAnnotation.timeoutMills());
            globalTransactionalDesc.setName(globalLockAnnotation.name());
            globalTransactionalDesc.setRollbackFor(globalLockAnnotation.rollbackFor());
            globalTransactionalDesc.setRollbackForClassName(globalLockAnnotation.rollbackForClassName());
            globalTransactionalDesc.setNoRollbackFor(globalLockAnnotation.noRollbackFor());
            globalTransactionalDesc.setNoRollbackForClassName(globalLockAnnotation.noRollbackForClassName());
            globalTransactionalDesc.setPropagation(globalLockAnnotation.propagation());
            globalTransactionalDesc.setLockRetryInterval(globalLockAnnotation.lockRetryInterval());
            globalTransactionalDesc.setLockRetryTimes(globalLockAnnotation.lockRetryTimes());
            globalTransactionalDesc.setLockStrategyMode(globalLockAnnotation.lockStrategyMode());
            return globalTransactionalDesc;
        }

        io.seata.spring.annotation.GlobalTransactional deprecatedGlobalLockAnnotation = getAnnotation(method, targetClass, io.seata.spring.annotation.GlobalTransactional.class);
        if (deprecatedGlobalLockAnnotation != null) {
            GlobalTransactionalDesc globalTransactionalDesc = new GlobalTransactionalDesc();
            globalTransactionalDesc.setTimeoutMills(deprecatedGlobalLockAnnotation.timeoutMills());
            globalTransactionalDesc.setName(deprecatedGlobalLockAnnotation.name());
            globalTransactionalDesc.setRollbackFor(deprecatedGlobalLockAnnotation.rollbackFor());
            globalTransactionalDesc.setRollbackForClassName(deprecatedGlobalLockAnnotation.rollbackForClassName());
            globalTransactionalDesc.setNoRollbackFor(deprecatedGlobalLockAnnotation.noRollbackFor());
            globalTransactionalDesc.setNoRollbackForClassName(deprecatedGlobalLockAnnotation.noRollbackForClassName());
            globalTransactionalDesc.setPropagation(deprecatedGlobalLockAnnotation.propagation());
            globalTransactionalDesc.setLockRetryInterval(deprecatedGlobalLockAnnotation.lockRetryInterval());
            globalTransactionalDesc.setLockRetryTimes(deprecatedGlobalLockAnnotation.lockRetryTimes());
            return globalTransactionalDesc;
        }
        return null;
    }

    public static <T extends Annotation> T getAnnotation(Method method, Class<?> targetClass, Class<T> annotationClass) {
        return Optional.ofNullable(method).map(m -> m.getAnnotation(annotationClass))
                .orElse(Optional.ofNullable(targetClass).map(t -> t.getAnnotation(annotationClass)).orElse(null));
    }


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

    public LockStrategyMode getLockStrategyMode() {
        return lockStrategyMode;
    }

    public void setLockStrategyMode(LockStrategyMode lockStrategyMode) {
        this.lockStrategyMode = lockStrategyMode;
    }
}
