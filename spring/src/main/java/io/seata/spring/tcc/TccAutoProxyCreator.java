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
package io.seata.spring.tcc;

import java.util.List;

import io.seata.rm.tcc.config.TCCAutoProxyConfig;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator;

/**
 * Tcc Auto Proxy Creator
 *
 * @author wang.liang
 */
public class TccAutoProxyCreator extends AbstractAutoProxyCreator {

    private final List<String> proxyBeanClasses;
    private final List<String> proxyBeanNames;
    private final int proxyInterceptorOrder;

    private final TccAutoProxyAction tccAutoProxyAction;

    private MethodInterceptor interceptor;

    public TccAutoProxyCreator(TCCAutoProxyConfig config, TccAutoProxyAction tccAutoProxyAction) {
        this.proxyBeanClasses = config.getProxyBeanClasses();
        this.proxyBeanNames = config.getProxyBeanNames();
        this.proxyInterceptorOrder = config.getProxyInterceptorOrder();
        this.tccAutoProxyAction = tccAutoProxyAction;
    }

    @Override
    protected boolean shouldSkip(Class<?> beanClass, String beanName) {
        return proxyBeanClasses.contains(beanClass.getName()) || proxyBeanNames.contains(beanName);
    }

    @Override
    protected Object[] getAdvicesAndAdvisorsForBean(Class<?> beanClass, String beanName, TargetSource customTargetSource) {
        return new Object[]{interceptor};
    }

    @Override
    protected Object wrapIfNecessary(Object bean, String beanName, Object cacheKey) {
        this.interceptor = new TccAutoProxyInterceptor(beanName, tccAutoProxyAction, proxyInterceptorOrder);
        return super.wrapIfNecessary(bean, beanName, cacheKey);
    }
}
