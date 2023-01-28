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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Abstract remote configuration.
 *
 * @author wang.liang
 */
public abstract class AbstractRemoteConfiguration extends AbstractConfiguration implements RemoteConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractRemoteConfiguration.class);


    private Configuration fileConfiguration;

    private volatile boolean warned = false;


    /**
     * Override for not get config from the system property.
     *
     * @param dataId       the data id
     * @param defaultValue the default value
     * @param timeoutMills the timeout mills
     * @return the config
     */
    @Override
    public String getConfig(String dataId, String defaultValue, long timeoutMills) {
        return getLatestConfig(dataId, defaultValue, timeoutMills);
    }

    @Override
    public String getLatestConfig(String dataId, String defaultValue, long timeoutMills) {
        String value = getRemoteConfig(dataId, timeoutMills);
        if (value != null) {
            return value;
        }

        if (fileConfiguration != null) {
            LOGGER.debug("the remote config '{}' is null, load from the fileConfiguration", dataId);
            return fileConfiguration.getLatestConfig(dataId, defaultValue, timeoutMills);
        } else {
            if (!warned && LOGGER.isWarnEnabled()) {
                warned = true;
                LOGGER.warn("This remote configuration '{}' has no fileConfiguration, Please confirm whether it is a remote configuration.", dataId);
            }
            return defaultValue;
        }
    }


    public Configuration getFileConfiguration() {
        return fileConfiguration;
    }

    public void setFileConfiguration(Configuration fileConfig) {
        this.fileConfiguration = fileConfig;
    }
}
