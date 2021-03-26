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

import io.seata.config.springcloud.SpringApplicationContextProvider;
import io.seata.spring.boot.autoconfigure.properties.SeataProperties;
import io.seata.spring.boot.autoconfigure.properties.client.LockProperties;
import io.seata.spring.boot.autoconfigure.properties.client.LogProperties;
import io.seata.spring.boot.autoconfigure.properties.client.RmProperties;
import io.seata.spring.boot.autoconfigure.properties.client.ServiceProperties;
import io.seata.spring.boot.autoconfigure.properties.client.ShutdownProperties;
import io.seata.spring.boot.autoconfigure.properties.client.ThreadFactoryProperties;
import io.seata.spring.boot.autoconfigure.properties.client.TmProperties;
import io.seata.spring.boot.autoconfigure.properties.client.TransportProperties;
import io.seata.spring.boot.autoconfigure.properties.client.UndoCompressProperties;
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
import io.seata.spring.boot.autoconfigure.properties.registry.LoadBalanceProperties;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import static io.seata.common.Constants.BEAN_NAME_SPRING_APPLICATION_CONTEXT_PROVIDER;
import static io.seata.spring.boot.autoconfigure.StarterConstants.CLIENT_RM_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.CLIENT_TM_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.COMPRESS_PREFIX;
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
import static io.seata.spring.boot.autoconfigure.StarterConstants.LOAD_BALANCE_PREFIX;

/**
 * The type Seata properties auto configuration
 *
 * @author xingfudeshi@gmail.com
 */
@ConditionalOnProperty(prefix = SEATA_PREFIX, name = "enabled", havingValue = "true", matchIfMissing = true)
@ComponentScan(basePackages = "io.seata.spring.boot.autoconfigure.properties")
@AutoConfigureBefore({SeataAutoConfiguration.class, SeataDataSourceAutoConfiguration.class})
public class SeataPropertiesAutoConfiguration {
    static {

        PROPERTY_BEAN_MAP.put(SEATA_PREFIX, SeataProperties.class);

        PROPERTY_BEAN_MAP.put(CLIENT_RM_PREFIX, RmProperties.class);
        PROPERTY_BEAN_MAP.put(CLIENT_TM_PREFIX, TmProperties.class);
        PROPERTY_BEAN_MAP.put(LOCK_PREFIX, LockProperties.class);
        PROPERTY_BEAN_MAP.put(SERVICE_PREFIX, ServiceProperties.class);
        PROPERTY_BEAN_MAP.put(SHUTDOWN_PREFIX, ShutdownProperties.class);
        PROPERTY_BEAN_MAP.put(THREAD_FACTORY_PREFIX, ThreadFactoryProperties.class);
        PROPERTY_BEAN_MAP.put(UNDO_PREFIX, UndoProperties.class);
        PROPERTY_BEAN_MAP.put(COMPRESS_PREFIX, UndoCompressProperties.class);
        PROPERTY_BEAN_MAP.put(LOG_PREFIX, LogProperties.class);
        PROPERTY_BEAN_MAP.put(TRANSPORT_PREFIX, TransportProperties.class);
        PROPERTY_BEAN_MAP.put(CONFIG_PREFIX, ConfigProperties.class);
        PROPERTY_BEAN_MAP.put(CONFIG_FILE_PREFIX, ConfigFileProperties.class);
        PROPERTY_BEAN_MAP.put(REGISTRY_PREFIX, RegistryProperties.class);
        PROPERTY_BEAN_MAP.put(LOAD_BALANCE_PREFIX, LoadBalanceProperties.class);

        PROPERTY_BEAN_MAP.put(CONFIG_NACOS_PREFIX, ConfigNacosProperties.class);
        PROPERTY_BEAN_MAP.put(CONFIG_CONSUL_PREFIX, ConfigConsulProperties.class);
        PROPERTY_BEAN_MAP.put(CONFIG_ZK_PREFIX, ConfigZooKeeperProperties.class);
        PROPERTY_BEAN_MAP.put(CONFIG_APOLLO_PREFIX, ConfigApolloProperties.class);
        PROPERTY_BEAN_MAP.put(CONFIG_ETCD3_PREFIX, ConfigEtcd3Properties.class);
        PROPERTY_BEAN_MAP.put(CONFIG_CUSTOM_PREFIX, ConfigCustomProperties.class);

        PROPERTY_BEAN_MAP.put(REGISTRY_CONSUL_PREFIX, RegistryConsulProperties.class);
        PROPERTY_BEAN_MAP.put(REGISTRY_ETCD3_PREFIX, RegistryEtcd3Properties.class);
        PROPERTY_BEAN_MAP.put(REGISTRY_EUREKA_PREFIX, RegistryEurekaProperties.class);
        PROPERTY_BEAN_MAP.put(REGISTRY_NACOS_PREFIX, RegistryNacosProperties.class);
        PROPERTY_BEAN_MAP.put(REGISTRY_REDIS_PREFIX, RegistryRedisProperties.class);
        PROPERTY_BEAN_MAP.put(REGISTRY_SOFA_PREFIX, RegistrySofaProperties.class);
        PROPERTY_BEAN_MAP.put(REGISTRY_ZK_PREFIX, RegistryZooKeeperProperties.class);
        PROPERTY_BEAN_MAP.put(REGISTRY_CUSTOM_PREFIX, RegistryCustomProperties.class);

    }

    @Bean(BEAN_NAME_SPRING_APPLICATION_CONTEXT_PROVIDER)
    @ConditionalOnMissingBean(name = {BEAN_NAME_SPRING_APPLICATION_CONTEXT_PROVIDER})
    public SpringApplicationContextProvider springApplicationContextProvider() {
        return new SpringApplicationContextProvider();
    }
}
