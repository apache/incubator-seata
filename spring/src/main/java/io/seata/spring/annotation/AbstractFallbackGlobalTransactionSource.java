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
package io.seata.spring.annotation;

import io.seata.spring.util.SpringProxyUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author chusen
 * @email chusen12@163.com
 */
public abstract class AbstractFallbackGlobalTransactionSource implements GlobalTransactionalSource {


    private final Object NO_TRANSACTION_ATTRIBUTE = new Object();

    /**
     * the cache
     */
    private final Map<Object, Object> attributeCache = new ConcurrentHashMap<>(1024);


    @Override
    public boolean existGlobalTransactionalAnnotation(Object bean) throws Exception {
        Set<Class<?>> classes = new LinkedHashSet<>();
        Class<?> serviceInterface = SpringProxyUtils.findTargetClass(bean);
        Class<?>[] interfacesIfJdk = SpringProxyUtils.findInterfaces(bean);

        classes.add(serviceInterface);
        classes.addAll(Arrays.stream(interfacesIfJdk).collect(Collectors.toSet()));

        Annotation annotation;
        for (Class<?> clazz : classes) {
            Method[] methods = ReflectionUtils.getAllDeclaredMethods(clazz);
            for (Method method : methods) {
                annotation = getGlobalTransactionalAnnotation(method, clazz);
                if (annotation != null) {
                    return true;
                }
            }
        }
        return false;
    }


    @Override
    public Annotation getGlobalTransactionalAnnotation(Method method, Class<?> targetClass) {
        // skip object' method
        if (method.getDeclaringClass() == Object.class) {
            return null;
        }

        Object cacheKey = getCacheKey(method, targetClass);
        Object cached = this.attributeCache.get(cacheKey);

        if (cached != null) {
            return cached != NO_TRANSACTION_ATTRIBUTE ? (Annotation) cached : null;
        } else {
            Annotation transactionAnnotation = computeGlobalTransactionAnnotation(method, targetClass);
            if (transactionAnnotation != null) {
                this.attributeCache.put(cacheKey, transactionAnnotation);
            } else {
                this.attributeCache.put(cacheKey, NO_TRANSACTION_ATTRIBUTE);
            }
            return transactionAnnotation;
        }
    }


    /**
     * find the GlobalTransaction and GlobalLock annotation
     * @param method
     * @param targetClass
     * @return
     */
    private Annotation computeGlobalTransactionAnnotation(Method method, Class<?> targetClass) {
        if (allowPublicMethodsOnly() && !Modifier.isPublic(method.getModifiers())) {
            return null;
        }

        Method specificMethod = AopUtils.getMostSpecificMethod(method, targetClass);

        Annotation annotation = findTransactionAnnotation(specificMethod);
        if (annotation != null) {
            return annotation;
        }


        annotation = findTransactionAnnotation(specificMethod.getDeclaringClass());
        if (annotation != null && ClassUtils.isUserLevelMethod(specificMethod)) {
            return annotation;
        }

        if (specificMethod != method) {
            annotation = findTransactionAnnotation(method);
            if (annotation != null) {
                return annotation;
            }

            annotation = findTransactionAnnotation(method.getDeclaringClass());
            if (annotation != null && ClassUtils.isUserLevelMethod(method)) {
                return annotation;
            }
        }

        return null;
    }

    /**
     * find global transaction annotation on the method
     *
     * @param method
     * @return
     */
    protected abstract Annotation findTransactionAnnotation(Method method);


    /**
     * find global transaction annotation on the target class
     *
     * @param targetClass
     * @return
     */
    protected abstract Annotation findTransactionAnnotation(Class<?> targetClass);


    private Object getCacheKey(Method method, Class<?> targetClass) {
        return new MethodClassKey(method, targetClass);
    }


    private boolean allowPublicMethodsOnly() {
        return true;
    }
}
