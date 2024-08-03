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

import java.util.List;

import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.spring.annotation.GlobalTransactionScanner;
import org.apache.seata.spring.annotation.ScannerChecker;
import org.apache.seata.spring.boot.autoconfigure.properties.SeataProperties;
import org.apache.seata.tm.api.DefaultFailureHandlerImpl;
import org.apache.seata.tm.api.FailureHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;

import static org.apache.seata.common.Constants.BEAN_NAME_FAILURE_HANDLER;
import static org.apache.seata.common.Constants.BEAN_NAME_SPRING_APPLICATION_CONTEXT_PROVIDER;
import static org.apache.seata.spring.boot.autoconfigure.StarterConstants.SEATA_PREFIX;

/**
 * The type Seata auto configuration
 *
 */
@ConditionalOnProperty(prefix = SEATA_PREFIX, name = "enabled", havingValue = "true", matchIfMissing = true)
@AutoConfigureAfter({SeataCoreAutoConfiguration.class})
public class SeataAutoConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(SeataAutoConfiguration.class);

    @Bean(BEAN_NAME_FAILURE_HANDLER)
    @ConditionalOnMissingBean(FailureHandler.class)
    public FailureHandler failureHandler() {
        return new DefaultFailureHandlerImpl();
    }

    @Bean
    @DependsOn({BEAN_NAME_SPRING_APPLICATION_CONTEXT_PROVIDER, BEAN_NAME_FAILURE_HANDLER})
    @ConditionalOnMissingBean(GlobalTransactionScanner.class)
    public static GlobalTransactionScanner globalTransactionScanner(SeataProperties seataProperties, FailureHandler failureHandler,
            ConfigurableListableBeanFactory beanFactory,
            @Autowired(required = false) List<ScannerChecker> scannerCheckers) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Automatically configure Seata");
        }

        // set bean factory
        GlobalTransactionScanner.setBeanFactory(beanFactory);

        // add checkers
        // '/META-INF/services/org.apache.seata.spring.annotation.ScannerChecker'
        GlobalTransactionScanner.addScannerCheckers(EnhancedServiceLoader.loadAll(ScannerChecker.class));
        // spring beans
        GlobalTransactionScanner.addScannerCheckers(scannerCheckers);

        // add scannable packages
        GlobalTransactionScanner.addScannablePackages(seataProperties.getScanPackages());
        // add excludeBeanNames
        GlobalTransactionScanner.addScannerExcludeBeanNames(seataProperties.getExcludesForScanning());
        //set accessKey and secretKey
        GlobalTransactionScanner.setAccessKey(seataProperties.getAccessKey());
        GlobalTransactionScanner.setSecretKey(seataProperties.getSecretKey());
        // create global transaction scanner
        return new GlobalTransactionScanner(seataProperties.getApplicationId(), seataProperties.getTxServiceGroup(),
                seataProperties.isExposeProxy(), failureHandler);
    }
}
