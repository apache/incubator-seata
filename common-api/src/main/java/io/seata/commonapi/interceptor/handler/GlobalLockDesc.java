package io.seata.commonapi.interceptor.handler;

import io.seata.commonapi.annotation.GlobalLock;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Optional;

/**
 * @author leezongjie
 * @date 2022/12/23
 */
public class GlobalLockDesc {
    private int lockRetryInterval;
    private int lockRetryTimes;

    private GlobalLockDesc() {
    }

    /**
     * compatibility for io.seata.spring.annotation.GlobalLock
     *
     * @param method
     * @param targetClass
     * @return
     */
    public static GlobalLockDesc getGlobalLockAnnotationDesc(Method method, Class targetClass) {
        GlobalLock globalLockAnnotation = getAnnotation(method, targetClass, GlobalLock.class);
        if (globalLockAnnotation != null) {
            GlobalLockDesc globalLockDesc = new GlobalLockDesc();
            globalLockDesc.setLockRetryInterval(globalLockAnnotation.lockRetryInterval());
            globalLockDesc.setLockRetryTimes(globalLockAnnotation.lockRetryTimes());
            return globalLockDesc;
        }

        io.seata.spring.annotation.GlobalLock deprecatedGlobalLockAnnotation = getAnnotation(method, targetClass, io.seata.spring.annotation.GlobalLock.class);
        if (deprecatedGlobalLockAnnotation != null) {
            GlobalLockDesc globalLockDesc = new GlobalLockDesc();
            globalLockDesc.setLockRetryInterval(deprecatedGlobalLockAnnotation.lockRetryInterval());
            globalLockDesc.setLockRetryTimes(deprecatedGlobalLockAnnotation.lockRetryTimes());
            return globalLockDesc;
        }
        return null;
    }

    public static <T extends Annotation> T getAnnotation(Method method, Class<?> targetClass, Class<T> annotationClass) {
        return Optional.ofNullable(method).map(m -> m.getAnnotation(annotationClass))
                .orElse(Optional.ofNullable(targetClass).map(t -> t.getAnnotation(annotationClass)).orElse(null));
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
}
