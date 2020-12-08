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
import io.seata.spring.annotation.datasource.SeataAutoDataSourceProxyCreator;
import io.seata.spring.annotation.datasource.SeataDataSourceBeanPostProcessor;
import io.seata.spring.boot.autoconfigure.properties.SeataProperties;
import io.seata.spring.boot.autoconfigure.properties.client.LockProperties;
import io.seata.spring.boot.autoconfigure.properties.client.LogProperties;
import io.seata.spring.boot.autoconfigure.properties.client.RmProperties;
import io.seata.spring.boot.autoconfigure.properties.client.ServiceProperties;
import io.seata.spring.boot.autoconfigure.properties.client.ShutdownProperties;
import io.seata.spring.boot.autoconfigure.properties.client.ThreadFactoryProperties;
import io.seata.spring.boot.autoconfigure.properties.client.TmProperties;
import io.seata.spring.boot.autoconfigure.properties.client.TransportProperties;
import io.seata.spring.boot.autoconfigure.properties.client.UndoProperties;
import io.seata.spring.boot.autoconfigure.properties.config.ConfigApolloProperties;
import io.seata.spring.boot.autoconfigure.properties.config.ConfigConsulProperties;
import io.seata.spring.boot.autoconfigure.properties.config.ConfigCustomProperties;
import io.seata.spring.boot.autoconfigure.properties.config.ConfigEtcd3Properties;
import io.seata.spring.boot.autoconfigure.properties.config.ConfigFileProperties;
import io.seata.spring.boot.autoconfigure.properties.config.ConfigNacosProperties;
import io.seata.spring.boot.autoconfigure.properties.config.ConfigProperties;
import io.seata.spring.boot.autoconfigure.properties.config.ConfigZooKeeperProperties;
import io.seata.spring.boot.autoconfigure.properties.registry.RegistryConsulProperties;
import io.seata.spring.boot.autoconfigure.properties.registry.RegistryCustomProperties;
import io.seata.spring.boot.autoconfigure.properties.registry.RegistryEtcd3Properties;
import io.seata.spring.boot.autoconfigure.properties.registry.RegistryEurekaProperties;
import io.seata.spring.boot.autoconfigure.properties.registry.RegistryNacosProperties;
import io.seata.spring.boot.autoconfigure.properties.registry.RegistryProperties;
import io.seata.spring.boot.autoconfigure.properties.registry.RegistryRedisProperties;
import io.seata.spring.boot.autoconfigure.properties.registry.RegistrySofaProperties;
import io.seata.spring.boot.autoconfigure.properties.registry.RegistryZooKeeperProperties;
import io.seata.spring.boot.autoconfigure.provider.SpringApplicationContextProvider;
import io.seata.tm.api.DefaultFailureHandlerImpl;
import io.seata.tm.api.FailureHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import static io.seata.common.Constants.BEAN_NAME_FAILURE_HANDLER;
import static io.seata.common.Constants.BEAN_NAME_SPRING_APPLICATION_CONTEXT_PROVIDER;
import static io.seata.spring.annotation.datasource.AutoDataSourceProxyRegistrar.BEAN_NAME_SEATA_AUTO_DATA_SOURCE_PROXY_CREATOR;
import static io.seata.spring.annotation.datasource.AutoDataSourceProxyRegistrar.BEAN_NAME_SEATA_DATA_SOURCE_BEAN_POST_PROCESSOR;
import static io.seata.spring.boot.autoconfigure.StarterConstants.CLIENT_RM_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.CLIENT_TM_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.CONFIG_APOLLO_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.CONFIG_CONSUL_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.CONFIG_CUSTOM_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.CONFIG_ETCD3_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.CONFIG_FILE_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.CONFIG_NACOS_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.CONFIG_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.CONFIG_ZK_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.LOCK_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.LOG_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.PROPERTY_BEAN_MAP;
import static io.seata.spring.boot.autoconfigure.StarterConstants.REGISTRY_CONSUL_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.REGISTRY_CUSTOM_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.REGISTRY_ETCD3_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.REGISTRY_EUREKA_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.REGISTRY_NACOS_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.REGISTRY_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.REGISTRY_REDIS_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.REGISTRY_SOFA_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.REGISTRY_ZK_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.SEATA_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.SERVICE_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.SHUTDOWN_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.THREAD_FACTORY_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.TRANSPORT_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.UNDO_PREFIX;

