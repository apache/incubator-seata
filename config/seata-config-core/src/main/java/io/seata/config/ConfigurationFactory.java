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

import io.seata.common.Cleanable;
import io.seata.common.executor.Initialize;
import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.config.listener.ConfigurationChangeListener;
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


    //region Configuration instance related

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
                        instance.getTypeName());
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

    /**
     * Reload the instance.
     */
    static void reload() {
        clean();
        instance = null;
        getInstance();
    }

    //endregion


    //region Config listener related

    public static void addConfigListener(String dataId, ConfigurationChangeListener listener) {
        getInstance().addConfigListener(dataId, listener);
    }

    public static void removeConfigListener(String dataId, ConfigurationChangeListener listener) {
        getInstance().removeConfigListener(dataId, listener);
    }

    public static Set<String> getListenedConfigDataIds() {
        return getInstance().getListenedConfigDataIds();
    }

    public static Set<ConfigurationChangeListener> getConfigListeners(String dataId) {
        return getInstance().getConfigListeners(dataId);
    }

    //endregion
}
