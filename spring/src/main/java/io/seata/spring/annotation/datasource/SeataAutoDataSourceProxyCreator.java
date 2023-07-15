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
package io.seata.spring.annotation.datasource;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.sql.DataSource;

import io.seata.common.aot.NativeUtils;
import io.seata.core.model.BranchType;
import io.seata.rm.datasource.DataSourceProxy;
import io.seata.rm.datasource.SeataDataSourceProxy;
import io.seata.rm.datasource.xa.DataSourceProxyXA;
import org.aopalliance.aop.Advice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator;
import org.springframework.aop.support.DefaultIntroductionAdvisor;

/**
 * @author xingfudeshi@gmail.com
 * @author selfishlover
 */
public class SeataAutoDataSourceProxyCreator extends AbstractAutoProxyCreator {

    private static final Logger LOGGER = LoggerFactory.getLogger(SeataAutoDataSourceProxyCreator.class);

    private final Set<String> excludes;

    private final String dataSourceProxyMode;

    private final Object[] advisors;

    public SeataAutoDataSourceProxyCreator(boolean useJdkProxy, String[] excludes, String dataSourceProxyMode) {
        setProxyTargetClass(!useJdkProxy);
        this.excludes = new HashSet<>(Arrays.asList(excludes));
        this.dataSourceProxyMode = dataSourceProxyMode;
        this.advisors = buildAdvisors(dataSourceProxyMode);
    }

    private Object[] buildAdvisors(String dataSourceProxyMode) {
        Advice advice = new SeataAutoDataSourceProxyAdvice(dataSourceProxyMode);
        return new Object[]{new DefaultIntroductionAdvisor(advice)};
    }

    @Override
    protected Object[] getAdvicesAndAdvisorsForBean(Class<?> beanClass, String beanName, TargetSource customTargetSource) {
        if (NativeUtils.isSpringAotProcessing()) {
            if (!DataSource.class.isAssignableFrom(beanClass)) {
                return DO_NOT_PROXY;
            }

            if (this.shouldSkip(beanClass, beanName)) {
                return DO_NOT_PROXY;
            }
        }
        return advisors;
    }

    @Override
    protected boolean shouldSkip(Class<?> beanClass, String beanName) {
        if (excludes.contains(beanClass.getName())) {
            return true;
        }
        return SeataProxy.class.isAssignableFrom(beanClass);
    }

    @Override
    protected Object wrapIfNecessary(Object bean, String beanName, Object cacheKey) {
        // we only care DataSource bean
        if (!(bean instanceof DataSource)) {
            return bean;
        }

        // when this bean is just a simple DataSource, not SeataDataSourceProxy
        if (!(bean instanceof SeataDataSourceProxy)) {
            Object enhancer = super.wrapIfNecessary(bean, beanName, cacheKey);
            // this mean this bean is either excluded by user or had been proxy before
            if (bean == enhancer) {
                return bean;
            }
            // else, build proxy,  put <origin, proxy> to holder and return enhancer
            DataSource origin = (DataSource) bean;
            SeataDataSourceProxy proxy = buildProxy(origin, dataSourceProxyMode);
            DataSourceProxyHolder.put(origin, proxy);
            LOGGER.info("Auto proxy data source '{}' by '{}' mode.", beanName, dataSourceProxyMode);
            return enhancer;
        }

        /*
         * things get dangerous when you try to register SeataDataSourceProxy bean by yourself!
         * if you insist on doing so, you must make sure your method return type is DataSource,
         * because this processor will never return any subclass of SeataDataSourceProxy
         */
        LOGGER.warn("Manually register SeataDataSourceProxy(or its subclass) bean is discouraged! bean name: {}", beanName);
        SeataDataSourceProxy proxy = (SeataDataSourceProxy) bean;
        DataSource origin = proxy.getTargetDataSource();
        Object originEnhancer = super.wrapIfNecessary(origin, beanName, cacheKey);
        // this mean origin is either excluded by user or had been proxy before
        if (origin == originEnhancer) {
            return origin;
        }
        // else, put <origin, proxy> to holder and return originEnhancer
        DataSourceProxyHolder.put(origin, proxy);
        return originEnhancer;
    }

    SeataDataSourceProxy buildProxy(DataSource origin, String proxyMode) {
        if (BranchType.AT.name().equalsIgnoreCase(proxyMode)) {
            return new DataSourceProxy(origin);
        }
        if (BranchType.XA.name().equalsIgnoreCase(proxyMode)) {
            return new DataSourceProxyXA(origin);
        }
        throw new IllegalArgumentException("Unknown dataSourceProxyMode: " + proxyMode);
    }
}
