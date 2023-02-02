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

import java.util.Set;

import io.seata.common.exception.NotSupportYetException;
import io.seata.common.executor.Cacheable;
import io.seata.common.executor.Cleanable;
import io.seata.common.executor.Initialize;
import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.config.changelistener.ConfigurationChangeListener;
import io.seata.config.changelistener.ConfigurationChangeListenerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Configuration factory.
 *
 * @author slievrly
 * @author Geng Zhang
 * @author wang.liang
 */
public final class ConfigurationFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationFactory.class);


    //region Configuration

    private static volatile Configuration instance = null;

    private static volatile boolean initializationStarted = false;

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

        if (instance instanceof Initialize) {
            Initialize initialize = (Initialize)instance;

            if (!initializationStarted) {
                synchronized (Configuration.class) {
                    if (!initializationStarted) {
                        initializationStarted = true;
                        initialize.init();
                    }
                }
            } else if (!initialize.isInitialized()) {
                LOGGER.warn("Current configuration '{}' has not been fully initialized. Some config source may not be available.",
                        instance.getName());
            }
        }

        return instance;
    }

    private static Configuration buildConfiguration() {
        ConfigurationBuilder configurationBuilder = EnhancedServiceLoader.load(ConfigurationBuilder.class);
        return configurationBuilder.build();
    }

    public static void clean() {
        if (instance instanceof Cleanable) {
            ((Cleanable)instance).clean();
        }
    }

    public static void removeCache(String dataId) {
        if (instance instanceof Cacheable) {
            ((Cacheable)instance).removeCache(dataId);
        }
    }

    public static void cleanCaches() {
        if (instance instanceof Cacheable) {
            ((Cacheable)instance).cleanCaches();
        }
    }

    /**
     * Reload the instance.
     */
    static void reload() {
        clean();
        instance = null;
        getInstance();
    }

    //endregion


    //region UpdatableConfiguration

    public static UpdatableConfiguration getUpdatableConfiguration() {
        Configuration instance = getInstance();
        if (instance instanceof UpdatableConfiguration) {
            return (UpdatableConfiguration)instance;
        } else {
            throw new NotSupportYetException("Current configuration is not a " + UpdatableConfiguration.class.getSimpleName() + ".");
        }
    }


    // putConfig
    public static boolean putConfig(String dataId, String content, long timeoutMills) {
        return getUpdatableConfiguration().putConfig(dataId, content, timeoutMills);
    }

    public static boolean putConfig(String dataId, String content) {
        return getUpdatableConfiguration().putConfig(dataId, content);
    }

    // putConfigIfAbsent
    public static boolean putConfigIfAbsent(String dataId, String content, long timeoutMills) {
        return getUpdatableConfiguration().putConfigIfAbsent(dataId, content, timeoutMills);
    }

    public static boolean putConfigIfAbsent(String dataId, String content) {
        return getUpdatableConfiguration().putConfigIfAbsent(dataId, content);
    }

    // removeConfig
    public static boolean removeConfig(String dataId, long timeoutMills) {
        return getUpdatableConfiguration().removeConfig(dataId, timeoutMills);
    }

    public static boolean removeConfig(String dataId) {
        return getUpdatableConfiguration().removeConfig(dataId);
    }

    //endregion


    //region ConfigurationChangeListenerManager

    public static ConfigurationChangeListenerManager getConfigChangeListenerManager() {
        Configuration instance = getInstance();
        if (instance instanceof ConfigurationChangeListenerManager) {
            return (ConfigurationChangeListenerManager)instance;
        } else {
            throw new NotSupportYetException("Current configuration is not a " + ConfigurationChangeListenerManager.class.getSimpleName() + ".");
        }
    }


    public static void addConfigListener(String dataId, ConfigurationChangeListener listener) {
        getConfigChangeListenerManager().addConfigListener(dataId, listener);
    }

    public static void removeConfigListener(String dataId, ConfigurationChangeListener listener) {
        getConfigChangeListenerManager().removeConfigListener(dataId, listener);
    }

    public static Set<String> getListenedConfigDataIds() {
        return getConfigChangeListenerManager().getListenedConfigDataIds();
    }

    public static Set<ConfigurationChangeListener> getConfigListeners(String dataId) {
        return getConfigChangeListenerManager().getConfigListeners(dataId);
    }

    //endregion
}
