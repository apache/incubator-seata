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

    public static Class<?> findTargetClass(Object proxy) throws Exception {
        if (AopUtils.isAopProxy(proxy)) {
            AdvisedSupport advised = getAdvisedSupport(proxy);
            Object target = advised.getTargetSource().getTarget();
            return findTargetClass(target);
        } else {
            return proxy.getClass();
        }
    }

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
     * 获取目标类类型，如果是Proxy则获取其代理的interface
     * @param proxy
     * @return
     * @throws Exception
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
     * 获取 被代理 目标对象
     * @param proxy 代理对象
     * @return
     * @throws Exception
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
