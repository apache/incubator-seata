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
import java.util.List;
import java.util.Set;

import io.seata.common.util.CollectionUtils;
import io.seata.common.util.ReflectionUtil;
import org.aopalliance.intercept.MethodInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator;

/**
 * Seata Proxy Scanner
 *
 * @author wang.liang
 */
public class SeataProxyScanner extends AbstractAutoProxyCreator {

    private static final Logger LOGGER = LoggerFactory.getLogger(SeataProxyScanner.class);

    private static final Set<Class<?>> PROXY_BEAN_CLASSES = new HashSet<>();
    private static final Set<String> PROXY_BEAN_NAMES = new HashSet<>();

    private final SeataProxyHandler seataProxyHandler;
    private final int proxyInterceptorOrder;

    private MethodInterceptor interceptor;

    public SeataProxyScanner(SeataProxyConfig config, List<SeataProxyBeanRegister> registers, SeataProxyHandler seataProxyHandler) {
        // beans info from config
        addProxyBeanClasses(ReflectionUtil.classNamesToClassSet(config.getTargetBeanClasses()));
        addProxyBeanNames(config.getTargetBeanNames());

        // beans from registers
        if (CollectionUtils.isNotEmpty(registers)) {
            for(SeataProxyBeanRegister register : registers) {
                if(register == null) {
                    continue;
                }
                addProxyBeanClasses(register.getBeanClasses());
                addProxyBeanNames(register.getBeanNames());
            }
        }

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
    protected boolean shouldSkip(Class<?> beanClass, String beanName) {
        // if has `@SeataProxy` on the bean class, and the `skip == false`, need to proxy
        SeataProxy seataProxyAnno = beanClass.getAnnotation(SeataProxy.class);
        if (seataProxyAnno != null && !seataProxyAnno.skip()) {
            return false;
        }

        return !PROXY_BEAN_CLASSES.contains(beanClass) && !PROXY_BEAN_NAMES.contains(beanName);
    }

    @Override
    protected Object[] getAdvicesAndAdvisorsForBean(Class<?> beanClass, String beanName, TargetSource customTargetSource) {
        return new Object[]{interceptor};
    }


    //region static methods

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

    //endregion
}
