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
package org.apache.seata.spring.boot.autoconfigure.loader;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import org.apache.seata.common.holder.ObjectHolder;
import org.apache.seata.common.util.CollectionUtils;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.config.FileConfiguration;
import org.apache.seata.config.file.FileConfig;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;

import static org.apache.seata.common.ConfigurationKeys.FILE_ROOT_PREFIX_CONFIG;
import static org.apache.seata.common.ConfigurationKeys.FILE_ROOT_PREFIX_REGISTRY;
import static org.apache.seata.common.ConfigurationKeys.METRICS_PREFIX;
import static org.apache.seata.common.ConfigurationKeys.SEATA_FILE_PREFIX_ROOT_CONFIG;
import static org.apache.seata.common.ConfigurationKeys.SERVER_PREFIX;
import static org.apache.seata.common.ConfigurationKeys.STORE_PREFIX;
import static org.apache.seata.common.ConfigurationKeys.TRANSPORT_PREFIX;
import static org.apache.seata.common.Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT;

@Order(Ordered.HIGHEST_PRECEDENCE)
public class SeataPropertiesLoader implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    
    List<String> prefixList = Arrays.asList(FILE_ROOT_PREFIX_CONFIG, FILE_ROOT_PREFIX_REGISTRY, SERVER_PREFIX,
        STORE_PREFIX, METRICS_PREFIX, TRANSPORT_PREFIX);

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        ConfigurableEnvironment environment = applicationContext.getEnvironment();
        if (ObjectHolder.INSTANCE.getObject(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT) == null) {
            ObjectHolder.INSTANCE.setObject(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT,
                    applicationContext.getEnvironment());
        }
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
        loadSessionAndLockModes();
    }

    public void loadSessionAndLockModes() {
        try {
            Class<?> storeConfigClass = Class.forName("org.apache.seata.server.store.StoreConfig");
            Optional<String> sessionMode = invokeEnumMethod(storeConfigClass, "getSessionMode", "getName");
            Optional<String> lockMode = invokeEnumMethod(storeConfigClass, "getLockMode", "getName");
            sessionMode.ifPresent(value -> System.setProperty("sessionMode", value));
            lockMode.ifPresent(value -> System.setProperty("lockMode", value));
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            // The exception is not printed because it is an expected behavior and does not affect the normal operation of the program.
            // StoreConfig only exists on the server side
        }
    }

    private Optional<String> invokeEnumMethod(Class<?> clazz, String enumMethodName, String getterMethodName)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method enumMethod = clazz.getMethod(enumMethodName);
        Object enumValue = enumMethod.invoke(null);
        if (enumValue != null) {
            Method getterMethod = enumValue.getClass().getMethod(getterMethodName);
            return Optional.ofNullable((String) getterMethod.invoke(enumValue));
        }
        return Optional.empty();
    }
}
