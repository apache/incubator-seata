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
package io.seata.config;

import java.util.Objects;

import io.seata.common.exception.NotSupportYetException;
import io.seata.common.loader.EnhancedServiceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Configuration factory.
 * <p>vergilyn-comment, 2020-02-13 >>>> <br/>
 *   单例类，seata-server启动时读取服务端的`register.conf`（静态代码块）
 * </p>
 * @author slievrly
 * @author Geng Zhang
 */
public final class ConfigurationFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationFactory.class);

    private static final String REGISTRY_CONF_PREFIX = "registry";
    private static final String REGISTRY_CONF_SUFFIX = ".conf";
    private static final String ENV_SYSTEM_KEY = "SEATA_ENV";
    public static final String ENV_PROPERTY_KEY = "seataEnv";

    private static final String SYSTEM_PROPERTY_SEATA_CONFIG_NAME = "seata.config.name";

    private static final String ENV_SEATA_CONFIG_NAME = "SEATA_CONFIG_NAME";

    /**
     * <p>vergilyn-comment, 2020-02-13 >>>> <br/>
     *   例如`register.conf`的配置
     * </p>
     * @see #instance instance - `register.conf`中对应的`config.type`对应的配置，例如`file.conf`
     */
    public static final Configuration CURRENT_FILE_INSTANCE;

    static {
        String seataConfigName = System.getProperty(SYSTEM_PROPERTY_SEATA_CONFIG_NAME);
        if (null == seataConfigName) {
            seataConfigName = System.getenv(ENV_SEATA_CONFIG_NAME);
        }
        if (null == seataConfigName) {
            seataConfigName = REGISTRY_CONF_PREFIX;
        }
        String envValue = System.getProperty(ENV_PROPERTY_KEY);
        if (null == envValue) {
            envValue = System.getenv(ENV_SYSTEM_KEY);
        }
        Configuration configuration = (null == envValue) ? new FileConfiguration(seataConfigName + REGISTRY_CONF_SUFFIX,
            false) : new FileConfiguration(seataConfigName + "-" + envValue + REGISTRY_CONF_SUFFIX, false);

        /* vergilyn-comment, 2020-02-18 >>>>
         *   如果seata-client中引入了`seata-spring-boot-starter`需要特别小心，因为其存在`class SpringBootConfigurationProvider implements ExtConfigurationProvider`
         *   导致的结果是，如果seata-client中期望读取`register.conf`中的配置，其解析结果是不一定对的！
         *   例如`register.conf`中"register.type = nacos"，最后其实并没读取该值，而是获取的{@linkplain io.seata.spring.boot.autoconfigure.properties.registry.RegistryProperties#type}
         */
        Configuration extConfiguration = null;
        try {
            extConfiguration = EnhancedServiceLoader.load(ExtConfigurationProvider.class).provide(configuration);
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("load extConfiguration:{}",
                    extConfiguration == null ? null : extConfiguration.getClass().getSimpleName());
            }
        } catch (Exception e) {
            LOGGER.warn("failed to load extConfiguration:{}", e.getMessage(), e);
        }
        CURRENT_FILE_INSTANCE = null == extConfiguration ? configuration : extConfiguration;
    }

    private static final String NAME_KEY = "name";
    private static final String FILE_TYPE = "file";

    /**
     * <p>vergilyn-comment, 2020-02-13 >>>> <br/>
     *   例如`file.conf`的配置
     * </p>
     * @see #CURRENT_FILE_INSTANCE CURRENT_FILE_INSTANCE - `register.conf`的配置
     */
    private static volatile Configuration instance = null;

    /**
     * Gets instance.
     *
     * @return the instance
     */
    public static Configuration getInstance() {
        if (instance == null) {
            synchronized (Configuration.class) {
                if (instance == null) {
                    instance = buildConfiguration();
                }
            }
        }
        return instance;
    }

    private static Configuration buildConfiguration() {
        ConfigType configType = null;
        String configTypeName = null;
        try {
            // vergilyn-comment, 2020-02-13 >>>> 从已加载的`register.conf`获取配置参数`conf.type = "file"`
            configTypeName = CURRENT_FILE_INSTANCE.getConfig(
                ConfigurationKeys.FILE_ROOT_CONFIG + ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR
                    + ConfigurationKeys.FILE_ROOT_TYPE);
            configType = ConfigType.getType(configTypeName);
        } catch (Exception e) {
            throw new NotSupportYetException("not support register type: " + configTypeName, e);
        }
        if (ConfigType.File == configType) {
            // vergilyn-comment, 2020-02-13 >>>> 通过配置`conf.file.name = "file.conf"`读取conf
            String pathDataId = ConfigurationKeys.FILE_ROOT_CONFIG + ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR
                + FILE_TYPE + ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR + NAME_KEY;
            String name = CURRENT_FILE_INSTANCE.getConfig(pathDataId);
            Configuration configuration = new FileConfiguration(name);
            Configuration extConfiguration = null;
            try {
                extConfiguration = EnhancedServiceLoader.load(ExtConfigurationProvider.class).provide(configuration);
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("load extConfiguration:{}",
                        extConfiguration == null ? null : extConfiguration.getClass().getSimpleName());
                }
            } catch (Exception e) {
                LOGGER.warn("failed to load extConfiguration:{}", e.getMessage(), e);
            }

            return null == extConfiguration ? configuration : extConfiguration;
        } else {
            /* vergilyn-comment, 2020-02-13 >>>>
             *   nacos 、apollo、zk、consul、etcd3等的读取，参考`ConfigurationProvider.class`的实现类
             */
            return EnhancedServiceLoader.load(ConfigurationProvider.class, Objects.requireNonNull(configType).name())
                .provide();
        }
    }
}
