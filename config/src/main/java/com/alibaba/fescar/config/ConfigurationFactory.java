/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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
package com.alibaba.fescar.config;

import com.alibaba.fescar.common.exception.NotSupportYetException;
import com.alibaba.nacos.api.exception.NacosException;

import com.ctrip.framework.apollo.exceptions.ApolloConfigException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Configuration factory.
 *
 * @author jimin.jm @alibaba-inc.com
 * @date 2018 /12/24
 */
public final class ConfigurationFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationFactory.class);
    private static final String REGISTRY_CONF = "registry.conf";
    /**
     * The constant FILE_INSTANCE.
     */
    public static final Configuration FILE_INSTANCE = new FileConfiguration(REGISTRY_CONF);
    private static final String NAME_KEY = "name";
    private static final String FILE_TYPE = "file";

    /**
     * Gets instance.
     *
     * @return the instance
     */
    public static Configuration getInstance() {
        ConfigType configType = null;
        try {
            configType = ConfigType.getType(
                FILE_INSTANCE.getConfig(ConfigurationKeys.FILE_ROOT_CONFIG + ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR
                    + ConfigurationKeys.FILE_ROOT_TYPE));
        } catch (Exception exx) {
            LOGGER.error(exx.getMessage());
        }
        Configuration configuration;
        switch (configType) {
            case Nacos:
                try {
                    configuration = new NacosConfiguration();
                } catch (NacosException e) {
                    throw new RuntimeException(e);
                }
                break;
            case Apollo:
                try {
                    configuration = ApolloConfiguration.getInstance();
                } catch (ApolloConfigException e) {
                    throw new RuntimeException(e);
                }
                break;
            case File:
                String pathDataId = ConfigurationKeys.FILE_ROOT_CONFIG + ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR
                    + FILE_TYPE + ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR
                    + NAME_KEY;
                String name = FILE_INSTANCE.getConfig(pathDataId);
                configuration = new FileConfiguration(name);
                break;
            default:
                throw new NotSupportYetException("not support register type:" + configType);
        }
        return configuration;
    }
}
