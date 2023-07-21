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

import io.seata.common.util.CollectionUtils;
import io.seata.common.util.StringUtils;
import io.seata.config.ConfigurationFactory;
import io.seata.config.FileConfiguration;
import io.seata.config.file.FileConfig;
import io.seata.server.store.StoreConfig;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import static io.seata.common.ConfigurationKeys.FILE_ROOT_PREFIX_CONFIG;
import static io.seata.common.ConfigurationKeys.FILE_ROOT_PREFIX_REGISTRY;
import static io.seata.common.ConfigurationKeys.METRICS_PREFIX;
import static io.seata.common.ConfigurationKeys.SEATA_FILE_PREFIX_ROOT_CONFIG;
import static io.seata.common.ConfigurationKeys.SERVER_PREFIX;
import static io.seata.common.ConfigurationKeys.STORE_PREFIX;
import static io.seata.common.ConfigurationKeys.TRANSPORT_PREFIX;

@Order(Ordered.HIGHEST_PRECEDENCE)
public class SeataPropertiesLoader implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    
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
    }

}
