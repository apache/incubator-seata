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
package io.seata.spring.util;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import io.seata.common.util.CollectionUtils;
import io.seata.rm.tcc.remoting.parser.DubboUtil;
import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.aop.target.EmptyTargetSource;

/**
 * Proxy tools base on spring
 *
 * @author zhangsen
 */
public class SpringProxyUtils {
    private SpringProxyUtils() {

    }

    /**
     * Find target class class.
     *
     * @param proxy the proxy
     * @return the class
     * @throws Exception the exception
     */
    public static Class<?> findTargetClass(Object proxy) throws Exception {
        if (AopUtils.isAopProxy(proxy)) {
            Advised advised = getAdvisedSupport(proxy);
            if (AopUtils.isJdkDynamicProxy(proxy)) {
                TargetSource targetSource = advised.getTargetSource();
                return targetSource instanceof EmptyTargetSource ? getFirstInterfaceByAdvised(advised)
                    : targetSource.getTargetClass();
            }
            Object target = advised.getTargetSource().getTarget();
            return findTargetClass(target);
        } else {
            return proxy == null ? null : proxy.getClass();
        }
    }

    public static Class<?>[] findInterfaces(Object proxy) throws Exception {
        if (AopUtils.isJdkDynamicProxy(proxy)) {
            Advised advised = getAdvisedSupport(proxy);
            return getInterfacesByAdvised(advised);
        } else {
            return new Class<?>[]{};
        }
    }

    private static Class<?>[] getInterfacesByAdvised(Advised advised) {
        Class<?>[] interfaces = advised.getProxiedInterfaces();
        if (interfaces.length > 0) {
            return interfaces;
        } else {
            throw new IllegalStateException("Find the jdk dynamic proxy class that does not implement the interface");
        }
    }

    private static Class<?> getFirstInterfaceByAdvised(Advised advised) {
        Class<?>[] interfaces = advised.getProxiedInterfaces();
        if (interfaces.length > 0) {
            return interfaces[0];
        } else {
            throw new IllegalStateException("Find the jdk dynamic proxy class that does not implement the interface");
        }
    }

    /**
     * Gets advised support.
     *
     * @param proxy the proxy
     * @return the advised support
     * @throws Exception the exception
     */
    public static Advised getAdvisedSupport(Object proxy) throws Exception {
        return (Advised) proxy;
    }

    /**
     * Is proxy boolean.
     *
     * @param bean the bean
     * @return the boolean
     */
    public static boolean isProxy(Object bean) {
        if (bean == null) {
            return false;
        }
        //check dubbo proxy ?
        return DubboUtil.isDubboProxyName(bean.getClass().getName()) || (Proxy.class.isAssignableFrom(bean.getClass())
                || AopUtils.isAopProxy(bean));
    }

    /**
     * Get the target class , get the interface of its agent if it is a Proxy
     *
     * @param proxy the proxy
     * @return target interface
     * @throws Exception the exception
     */
    public static Class<?> getTargetInterface(Object proxy) throws Exception {
        if (proxy == null) {
            throw new java.lang.IllegalArgumentException("proxy can not be null");
        }

        //jdk proxy
        if (Proxy.class.isAssignableFrom(proxy.getClass())) {
            Proxy p = (Proxy)proxy;
            return p.getClass().getInterfaces()[0];
        }

        return getTargetClass(proxy);
    }

    /**
     * Get the class type of the proxy target object, if hadn't a target object, return the interface of the proxy
     *
     * @param proxy
     * @return
     * @throws Exception
     */
    protected static Class<?> getTargetClass(Object proxy) throws Exception {
        if (proxy == null) {
            throw new java.lang.IllegalArgumentException("proxy can not be null");
        }
        //not proxy
        if (!AopUtils.isAopProxy(proxy)) {
            return proxy.getClass();
        }
        Advised advisedSupport = getAdvisedSupport(proxy);
        Object target = advisedSupport.getTargetSource().getTarget();
        /*
         * the Proxy of sofa:reference has no target
         */
        if (target == null) {
            if (CollectionUtils.isNotEmpty(advisedSupport.getProxiedInterfaces())) {
                return advisedSupport.getProxiedInterfaces()[0];
            } else {
                return proxy.getClass();
            }
        } else {
            return getTargetClass(target);
        }
    }

    /**
     * get the all interfaces of bean, if the bean is null, then return empty array
     * @param bean
     * @return
     */
    public static Class<?>[] getAllInterfaces(Object bean) {
        Set<Class<?>> interfaces = new HashSet<>();
        if (bean != null) {
            Class<?> clazz = bean.getClass();
            while (!Object.class.getName().equalsIgnoreCase(clazz.getName())) {
                Class<?>[] clazzInterfaces = clazz.getInterfaces();
                interfaces.addAll(Arrays.asList(clazzInterfaces));
                clazz = clazz.getSuperclass();
            }
        }
        return interfaces.toArray(new Class[0]);
    }

}
