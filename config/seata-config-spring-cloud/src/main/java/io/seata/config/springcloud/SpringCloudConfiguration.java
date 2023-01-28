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
package io.seata.config.springcloud;

import java.util.Set;

import io.seata.common.holder.ObjectHolder;
import io.seata.config.AbstractRemoteConfiguration;
import io.seata.config.ConfigurationChangeListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

public class SpringCloudConfiguration extends AbstractRemoteConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpringCloudConfiguration.class);
    private static final String CONFIG_TYPE = "SpringCloudConfig";
    private static volatile SpringCloudConfiguration instance;
    private static final String PREFIX = "seata.";

    public static SpringCloudConfiguration getInstance() {
        if (instance == null) {
            synchronized (SpringCloudConfiguration.class) {
                if (instance == null) {
                    instance = new SpringCloudConfiguration();
                }
            }
        }
        return instance;
    }

    private SpringCloudConfiguration() {

    }

    @Override
    public String getTypeName() {
        return CONFIG_TYPE;
    }

    @Override
    public String getRemoteConfig(String dataId, long timeoutMills) {
        ApplicationContext applicationContext = ObjectHolder.INSTANCE.getObject(ApplicationContext.class);
        if (applicationContext == null || applicationContext.getEnvironment() == null) {
            return null;
        }
        return applicationContext.getEnvironment().getProperty(PREFIX + dataId);
    }

    @Override
    public boolean putConfig(String dataId, String content, long timeoutMills) {
        return false;
    }

    @Override
    public boolean putConfigIfAbsent(String dataId, String content, long timeoutMills) {
        return false;
    }

    @Override
    public boolean removeConfig(String dataId, long timeoutMills) {
        return false;
    }

    @Override
    public void addConfigListener(String dataId, ConfigurationChangeListener listener) {
        LOGGER.warn("dynamic listening is not supported spring cloud config");
    }

    @Override
    public void removeConfigListener(String dataId, ConfigurationChangeListener listener) {
    }

    @Override
    public Set<ConfigurationChangeListener> getConfigListeners(String dataId) {
        return null;
    }
}
