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
import java.util.List;
import javax.sql.DataSource;

import io.seata.core.model.BranchType;
import io.seata.rm.datasource.SeataDataSourceProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * The type seata data source bean post processor
 *
 * @author xingfudeshi@gmail.com
 * @author wang.liang
 */
public class SeataDataSourceBeanPostProcessor implements BeanPostProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(SeataDataSourceBeanPostProcessor.class);

    private final List<String> excludes;
    private final BranchType dataSourceProxyMode;

    public SeataDataSourceBeanPostProcessor(String[] excludes, String dataSourceProxyMode) {
        this.excludes = Arrays.asList(excludes);
        this.dataSourceProxyMode = BranchType.XA.name().equalsIgnoreCase(dataSourceProxyMode) ? BranchType.XA : BranchType.AT;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof DataSource) {
            //When not in the excludes, put and init proxy.
            if (!excludes.contains(bean.getClass().getName())) {
                //Only put and init proxy, not return proxy.
                DataSourceProxyHolder.get().putDataSource((DataSource) bean, dataSourceProxyMode);
            }

            //If is SeataDataSourceProxy, return the original data source.
            if (bean instanceof SeataDataSourceProxy) {
                LOGGER.info("Unwrap the bean of the data source," +
                    " and return the original data source to replace the data source proxy.");
                return ((SeataDataSourceProxy) bean).getTargetDataSource();
            }
        }
        return bean;
    }
}
