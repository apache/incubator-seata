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

import java.util.List;
import java.util.Set;

import io.seata.common.Cleanable;
import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.config.listener.ConfigurationChangeListener;
import io.seata.config.processor.ConfigurationProcessor;

/**
 * The type Configuration factory.
 *
 * @author slievrly
 * @author Geng Zhang
 * @author wang.liang
 */
public final class ConfigurationFactory {

    //region Configuration instance related

    private static volatile Configuration instance = null;
    private static volatile boolean initialized = false;

    /**
     * Gets instance.
     *
     * @param waitInit whether wait init
     * @return the instance
     */
    public static Configuration getInstance(boolean waitInit) {
        if (instance == null) {
            synchronized (Configuration.class) {
                if (instance == null) {
                    instance = buildConfiguration();
                }
            }
        }

        if (!initialized && waitInit) {
            synchronized (Configuration.class) {
                if (!initialized) {
                    initConfiguration();
                    initialized = true;
                }
            }
        }

        return instance;
    }

    public static Configuration getInstance() {
        return getInstance(true);
    }


    private static Configuration buildConfiguration() {
        ConfigurationBuilder configurationBuilder = EnhancedServiceLoader.load(ConfigurationBuilder.class);
        return configurationBuilder.build();
    }

    private static void initConfiguration() {
        List<ConfigurationProcessor> processors = EnhancedServiceLoader.loadAll(ConfigurationProcessor.class);
        for (ConfigurationProcessor processor : processors) {
            processor.process(instance);
        }
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

    public static Set<ConfigurationChangeListener> getConfigListeners(String dataId) {
        return getInstance().getConfigListeners(dataId);
    }

    //endregion
}
