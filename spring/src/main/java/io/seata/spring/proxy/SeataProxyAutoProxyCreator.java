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
package io.seata.spring.proxy;

import java.util.HashSet;
import java.util.Set;

import io.seata.common.util.ReflectionUtil;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator;

/**
 * Seata Proxy Auto Proxy Creator
 *
 * @author wang.liang
 */
public class SeataProxyAutoProxyCreator extends AbstractAutoProxyCreator {

    private final Set<Class<?>> proxyBeanClasses;
    private final Set<String> proxyBeanNames;
    private final int proxyInterceptorOrder;

    private final SeataProxyHandler seataProxyHandler;

    private MethodInterceptor interceptor;

    public SeataProxyAutoProxyCreator(SeataProxyConfig config, SeataProxyHandler seataProxyHandler) {
        this.proxyBeanClasses = ReflectionUtil.classNameCollToClassSet(config.getTargetBeanClasses());
        this.proxyBeanNames = config.getTargetBeanNames();
        this.proxyInterceptorOrder = config.getProxyInterceptorOrder();

        this.seataProxyHandler = seataProxyHandler;
    }

    @Override
    protected Object wrapIfNecessary(Object bean, String beanName, Object cacheKey) {
        // create an interceptor for the bean
        this.interceptor = new SeataProxyInterceptor(beanName, this.seataProxyHandler, this.proxyInterceptorOrder);

        // do wrap
        return super.wrapIfNecessary(bean, beanName, cacheKey);
    }

    @Override
    protected Object[] getAdvicesAndAdvisorsForBean(Class<?> beanClass, String beanName, TargetSource customTargetSource) {
        return new Object[]{interceptor};
    }

    @Override
    protected boolean shouldSkip(Class<?> beanClass, String beanName) {
        return proxyBeanClasses.contains(beanClass) || proxyBeanNames.contains(beanName);
    }
}
