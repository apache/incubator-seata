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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import io.seata.common.util.CollectionUtils;
import io.seata.common.util.ReflectionUtil;
import org.aopalliance.intercept.MethodInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator;

/**
 * Seata Proxy Auto Proxy Creator
 *
 * @author wang.liang
 */
public class SeataProxyAutoProxyCreator extends AbstractAutoProxyCreator {

    private static final Logger LOGGER = LoggerFactory.getLogger(SeataProxyAutoProxyCreator.class);

    private static final Set<Class<?>> PROXY_BEAN_CLASSES = new HashSet<>();
    private static final Set<String> PROXY_BEAN_NAMES = new HashSet<>();

    private final SeataProxyHandler seataProxyHandler;
    private final int proxyInterceptorOrder;

    private MethodInterceptor interceptor;

    public SeataProxyAutoProxyCreator(SeataProxyConfig config, SeataProxyHandler seataProxyHandler) {
        addProxyBeanClasses(ReflectionUtil.classNamesToClassSet(config.getTargetBeanClasses()));
        addProxyBeanNames(config.getTargetBeanNames());

        this.seataProxyHandler = seataProxyHandler;
        this.proxyInterceptorOrder = config.getProxyInterceptorOrder();
    }

    @Override
    protected Object wrapIfNecessary(Object bean, String beanName, Object cacheKey) {
        if (this.shouldSkip(bean.getClass(), beanName)) {
            return bean;
        }

        // create an interceptor for the bean
        this.interceptor = new SeataProxyInterceptor(beanName, this.seataProxyHandler, this.proxyInterceptorOrder);

        LOGGER.info("Bean[{}] with name [{}] would use interceptor [{}]", bean.getClass().getName(), beanName, interceptor.getClass().getName());

        // do wrap
        return super.wrapIfNecessary(bean, beanName, cacheKey);
    }

    @Override
    protected Object[] getAdvicesAndAdvisorsForBean(Class<?> beanClass, String beanName, TargetSource customTargetSource) {
        return new Object[]{interceptor};
    }

    @Override
    protected boolean shouldSkip(Class<?> beanClass, String beanName) {
        return !PROXY_BEAN_CLASSES.contains(beanClass) && !PROXY_BEAN_NAMES.contains(beanName);
    }

    public static void addProxyBeanClasses(Collection<Class<?>> beanClasses) {
        CollectionUtils.addAll(PROXY_BEAN_CLASSES, beanClasses);
    }

    public static void addProxyBeanClasses(Class<?>... beanClasses) {
        CollectionUtils.addAll(PROXY_BEAN_CLASSES, beanClasses);
    }

    public static void addProxyBeanClasses(String... beanClassNames) {
        if (CollectionUtils.isNotEmpty(beanClassNames)) {
            addProxyBeanClasses(ReflectionUtil.classNamesToClassSet(Arrays.asList(beanClassNames)));
        }
    }

    public static void addProxyBeanNames(Collection<String> beanNames) {
        CollectionUtils.addAll(PROXY_BEAN_NAMES, beanNames);
    }

    public static void addProxyBeanNames(String... beanNames) {
        CollectionUtils.addAll(PROXY_BEAN_NAMES, beanNames);
    }
}