/**
 * @author xingfudeshi@gmail.com
 */
@ComponentScan(basePackages = "io.seata.spring.boot.autoconfigure.properties")
@ConditionalOnProperty(prefix = SEATA_PREFIX, name = "enabled", havingValue = "true", matchIfMissing = true)
@Configuration
@EnableConfigurationProperties({SeataProperties.class})
public class SeataAutoConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(SeataAutoConfiguration.class);

    public SeataAutoConfiguration(SeataProperties seataProperties,
            RmProperties rmProperties, TmProperties tmProperties, LockProperties lockProperties,
            ServiceProperties serviceProperties, ShutdownProperties shutdownProperties, ThreadFactoryProperties threadFactoryProperties,
            UndoProperties undoProperties, LogProperties logProperties, TransportProperties transportProperties,
            ConfigProperties configProperties, ConfigFileProperties configFileProperties, RegistryProperties registryProperties,
            ConfigNacosProperties configNacosProperties, ConfigConsulProperties configConsulProperties, ConfigZooKeeperProperties configZooKeeperProperties,
            ConfigApolloProperties configApolloProperties, ConfigEtcd3Properties configEtcd3Properties, ConfigCustomProperties configCustomProperties,
            RegistryConsulProperties registryConsulProperties, RegistryEtcd3Properties registryEtcd3Properties, RegistryEurekaProperties registryEurekaProperties,
            RegistryNacosProperties registryNacosProperties, RegistryRedisProperties registryRedisProperties, RegistrySofaProperties registrySofaProperties,
            RegistryZooKeeperProperties registryZooKeeperProperties, RegistryCustomProperties registryCustomProperties) {
        PROPERTY_BEAN_MAP.put(SEATA_PREFIX, seataProperties);

        PROPERTY_BEAN_MAP.put(CLIENT_RM_PREFIX, rmProperties);
        PROPERTY_BEAN_MAP.put(CLIENT_TM_PREFIX, tmProperties);
        PROPERTY_BEAN_MAP.put(LOCK_PREFIX, lockProperties);
        PROPERTY_BEAN_MAP.put(SERVICE_PREFIX, serviceProperties);
        PROPERTY_BEAN_MAP.put(SHUTDOWN_PREFIX, shutdownProperties);
        PROPERTY_BEAN_MAP.put(THREAD_FACTORY_PREFIX, threadFactoryProperties);
        PROPERTY_BEAN_MAP.put(UNDO_PREFIX, undoProperties);
        PROPERTY_BEAN_MAP.put(LOG_PREFIX, logProperties);
        PROPERTY_BEAN_MAP.put(TRANSPORT_PREFIX, transportProperties);
        PROPERTY_BEAN_MAP.put(CONFIG_PREFIX, configProperties);
        PROPERTY_BEAN_MAP.put(CONFIG_FILE_PREFIX, configFileProperties);
        PROPERTY_BEAN_MAP.put(REGISTRY_PREFIX, registryProperties);

        PROPERTY_BEAN_MAP.put(CONFIG_NACOS_PREFIX, configNacosProperties);
        PROPERTY_BEAN_MAP.put(CONFIG_CONSUL_PREFIX, configConsulProperties);
        PROPERTY_BEAN_MAP.put(CONFIG_ZK_PREFIX, configZooKeeperProperties);
        PROPERTY_BEAN_MAP.put(CONFIG_APOLLO_PREFIX, configApolloProperties);
        PROPERTY_BEAN_MAP.put(CONFIG_ETCD3_PREFIX, configEtcd3Properties);
        PROPERTY_BEAN_MAP.put(CONFIG_CUSTOM_PREFIX, configCustomProperties);

        PROPERTY_BEAN_MAP.put(REGISTRY_CONSUL_PREFIX, registryConsulProperties);
        PROPERTY_BEAN_MAP.put(REGISTRY_ETCD3_PREFIX, registryEtcd3Properties);
        PROPERTY_BEAN_MAP.put(REGISTRY_EUREKA_PREFIX, registryEurekaProperties);
        PROPERTY_BEAN_MAP.put(REGISTRY_NACOS_PREFIX, registryNacosProperties);
        PROPERTY_BEAN_MAP.put(REGISTRY_REDIS_PREFIX, registryRedisProperties);
        PROPERTY_BEAN_MAP.put(REGISTRY_SOFA_PREFIX, registrySofaProperties);
        PROPERTY_BEAN_MAP.put(REGISTRY_ZK_PREFIX, registryZooKeeperProperties);
        PROPERTY_BEAN_MAP.put(REGISTRY_CUSTOM_PREFIX, registryCustomProperties);
    }

    @Bean(BEAN_NAME_SPRING_APPLICATION_CONTEXT_PROVIDER)
    @ConditionalOnMissingBean(name = {BEAN_NAME_SPRING_APPLICATION_CONTEXT_PROVIDER})
    public SpringApplicationContextProvider springApplicationContextProvider() {
        return new SpringApplicationContextProvider();
    }

    @Bean(BEAN_NAME_FAILURE_HANDLER)
    @ConditionalOnMissingBean(FailureHandler.class)
    public FailureHandler failureHandler() {
        return new DefaultFailureHandlerImpl();
    }

    @Bean
    @DependsOn({BEAN_NAME_SPRING_APPLICATION_CONTEXT_PROVIDER, BEAN_NAME_FAILURE_HANDLER})
    @ConditionalOnMissingBean(GlobalTransactionScanner.class)
    public GlobalTransactionScanner globalTransactionScanner(SeataProperties seataProperties, FailureHandler failureHandler) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Automatically configure Seata");
        }
        return new GlobalTransactionScanner(seataProperties.getApplicationId(), seataProperties.getTxServiceGroup(), failureHandler);
    }

    /**
     * The data source configuration.
     */
    @Configuration
    @ConditionalOnProperty(prefix = SEATA_PREFIX, name = {"enableAutoDataSourceProxy", "enable-auto-data-source-proxy"}, havingValue = "true", matchIfMissing = true)
    static class SeataDataSourceConfiguration {

        /**
         * The bean seataDataSourceBeanPostProcessor.
         */
        @Bean(BEAN_NAME_SEATA_DATA_SOURCE_BEAN_POST_PROCESSOR)
        @ConditionalOnMissingBean(SeataDataSourceBeanPostProcessor.class)
        public SeataDataSourceBeanPostProcessor seataDataSourceBeanPostProcessor(SeataProperties seataProperties) {
            return new SeataDataSourceBeanPostProcessor(seataProperties.getExcludesForAutoProxying(), seataProperties.getDataSourceProxyMode());
        }

        /**
         * The bean seataAutoDataSourceProxyCreator.
         */
        @Bean(BEAN_NAME_SEATA_AUTO_DATA_SOURCE_PROXY_CREATOR)
        @ConditionalOnMissingBean(SeataAutoDataSourceProxyCreator.class)
        public SeataAutoDataSourceProxyCreator seataAutoDataSourceProxyCreator(SeataProperties seataProperties) {
            return new SeataAutoDataSourceProxyCreator(seataProperties.isUseJdkProxy(),
                    seataProperties.getExcludesForAutoProxying(), seataProperties.getDataSourceProxyMode());
        }
    }
}
