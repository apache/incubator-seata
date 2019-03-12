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

import java.util.List;
import java.util.Properties;

import com.alibaba.fescar.common.exception.NotSupportYetException;
import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Nacos configuration.
 *
 * @author jimin.jm @alibaba-inc.com
 * @date 2019 /2/1
 */
public class NacosConfiguration extends AbstractConfiguration<Listener> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NacosConfiguration.class);
    private static final String FESCAR_GROUP = "FESCAR_GROUP";
    private static final String PRO_SERVER_ADDR_KEY = "serverAddr";
    private static final String REGISTRY_TYPE = "nacos";
    private static final Configuration FILE_CONFIG = ConfigurationFactory.FILE_INSTANCE;
    private static volatile ConfigService configService;

    /**
     * Instantiates a new Nacos configuration.
     *
     * @throws NacosException the nacos exception
     */
    public NacosConfiguration() throws NacosException {
        if (null == configService) {
            configService = NacosFactory.createConfigService(getConfigProperties());
        }
    }

    @Override
    public String getConfig(String dataId, String defaultValue, long timeoutMills) {
        String value;
        try {
            value = configService.getConfig(dataId, FESCAR_GROUP, timeoutMills);
        } catch (NacosException exx) {
            LOGGER.error(exx.getErrMsg());
            value = defaultValue;
        }
        return value == null ? defaultValue : value;
    }

    @Override
    public boolean putConfig(String dataId, String content, long timeoutMills) {
        boolean result = false;
        try {
            result = configService.publishConfig(dataId, FESCAR_GROUP, content);
        } catch (NacosException exx) {
            LOGGER.error(exx.getErrMsg());
        }
        return result;
    }

    @Override
    public boolean putConfigIfAbsent(String dataId, String content, long timeoutMills) {
        throw new NotSupportYetException("not support putConfigIfAbsent");
    }

    @Override
    public boolean removeConfig(String dataId, long timeoutMills) {
        boolean result = false;
        try {
            result = configService.removeConfig(dataId, FESCAR_GROUP);
        } catch (NacosException exx) {
            LOGGER.error(exx.getErrMsg());
        }
        return result;
    }

    @Override
    public void addConfigListener(String dataId, Listener listener) {
        try {
            configService.addListener(dataId, FESCAR_GROUP, listener);
        } catch (NacosException exx) {
            LOGGER.error(exx.getErrMsg());
        }
    }

    @Override
    public void removeConfigListener(String dataId, Listener listener) {
        configService.removeListener(dataId, FESCAR_GROUP, listener);
    }

    @Override
    public List<Listener> getConfigListeners(String dataId) {
        throw new NotSupportYetException("not support putConfigIfAbsent");
    }

    private static Properties getConfigProperties() {
        Properties properties = new Properties();
        if (null != System.getProperty(PRO_SERVER_ADDR_KEY)) {
            properties.setProperty(PRO_SERVER_ADDR_KEY, System.getProperty(PRO_SERVER_ADDR_KEY));
        } else {
            String address = FILE_CONFIG.getConfig(getNacosAddrFileKey());
            if (null != address) {
                properties.setProperty(PRO_SERVER_ADDR_KEY, address);
            }
        }
        return properties;
    }

    private static String getNacosAddrFileKey() {
        return ConfigurationKeys.FILE_ROOT_REGISTRY + ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR + REGISTRY_TYPE
            + ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR
            + PRO_SERVER_ADDR_KEY;
    }

    @Override
    public String getTypeName() {
        return REGISTRY_TYPE;
    }
}
