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

import io.seata.spring.annotation.GlobalTransactionScanner;
import io.seata.spring.annotation.datasource.SeataDataSourceBeanPostProcessor;
import io.seata.spring.boot.autoconfigure.properties.SeataProperties;
import io.seata.spring.boot.autoconfigure.util.SpringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import static io.seata.spring.annotation.datasource.AutoDataSourceProxyRegistrar.BEAN_NAME_SEATA_DATA_SOURCE_BEAN_POST_PROCESSOR;

/**
 * @author xingfudeshi@gmail.com
 */
@ComponentScan(basePackages = "io.seata.spring.boot.autoconfigure.properties")
@ConditionalOnProperty(prefix = StarterConstants.SEATA_PREFIX, name = "enabled", havingValue = "true", matchIfMissing = true)
@Configuration
@EnableConfigurationProperties({SeataProperties.class})
public class SeataAutoConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(SeataAutoConfiguration.class);

    @Bean
    public SpringUtils springUtils() {
        return new SpringUtils();
    }

    @Bean
    @DependsOn({"springUtils"})
    @ConditionalOnMissingBean(GlobalTransactionScanner.class)
    public GlobalTransactionScanner globalTransactionScanner(SeataProperties seataProperties) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Automatically configure Seata");
        }
        return new GlobalTransactionScanner(seataProperties.getApplicationId(), seataProperties.getTxServiceGroup());
    }

    @Bean(BEAN_NAME_SEATA_DATA_SOURCE_BEAN_POST_PROCESSOR)
    @ConditionalOnProperty(prefix = StarterConstants.SEATA_PREFIX, name = "enableAutoDataSourceProxy", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean(SeataDataSourceBeanPostProcessor.class)
    public SeataDataSourceBeanPostProcessor seataDataSourceBeanPostProcessor(SeataProperties seataProperties) {
        return new SeataDataSourceBeanPostProcessor(seataProperties.isUseJdkProxy());
    }
}
