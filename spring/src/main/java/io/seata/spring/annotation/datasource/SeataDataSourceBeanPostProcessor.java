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

import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.rm.datasource.DataSourceProxy;
import io.seata.rm.datasource.xa.DataSourceProxyXA;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import javax.sql.DataSource;

/**
 * The type seata data source bean post processor
 *
 * @author xingfudeshi@gmail.com
 * @author wang.liang
 */
public class SeataDataSourceBeanPostProcessor implements BeanPostProcessor {

    private final String dataSourceProxyMode;

    public SeataDataSourceBeanPostProcessor(String dataSourceProxyMode) {
        this.dataSourceProxyMode = dataSourceProxyMode;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof DataSource && !(bean instanceof DataSourceProxy) && !(bean instanceof DataSourceProxyXA)) {
            //only put and init proxy, not return proxy.
            DataSourceProxyHolder.get().putDataSource((DataSource) bean, dataSourceProxyMode);
        }
        return bean;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        if (bean instanceof DataSourceProxy || bean instanceof DataSourceProxyXA) {
            throw new ShouldNeverHappenException("Auto proxy of DataSource can't be enabled as you've created a DataSourceProxy bean." +
                    "Please consider removing DataSourceProxy bean or disabling auto proxy of DataSource.");
        }
        return bean;
    }
}
