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

import java.util.Map;
import java.util.Properties;

import static io.seata.common.ConfigurationKeys.FILE_ROOT_PREFIX_CONFIG;
import static io.seata.common.ConfigurationKeys.FILE_ROOT_PREFIX_REGISTRY;
import static io.seata.common.ConfigurationKeys.SEATA_FILE_PREFIX_ROOT_CONFIG;

@Order(Ordered.HIGHEST_PRECEDENCE)
public class SeataPropertiesLoader implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        ConfigurableEnvironment environment = applicationContext.getEnvironment();
        FileConfiguration configuration = ConfigurationFactory.getOriginFileInstance();
        FileConfig fileConfig = configuration.getFileConfig();
        Map<String, Object> configs = fileConfig.getAllConfig();
        if(CollectionUtils.isNotEmpty(configs)) {
            Properties properties = new Properties();
            configs.forEach((k, v) -> {
                if(v instanceof String){
                    if(StringUtils.isEmpty((String)v)){
                        return;
                    }
                }
                // Convert the configuration name to the configuration name under Spring Boot
                if (k.startsWith(FILE_ROOT_PREFIX_REGISTRY) || k.startsWith(FILE_ROOT_PREFIX_CONFIG)) {
                    properties.put(SEATA_FILE_PREFIX_ROOT_CONFIG + k, v);
                }
            });
            environment.getPropertySources().addLast(new PropertiesPropertySource("seataRegistryConfig", properties));
        }
        // Load by priority
        System.setProperty("sessionMode", StoreConfig.getSessionMode().getName());
        System.setProperty("lockMode", StoreConfig.getLockMode().getName());
    }

}
