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
import java.lang.reflect.Method;

import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.config.ConfigurationFactory;
import io.seata.rm.datasource.DataSourceProxy;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.PriorityOrdered;

import static io.seata.core.constants.ConfigurationKeys.DATASOURCE_AUTOPROXY;

/**
 * @author xingfudeshi@gmail.com
 * @date 2019/12/26
 * The type seata data source bean post processor
 */
public class SeataDataSourceBeanPostProcessor implements BeanPostProcessor, PriorityOrdered {
    private static final Logger LOGGER = LoggerFactory.getLogger(SeataDataSourceBeanPostProcessor.class);

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof DataSource && !(bean instanceof DataSourceProxy) && ConfigurationFactory.getInstance().getBoolean(DATASOURCE_AUTOPROXY, false)) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Auto proxy of [{}]", beanName);
            }
            DataSourceProxy dataSourceProxy = DataSourceProxyHolder.get().putDataSource((DataSource) bean);
            return Enhancer.create(bean.getClass(), new net.sf.cglib.proxy.MethodInterceptor() {
                @Override
                public Object intercept(Object proxy, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
                    Method m = BeanUtils.findDeclaredMethod(DataSourceProxy.class, method.getName(), method.getParameterTypes());
                    if (null != m) {
                        return m.invoke(dataSourceProxy, args);
                    } else {
                        boolean oldAccessible = method.isAccessible();
                        try {
                            method.setAccessible(true);
                            return method.invoke(bean, args);
                        } finally {
                            //recover the original accessible for security reason
                            method.setAccessible(oldAccessible);
                        }
                    }
                }
            });
        }
        return bean;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        if (bean instanceof DataSourceProxy && ConfigurationFactory.getInstance().getBoolean(DATASOURCE_AUTOPROXY, false)) {
            throw new ShouldNeverHappenException("Auto proxy of DataSource can't be enabled as you've created a DataSourceProxy bean." +
                "Please consider removing DataSourceProxy bean or disabling auto proxy of DataSource.");
        }
        return bean;
    }

    @Override
    public int getOrder() {
        return HIGHEST_PRECEDENCE;
    }
}
