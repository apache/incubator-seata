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
package io.seata.server.spring.listener;

import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.common.metadata.Instance;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.StringUtils;
import io.seata.config.ConfigurationFactory;
import io.seata.config.FileConfiguration;
import io.seata.config.file.FileConfig;
import io.seata.server.store.StoreConfig;
import io.seata.server.store.VGroupMappingStoreManager;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;

import java.util.*;

import static io.seata.common.ConfigurationKeys.*;
import static io.seata.common.ConfigurationKeys.FILE_ROOT_REGISTRY;

@Order(Ordered.HIGHEST_PRECEDENCE)
public class SeataPropertiesLoader implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final String SEATA_ROOT_KEY = "seata";
    private static final String REGISTRY_TYPE = "namingserver";
    private static final String NAMESPACE_KEY = "namespace";
    private static final String DEFAULT_NAMESPACE = "public";
    private static final String CLUSTER_NAME_KEY = "cluster";
    private static final String DEFAULT_CLUSTER_NAME = "default";
    
    List<String> prefixList = Arrays.asList(FILE_ROOT_PREFIX_CONFIG, FILE_ROOT_PREFIX_REGISTRY, SERVER_PREFIX,
        STORE_PREFIX, METRICS_PREFIX, TRANSPORT_PREFIX);

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        ConfigurableEnvironment environment = applicationContext.getEnvironment();
        FileConfiguration configuration = ConfigurationFactory.getOriginFileInstanceRegistry();
        FileConfig fileConfig = configuration.getFileConfig();
        Map<String, Object> configs = fileConfig.getAllConfig();
        if (CollectionUtils.isNotEmpty(configs)) {
            Optional<FileConfiguration> originFileInstance = ConfigurationFactory.getOriginFileInstance();
            originFileInstance
                .ifPresent(fileConfiguration -> configs.putAll(fileConfiguration.getFileConfig().getAllConfig()));
            Properties properties = new Properties();
            configs.forEach((k, v) -> {
                if (v instanceof String) {
                    if (StringUtils.isEmpty((String)v)) {
                        return;
                    }
                }
                // Convert the configuration name to the configuration name under Spring Boot
                if (prefixList.stream().anyMatch(k::startsWith)) {
                    properties.put(SEATA_FILE_PREFIX_ROOT_CONFIG + k, v);
                }
            });
            environment.getPropertySources().addLast(new PropertiesPropertySource("seataOldConfig", properties));
        }
        // Load by priority
        System.setProperty("sessionMode", StoreConfig.getSessionMode().getName());
        System.setProperty("lockMode", StoreConfig.getLockMode().getName());

        // load node properties
        Instance instance = Instance.getInstance();

        // load namespace
        String namespaceKey = String.join(FILE_CONFIG_SPLIT_CHAR, SEATA_ROOT_KEY, FILE_ROOT_REGISTRY, REGISTRY_TYPE, NAMESPACE_KEY);
        String namespace = environment.getProperty(namespaceKey, DEFAULT_NAMESPACE);
        instance.setNamespace(namespace);

        // load cluster name
        String clusterNameKey = String.join(FILE_CONFIG_SPLIT_CHAR, SEATA_ROOT_KEY, FILE_ROOT_REGISTRY, REGISTRY_TYPE, CLUSTER_NAME_KEY);
        String clusterName = environment.getProperty(clusterNameKey, DEFAULT_CLUSTER_NAME);
        instance.setClusterName(clusterName);

        // load cluster type
        String clusterType=environment.getProperty("seata.store.mode","default");
        instance.addMetadata("cluster-type",clusterType.equals("raft")?clusterType:"default");

        // load unit name
        instance.setUnit(String.valueOf(UUID.randomUUID()));




        // load metadata
        String prefix = "seata.registry.metadata.";
        for (PropertySource<?> propertySource : environment.getPropertySources()) {
            if (propertySource instanceof EnumerablePropertySource) {
                EnumerablePropertySource<?> enumerablePropertySource = (EnumerablePropertySource<?>) propertySource;
                for (String propertyName : enumerablePropertySource.getPropertyNames()) {
                    if (propertyName.startsWith(prefix)) {
                        instance.addMetadata(propertyName.substring(prefix.length()), enumerablePropertySource.getProperty(propertyName));
                    }
                }
            }
        }

        // load vgroup mapping relationship
        String storeType= ConfigurationFactory.getInstance().getConfig("store.mode","db");
        VGroupMappingStoreManager vGroupMappingStoreManager = EnhancedServiceLoader.load(VGroupMappingStoreManager.class, storeType);
        instance.addMetadata("vGroup", vGroupMappingStoreManager.load());

    }

}
