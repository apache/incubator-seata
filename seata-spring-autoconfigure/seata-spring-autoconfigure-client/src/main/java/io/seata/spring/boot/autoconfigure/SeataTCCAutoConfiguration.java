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
package io.seata.spring.boot.autoconfigure;

import javax.sql.DataSource;

import io.seata.rm.tcc.config.TCCFenceConfig;
import io.seata.spring.proxy.DefaultSeataProxyHandler;
import io.seata.spring.proxy.SeataProxyHandler;
import io.seata.spring.tcc.DefaultTccSeataProxyActionImpl;
import io.seata.spring.tcc.TccSeataProxyAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * TCC auto configuration.
 *
 * @author kaka2code
 * @author wang.liang
 */
@Configuration
@ConditionalOnProperty(prefix = StarterConstants.SEATA_PREFIX, name = "enabled", matchIfMissing = true)
@AutoConfigureAfter({DataSourceAutoConfiguration.class, DataSourceTransactionManagerAutoConfiguration.class})
public class SeataTCCAutoConfiguration {

    public static final String TCC_FENCE_DATA_SOURCE_BEAN_NAME = "seataTCCFenceDataSource";
    public static final String TCC_FENCE_TRANSACTION_MANAGER_BEAN_NAME = "seataTCCFenceTransactionManager";

    @Bean
    @ConditionalOnMissingBean(TCCFenceConfig.class)
    @ConditionalOnProperty(prefix = StarterConstants.TCC_FENCE_PREFIX, name = "enabled", matchIfMissing = true)
    @ConditionalOnBean({DataSource.class, PlatformTransactionManager.class})
    @ConfigurationProperties(StarterConstants.TCC_FENCE_PREFIX)
    public TCCFenceConfig tccFenceConfig(
            DataSource dataSource,
            PlatformTransactionManager transactionManager,
            @Qualifier(TCC_FENCE_DATA_SOURCE_BEAN_NAME) @Autowired(required = false) DataSource tccFenceDataSource,
            @Qualifier(TCC_FENCE_TRANSACTION_MANAGER_BEAN_NAME) @Autowired(required = false) PlatformTransactionManager tccFenceTransactionManager) {
        return new TCCFenceConfig(tccFenceDataSource != null ? tccFenceDataSource : dataSource,
                tccFenceTransactionManager != null ? tccFenceTransactionManager : transactionManager);
    }

    @Configuration
    @ConditionalOnMissingBean(SeataProxyHandler.class)
    @ConditionalOnProperty(prefix = StarterConstants.PROXY_PREFIX, name = "enabled")
    static class TccSeataProxyConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public TccSeataProxyAction tccSeataProxyAction() {
            return new DefaultTccSeataProxyActionImpl();
        }

        @Bean
        public SeataProxyHandler tccSeataProxyHandler(TccSeataProxyAction tccSeataProxyAction) {
            return new DefaultSeataProxyHandler(tccSeataProxyAction);
        }
    }
}
