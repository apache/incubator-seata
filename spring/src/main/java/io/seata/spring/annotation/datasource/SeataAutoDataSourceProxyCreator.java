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

import javax.sql.DataSource;
import java.util.stream.Stream;

import io.seata.rm.datasource.DataSourceProxy;
import io.seata.spring.util.SpringProxyUtils;
import org.aopalliance.intercept.MethodInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.Advisor;
import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.AdvisedSupport;
import org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;

/**
 * @author xingfudeshi@gmail.com
 */
public class SeataAutoDataSourceProxyCreator extends AbstractAutoProxyCreator {
    private static final Logger LOGGER = LoggerFactory.getLogger(SeataAutoDataSourceProxyCreator.class);
    private MethodInterceptor advice;
    private final String[] exclude;

    public SeataAutoDataSourceProxyCreator(boolean useJdkProxy, String[] exclude) {
        this.exclude = exclude;
        setProxyTargetClass(!useJdkProxy);
    }

    @Override
    protected Object wrapIfNecessary(Object bean, String beanName, Object cacheKey) {
        String className = bean.getClass().getName();
        if (bean instanceof DataSource && Stream.of(exclude).noneMatch(s -> s.equals(className))) {
            boolean isProxied = AopUtils.isAopProxy(bean);
            if (!isProxied || !isAutoProxiedBySeata(bean)) {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("Auto proxy of [{}]", beanName);
                }
                DataSourceProxy dataSourceProxy = DataSourceProxyHolder.get().putDataSource((DataSource) bean);
                advice = new SeataAutoDataSourceProxyAdvice(dataSourceProxy);
                if (isProxied) {
                    try {
                        AdvisedSupport advised = SpringProxyUtils.getAdvisedSupport(bean);
                        Advisor[] advisor = buildAdvisors(beanName, getAdvicesAndAdvisorsForBean(null, null, null));
                        for (Advisor avr : advisor) {
                            advised.addAdvisor(0, avr);
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    bean = super.wrapIfNecessary(bean, beanName, cacheKey);
                }
            }
        }
        return bean;
    }

    /**
     * Whether this bean has been proxied by Seata
     *
     * @param bean
     * @return true if this bean has been proxied by Seata
     */
    private boolean isAutoProxiedBySeata(Object bean) {
        try {
            AdvisedSupport advised = SpringProxyUtils.getAdvisedSupport(bean);
            return advised.countAdvicesOfType(SeataAutoDataSourceProxyAdvice.class) > 0;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected Object[] getAdvicesAndAdvisorsForBean(Class<?> beanClass, String beanName, TargetSource customTargetSource) throws BeansException {
        return new Object[]{advice};
    }
}
