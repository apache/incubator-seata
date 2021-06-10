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
package io.seata.spring.proxy.desc;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;

import io.seata.common.util.ReflectionUtil;
import io.seata.spring.proxy.SeataProxy;
import io.seata.spring.proxy.SeataProxyBeanRegister;

/**
 * The SeataProxy Parser
 *
 * @author wang.liang
 * @see SeataProxyBeanDesc
 * @see SeataProxyMethodDesc
 */
public final class SeataProxyParser {

    private SeataProxyParser() {
    }


    /**
     * Phase bean desc of the target bean
     *
     * @param totalRegister the total register
     * @param bean          the bean
     * @param beanName      the bean name
     * @return the proxy bean desc
     */
    public static SeataProxyBeanDesc parserBeanDesc(SeataProxyBeanRegister totalRegister, @Nonnull Object bean, String beanName) {
        SeataProxyBeanDesc registerBeanDesc = totalRegister.getBeanClassBeanDescMap().get(bean.getClass());
        if (registerBeanDesc != null) {
            return registerBeanDesc;
        }

        registerBeanDesc = totalRegister.getBeanNameBeanDescMap().get(beanName);
        if (registerBeanDesc != null) {
            return registerBeanDesc;
        }

        return new SeataProxyBeanDesc(bean, beanName);
    }

    /**
     * Parser impl desc of the target bean class
     *
     * @param targetBeanClass the target bean class
     * @return the impl desc
     */
    public static SeataProxyImplementationDesc parserImplDesc(Class<?> targetBeanClass) {
        SeataProxy annotation = targetBeanClass.getAnnotation(SeataProxy.class);
        if (annotation == null) {
            throw new IllegalArgumentException("the targetBean has no annotation `@SeataProxy`");
        }
        return new SeataProxyImplementationDesc(annotation);
    }

    /**
     * Parser method desc map of the target bean class
     *
     * @param targetBeanClass the target bean class
     * @return the method desc map
     */
    public static Map<Method, SeataProxyMethodDesc> parserMethodDescMap(Class<?> targetBeanClass) {
        Map<Method, SeataProxyMethodDesc> methodDescMap = new HashMap<>();

        SeataProxyMethodDesc methodDesc;
        Method[] methods = targetBeanClass.getMethods();
        for (Method method : methods) {
            // ignore the static methods
            if (Modifier.isStatic(method.getModifiers())) {
                continue;
            }

            // ignore the synthetic methods
            if (method.isSynthetic()) {
                continue;
            }

            // ignore the methods of the Object class
            if (ReflectionUtil.hasMethod(Object.class, method)) {
                continue;
            }

            if (ReflectionUtil.containsMethod(methodDescMap, method)) {
                // ignore the method override in the child class
                continue;
            }

            // create the methodDesc and put to the map
            methodDesc = new SeataProxyMethodDesc(method);
            methodDescMap.put(method, methodDesc);
        }

        return methodDescMap;
    }

    public static String methodToString(Method method) {
        return method.getName() + ReflectionUtil.parameterTypesToString(method.getParameterTypes());
    }
}
