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
package org.apache.seata.config;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import org.apache.seata.common.exception.NotSupportYetException;
import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.common.loader.EnhancedServiceNotFoundException;
import org.apache.seata.common.util.CollectionUtils;
import org.apache.seata.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Configuration factory.
 *
 */
public final class ConfigurationFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationFactory.class);

    private static final String REGISTRY_CONF_DEFAULT = "registry";
    private static final String ENV_SYSTEM_KEY = "SEATA_ENV";
    public static final String ENV_PROPERTY_KEY = "seataEnv";

    private static final String SYSTEM_PROPERTY_SEATA_CONFIG_NAME = "seata.config.name";

    private static final String ENV_SEATA_CONFIG_NAME = "SEATA_CONFIG_NAME";

    public static volatile Configuration CURRENT_FILE_INSTANCE;

    public static volatile FileConfiguration ORIGIN_FILE_INSTANCE_REGISTRY;

    public static volatile FileConfiguration ORIGIN_FILE_INSTANCE = null;

    static {
        initOriginConfiguration();
        load();
        maybeNeedOriginFileInstance();
    }

    private static void load() {
        Configuration configuration = ORIGIN_FILE_INSTANCE_REGISTRY;
        Configuration extConfiguration = null;
        try {
            extConfiguration = EnhancedServiceLoader.load(ExtConfigurationProvider.class).provide(configuration);
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("load Configuration from :{}",
                    extConfiguration == null ? configuration.getClass().getSimpleName() : "Spring Configuration");
            }
        } catch (EnhancedServiceNotFoundException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("failed to load extConfiguration: {}", e.getMessage(), e);
            }
        } catch (Exception e) {
            LOGGER.error("failed to load extConfiguration: {}", e.getMessage(), e);
        }
        CURRENT_FILE_INSTANCE = extConfiguration == null ? configuration : extConfiguration;
    }

    private static void initOriginConfiguration() {
        String seataConfigName = System.getProperty(SYSTEM_PROPERTY_SEATA_CONFIG_NAME);
        if (seataConfigName == null) {
            seataConfigName = System.getenv(ENV_SEATA_CONFIG_NAME);
        }
        if (seataConfigName == null) {
            seataConfigName = REGISTRY_CONF_DEFAULT;
        }
        String envValue = System.getProperty(ENV_PROPERTY_KEY);
        if (envValue == null) {
            envValue = System.getenv(ENV_SYSTEM_KEY);
        }
        seataConfigName = envValue == null ? seataConfigName : seataConfigName + "-" + envValue;
        // create FileConfiguration for read registry.conf
        ORIGIN_FILE_INSTANCE_REGISTRY = new FileConfiguration(seataConfigName, false);
    }

    public static FileConfiguration getOriginFileInstanceRegistry() {
        return ORIGIN_FILE_INSTANCE_REGISTRY;
    }

    private static final String NAME_KEY = "name";
    private static final String FILE_TYPE = "file";

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

    private static void maybeNeedOriginFileInstance() {
        if (ConfigType.File.name().equalsIgnoreCase(getConfigType())) {
            String pathDataId = String.join(ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR,
                    ConfigurationKeys.FILE_ROOT_CONFIG, FILE_TYPE, NAME_KEY);
            String name = CURRENT_FILE_INSTANCE.getConfig(pathDataId);
            // create FileConfiguration for read file.conf
            ORIGIN_FILE_INSTANCE = new FileConfiguration(name);
        }
    }

    /**
     * Notes: should not rely on the ConfigType type, as it will prevent the extension of configuration types
     * implemented externally.
     * @return
     */
    private static String getConfigType() {
        String configTypeName = CURRENT_FILE_INSTANCE.getConfig(ConfigurationKeys.FILE_ROOT_CONFIG
            + ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR + ConfigurationKeys.FILE_ROOT_TYPE);
        if (StringUtils.isBlank(configTypeName)) {
            throw new NotSupportYetException("config type can not be null");
        }
        return configTypeName;
    }

    public static Optional<FileConfiguration> getOriginFileInstance() {
        return Optional.ofNullable(ORIGIN_FILE_INSTANCE);
    }

    private static Configuration buildConfiguration() {
        String configTypeName = getConfigType();
        Configuration configuration = ORIGIN_FILE_INSTANCE;
        Configuration extConfiguration = getSpringConfiguration();
        if (null == extConfiguration) {
            Configuration springConfiguration = getNonSpringConfiguration(configTypeName);
            if (null != springConfiguration) {
                configuration = springConfiguration;
            }
        }
        try {
            Configuration configurationCache;
            if (null != extConfiguration) {
                configurationCache = ConfigurationCache.getInstance().proxy(extConfiguration);
            } else {
                configurationCache = ConfigurationCache.getInstance().proxy(configuration);
            }
            if (null != configurationCache) {
                extConfiguration = configurationCache;
            }
        } catch (Exception e) {
            LOGGER.error("failed to load configurationCacheProvider:{}", e.getMessage(), e);
        }
        return null == extConfiguration ? configuration : extConfiguration;
    }

    private static Configuration getSpringConfiguration() {
        Configuration configuration = ORIGIN_FILE_INSTANCE;
        if (null != configuration) {
            try {
                Configuration extConfiguration = EnhancedServiceLoader.load(ExtConfigurationProvider.class, false).provide(
                    configuration);
                if (null != extConfiguration) {
                    return extConfiguration;
                }
            } catch (EnhancedServiceNotFoundException ignore) {
                //ignore

            } catch (Exception exx) {
                LOGGER.error("failed to load spring configuration :{}", exx.getMessage(), exx);
            }
        }
        return null;
    }

    private static Configuration getNonSpringConfiguration(String configTypeName) {
        try {
            io.seata.config.Configuration oldConfiguration = EnhancedServiceLoader.load(
                io.seata.config.ConfigurationProvider.class, Objects.requireNonNull(configTypeName)).provide();
            if (null != oldConfiguration) {
                Configuration configurationSPIInstanceProxy = (Configuration)Proxy.newProxyInstance(
                    ConfigurationFactory.class.getClassLoader(), new Class[] {Configuration.class},
                    new OldConfigurationInvocationHandler(oldConfiguration));
                return configurationSPIInstanceProxy;
            }
        } catch (EnhancedServiceNotFoundException ignore) {
            //ignore
        } catch (Exception exx) {
            LOGGER.error("failed to load non-spring configuration :{}", exx.getMessage(), exx);
        }
        try {
            Configuration configuration = EnhancedServiceLoader.load(ConfigurationProvider.class,
                Objects.requireNonNull(configTypeName), false).provide();
            return configuration;
        }  catch (Exception exx) {
            LOGGER.error("failed to load non-spring configuration :{}", exx.getMessage(), exx);
        }
        return null;
    }

    public static void reload() {
        ConfigurationCache.clear();
        initOriginConfiguration();
        load();
        maybeNeedOriginFileInstance();
        instance = null;
        getInstance();
    }

    static class OldConfigurationInvocationHandler implements InvocationHandler {
        private final io.seata.config.Configuration configuration;

        private static final String[] SIMPLE_PARAMS_METHOD_NAMES = new String[] {"getShort", "getInt", "getLong", "getDuration", "getBoolean", "getConfig", "putConfig", "getLatestConfig", "putConfigIfAbsent", "removeConfig", "getConfigFromSys"};

        private static final List<String> SIMPLE_METHOD_NAMES =
            Arrays.stream(SIMPLE_PARAMS_METHOD_NAMES).collect(Collectors.toList());

        public OldConfigurationInvocationHandler(io.seata.config.Configuration configuration) {
            this.configuration = configuration;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (SIMPLE_METHOD_NAMES.contains(method.getName())) {
                Class[] classes = new Class[args.length];
                for (int i = 0; i < args.length; i++) {
                    classes[i] = args[i].getClass();
                }
                Method oldMethod = this.configuration.getClass().getMethod(method.getName(), classes);
                return oldMethod.invoke(configuration, args);
            } else if ("addConfigListener".equals(method.getName()) || "removeConfigListener".equals(
                method.getName())) {
                if (args.length == 2) {
                    if (args[1] instanceof ConfigurationChangeListener) {
                        ConfigurationChangeListener listener = (ConfigurationChangeListener)args[1];
                        OldConfigurationChangeListenerWrapper wrapper = new OldConfigurationChangeListenerWrapper(
                            listener);
                        Method oldMethod = this.configuration.getClass().getMethod(method.getName(),
                            new Class[] {String.class, io.seata.config.ConfigurationChangeListener.class});
                        return oldMethod.invoke(configuration, args[0], wrapper);
                    }
                }
            } else if ("getConfigListeners".equals(method.getName())) {
                Method oldMethod = this.configuration.getClass().getMethod(method.getName(),
                    new Class[] {String.class});
                Set<io.seata.config.ConfigurationChangeListener> listeners
                    = (Set<io.seata.config.ConfigurationChangeListener>)oldMethod.invoke(configuration, args);
                if (CollectionUtils.isEmpty(listeners)) {
                    return null;
                }
                Set<ConfigurationChangeListener> newListeners = new HashSet<>();
                for (io.seata.config.ConfigurationChangeListener listener : listeners) {
                    if (listener instanceof OldConfigurationChangeListenerWrapper) {
                        newListeners.add(((OldConfigurationChangeListenerWrapper)listener).getTargetListener());
                    }
                }
                return newListeners;
            }
            throw new NotSupportYetException(String.format("not support method:%s", method.getName()));
        }
    }

    static class OldConfigurationChangeListenerWrapper implements io.seata.config.ConfigurationChangeListener {
        private final ConfigurationChangeListener listener;

        public OldConfigurationChangeListenerWrapper(ConfigurationChangeListener listener) {
            this.listener = listener;
        }

        private ConfigurationChangeEvent convert(io.seata.config.ConfigurationChangeEvent event) {
            ConfigurationChangeEvent newEvent = new ConfigurationChangeEvent();
            newEvent.setDataId(event.getDataId()).setOldValue(event.getOldValue()).setNewValue(event.getNewValue())
                .setNamespace(event.getNamespace());
            if (event.getChangeType() != null) {
                newEvent.setChangeType(ConfigurationChangeType.values()[event.getChangeType().ordinal()]);
            }
            return newEvent;
        }

        @Override
        public void onChangeEvent(io.seata.config.ConfigurationChangeEvent event) {
            onProcessEvent(event);
        }

        @Override
        public void onProcessEvent(io.seata.config.ConfigurationChangeEvent event) {
            listener.onProcessEvent(convert(event));
        }

        @Override
        public void onShutDown() {
            listener.onShutDown();
        }

        @Override
        public ExecutorService getExecutorService() {
            return listener.getExecutorService();
        }

        @Override
        public void beforeEvent() {
            listener.beforeEvent(null);
        }

        @Override
        public void afterEvent() {
            listener.afterEvent(null);
        }

        public ConfigurationChangeListener getTargetListener() {
            return listener;
        }
    }
}
