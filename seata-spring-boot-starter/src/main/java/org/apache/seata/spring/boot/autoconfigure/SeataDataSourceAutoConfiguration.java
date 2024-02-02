/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.spring.boot.autoconfigure;

import javax.sql.DataSource;
import org.apache.seata.spring.annotation.datasource.SeataAutoDataSourceProxyCreator;
import org.apache.seata.spring.boot.autoconfigure.properties.SeataProperties;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

import static org.apache.seata.spring.annotation.datasource.AutoDataSourceProxyRegistrar.BEAN_NAME_SEATA_AUTO_DATA_SOURCE_PROXY_CREATOR;

/**
 * The type Seata data source auto configuration.
 *
 */
@ConditionalOnBean(DataSource.class)
@ConditionalOnExpression("${seata.enabled:true} && ${seata.enableAutoDataSourceProxy:true} && ${seata.enable-auto-data-source-proxy:true}")
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
@AutoConfigureAfter(value = {SeataCoreAutoConfiguration.class},
    name = "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration")
public class SeataDataSourceAutoConfiguration {

    /**
     * The bean seataAutoDataSourceProxyCreator.
     */
    @Bean(BEAN_NAME_SEATA_AUTO_DATA_SOURCE_PROXY_CREATOR)
    @ConditionalOnMissingBean(SeataAutoDataSourceProxyCreator.class)
    public static SeataAutoDataSourceProxyCreator seataAutoDataSourceProxyCreator(SeataProperties seataProperties) {
        return new SeataAutoDataSourceProxyCreator(seataProperties.isUseJdkProxy(),
            seataProperties.getExcludesForAutoProxying(), seataProperties.getDataSourceProxyMode());
    }

}
