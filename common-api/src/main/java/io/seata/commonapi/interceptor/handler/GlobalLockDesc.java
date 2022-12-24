/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
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
