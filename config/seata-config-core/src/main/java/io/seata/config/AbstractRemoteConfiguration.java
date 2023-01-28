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
public abstract class AbstractRemoteConfiguration extends AbstractConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractRemoteConfiguration.class);


    private Configuration fileConfiguration;


    @Override
    public boolean isRemoteConfiguration() {
        return true;
    }

    @Override
    public String getLatestConfig(String dataId, String defaultValue, long timeoutMills) {
        String value = getRemoteConfig(dataId, timeoutMills);

        if (value == null && fileConfiguration != null) {
            LOGGER.debug("the remote config '{}' is null, load from the file configuration", dataId);
            return fileConfiguration.getLatestConfig(dataId, defaultValue, timeoutMills);
        } else {
            return value == null ? defaultValue : value;
        }
    }

    /**
     * Get remote config
     *
     * @param dataId       the data id
     * @param timeoutMills the timeout mills
     * @return the remote config
     */
    protected abstract String getRemoteConfig(String dataId, long timeoutMills);


    public Configuration getFileConfiguration() {
        return fileConfiguration;
    }

    public void setFileConfiguration(Configuration fileConfig) {
        this.fileConfiguration = fileConfig;
    }
}
