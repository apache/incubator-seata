/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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
package com.alibaba.fescar.spring.util;

import org.springframework.aop.framework.AdvisedSupport;
import org.springframework.aop.support.AopUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;

/**
 * Proxy tools base on spring
 *
 * @author zhangsen
 */
public class SpringProxyUtils {

    /**
     * Find target class class.
     *
     * @param proxy the proxy
     * @return the class
     * @throws Exception the exception
     */
    public static Class<?> findTargetClass(Object proxy) throws Exception {
        if (AopUtils.isAopProxy(proxy)) {
            AdvisedSupport advised = getAdvisedSupport(proxy);
            Object target = advised.getTargetSource().getTarget();
            return findTargetClass(target);
        } else {
            return proxy.getClass();
        }
    }

    /**
     * Gets advised support.
     *
     * @param proxy the proxy
     * @return the advised support
     * @throws Exception the exception
     */
    public static AdvisedSupport getAdvisedSupport(Object proxy) throws Exception {
        Field h;
        if (AopUtils.isJdkDynamicProxy(proxy)) {
            h = proxy.getClass().getSuperclass().getDeclaredField("h");
        } else {
            h = proxy.getClass().getDeclaredField("CGLIB$CALLBACK_0");
        }
        h.setAccessible(true);
        Object dynamicAdvisedInterceptor = h.get(proxy);
        Field advised = dynamicAdvisedInterceptor.getClass().getDeclaredField("advised");
        advised.setAccessible(true);
        return (AdvisedSupport)advised.get(dynamicAdvisedInterceptor);
    }

    /**
     * Is proxy boolean.
     *
     * @param bean the bean
     * @return the boolean
     */
    public static boolean isProxy(Object bean){
        if(bean == null){
            return false;
        }
        if (Proxy.class.isAssignableFrom(bean.getClass()) || AopUtils.isAopProxy(bean)) {
            return true;
        }else{
            return false;
        }
    }

    /**
     * Get the target class , get the interface of its agent if it is a Proxy
     *
     * @param proxy the proxy
     * @return target interface
     * @throws Exception the exception
     */
    public static Class<?> getTargetInterface(Object proxy) throws Exception {
        if(proxy == null){
            throw new java.lang.IllegalArgumentException("proxy can not be null");
        }

        //jdk proxy
        if (Proxy.class.isAssignableFrom(proxy.getClass())) {
            Proxy p = (Proxy) proxy;
            return p.getClass().getInterfaces()[0];
        }

        return getTarget(proxy).getClass();
    }

    /**
     * Get the proxy target object
     *
     * @param proxy 代理对象
     * @return target target
     * @throws Exception the exception
     */
    public static Object getTarget(Object proxy) throws Exception {
        if(proxy == null){
            throw new java.lang.IllegalArgumentException("proxy can not be null");
        }
        //非代理类
        if(!AopUtils.isAopProxy(proxy)) {
            return proxy;
        }
        AdvisedSupport advisedSupport = getAdvisedSupport(proxy);
        Object target = advisedSupport.getTargetSource().getTarget();
        /**
         * the Proxy of sofa:reference has no target
         */
        if (target == null ) {
            if(advisedSupport.getProxiedInterfaces() != null && advisedSupport.getProxiedInterfaces().length > 0){
                return advisedSupport.getProxiedInterfaces()[0];
            }else{
                //拿不到interface，返回原对象
                return proxy;
            }
        }else{
            return getTarget(target);
        }
    }

}
