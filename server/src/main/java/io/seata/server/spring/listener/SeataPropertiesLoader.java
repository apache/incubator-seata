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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Properties;

import static io.seata.config.FileConfiguration.SYS_FILE_RESOURCE_PREFIX;

//@ConditionalOnResource(resources = "${SEATA_CONFIG_NAME}")
@Component
public class SeataPropertiesLoader implements ApplicationListener<ContextRefreshedEvent> {

    @Value("${SEATA_CONFIG_NAME:}")
    Resource resource;

    @javax.annotation.Resource
    private ConfigurableEnvironment environment;

    @Override
    public void onApplicationEvent(@Nonnull ContextRefreshedEvent event) {
        Properties properties = new Properties();
        try {
            Config appConfig = ConfigFactory.parseFileAnySyntax(resource.getFile());
            String configPath = properties.getProperty(ConfigurationKeys.CONFIG_FILE_NAME);
            if (StringUtils.isNotBlank(configPath) && configPath.startsWith(SYS_FILE_RESOURCE_PREFIX)) {
                Resource fileResource = environment.getRequiredProperty(configPath, Resource.class);
                appConfig = ConfigFactory.parseFileAnySyntax(fileResource.getFile());
            }
            appConfig.entrySet().forEach(entry -> properties.put(entry.getKey(), entry.getValue().unwrapped()));
            environment.getPropertySources().addLast(new PropertiesPropertySource("SeataRegistryConfig", properties));
        } catch (IOException e) {
            throw new RuntimeException("load seata registry config error: " + resource.getFilename(), e);
        }
    }

}
