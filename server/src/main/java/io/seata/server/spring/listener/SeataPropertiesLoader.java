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

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.seata.common.ConfigurationKeys;
import io.seata.common.util.StringUtils;
import io.seata.server.store.StoreConfig;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.util.Properties;

import static io.seata.config.FileConfiguration.SYS_FILE_RESOURCE_PREFIX;

public class SeataPropertiesLoader implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    Resource resource;

    private ResourceLoader resourceLoader;

    private ConfigurableEnvironment environment;

    private void loadSeataConfig() {
        Properties properties = new Properties();
        try {
            Config appConfig = ConfigFactory.parseFileAnySyntax(resource.getFile());
            appConfig.entrySet().forEach(entry -> properties.put(
                ConfigurationKeys.SEATA_FILE_ROOT_CONFIG + ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR + entry.getKey(),
                entry.getValue().unwrapped()));
            String configPath = appConfig.getString("registry.file.name");
            if (StringUtils.isNotBlank(configPath) && configPath.startsWith(SYS_FILE_RESOURCE_PREFIX)) {
                Resource fileResource = resourceLoader.getResource(configPath);
                appConfig = ConfigFactory.parseFileAnySyntax(fileResource.getFile());
            }
            appConfig.entrySet().forEach(entry -> properties.put(
                ConfigurationKeys.SEATA_FILE_ROOT_CONFIG + ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR + entry.getKey(),
                entry.getValue().unwrapped()));
            environment.getPropertySources().addLast(new PropertiesPropertySource("SeataRegistryConfig", properties));
        } catch (IOException e) {
            throw new RuntimeException("load seata registry config error: " + resource.getFilename(), e);
        }
    }

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        this.resourceLoader = applicationContext;
        this.environment = applicationContext.getEnvironment();
        String registryName = environment.resolvePlaceholders("${SEATA_CONFIG_NAME:}");
        if (StringUtils.isNotBlank(registryName)) {
            resource = resourceLoader.getResource(registryName);
        }
        loadSeataConfig();
        // Load by priority
        System.setProperty("sessionMode", StoreConfig.getSessionMode().getName());
        System.setProperty("lockMode", StoreConfig.getLockMode().getName());
    }

}
