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

import org.apache.seata.common.holder.ObjectHolder;
import org.apache.seata.rm.fence.SpringFenceConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

import static org.apache.seata.common.Constants.BEAN_NAME_SPRING_FENCE_CONFIG;

/**
 * Spring fence auto configuration.
 *
 */
@ConditionalOnExpression("${seata.enabled:true}")
@ConditionalOnBean(type = {"javax.sql.DataSource", "org.springframework.transaction.PlatformTransactionManager"})
@ConditionalOnMissingBean(SpringFenceConfig.class)
@AutoConfigureAfter({SeataCoreAutoConfiguration.class, TransactionAutoConfiguration.class})
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
public class SeataSpringFenceAutoConfiguration {

    public static final String SPRING_FENCE_DATA_SOURCE_BEAN_NAME = "seataSpringFenceDataSource";
    public static final String SPRING_FENCE_TRANSACTION_MANAGER_BEAN_NAME = "seataSpringFenceTransactionManager";

    @Bean
    @ConfigurationProperties(StarterConstants.TCC_FENCE_PREFIX)
    public SpringFenceConfig springFenceConfig(
            DataSource dataSource,
            PlatformTransactionManager transactionManager,
            @Qualifier(SPRING_FENCE_DATA_SOURCE_BEAN_NAME) @Autowired(required = false) DataSource springFenceDataSource,
            @Qualifier(SPRING_FENCE_TRANSACTION_MANAGER_BEAN_NAME) @Autowired(required = false) PlatformTransactionManager springFenceTransactionManager) {
        SpringFenceConfig springFenceConfig = new SpringFenceConfig(springFenceDataSource != null ? springFenceDataSource : dataSource,
                springFenceTransactionManager != null ? springFenceTransactionManager : transactionManager);
        ObjectHolder.INSTANCE.setObject(BEAN_NAME_SPRING_FENCE_CONFIG, springFenceConfig);
        return springFenceConfig;
    }

}
