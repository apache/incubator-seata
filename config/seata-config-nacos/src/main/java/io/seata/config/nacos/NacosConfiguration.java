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
package io.seata.config.nacos;

import java.util.List;
import java.util.Properties;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;

import io.seata.common.exception.NotSupportYetException;
import io.seata.config.AbstractConfiguration;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.config.ConfigurationKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Nacos configuration.
 *
 * @author jimin.jm @alibaba-inc.com
 * @date 2019 /2/1
 */
public class NacosConfiguration extends AbstractConfiguration<Listener> {
    private static volatile NacosConfiguration instance;

    private static final Logger LOGGER = LoggerFactory.getLogger(NacosConfiguration.class);
    private static final String SEATA_GROUP = "SEATA_GROUP";
    private static final String PRO_SERVER_ADDR_KEY = "serverAddr";
    private static final String CONFIG_TYPE = "nacos";
    private static final String DEFAULT_NAMESPACE = "";
    private static final String PRO_NAMESPACE_KEY = "namespace";
    private static final Configuration FILE_CONFIG = ConfigurationFactory.CURRENT_FILE_INSTANCE;
    private static volatile ConfigService configService;

    /**
     * Get instance of NacosConfiguration
     *
     * @return
     */
    public static NacosConfiguration getInstance() {
        if (null == instance) {
            synchronized (NacosConfiguration.class) {
                if (null == instance) {
                    instance = new NacosConfiguration();
                }
            }
        }
        return instance;
    }

    /**
     * Instantiates a new Nacos configuration.
     */
    public NacosConfiguration() {
        if (null == configService) {
            try {
                configService = NacosFactory.createConfigService(getConfigProperties());
            } catch (NacosException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public String getConfig(String dataId, String defaultValue, long timeoutMills) {
        String value;
        if ((value = getConfigFromSysPro(dataId)) != null) {
            return value;
        }
        try {
            value = configService.getConfig(dataId, SEATA_GROUP, timeoutMills);
        } catch (NacosException exx) {
            LOGGER.error(exx.getErrMsg());
        }
        return value == null ? defaultValue : value;
    }

    @Override
    public boolean putConfig(String dataId, String content, long timeoutMills) {
        boolean result = false;
        try {
            result = configService.publishConfig(dataId, SEATA_GROUP, content);
        } catch (NacosException exx) {
            LOGGER.error(exx.getErrMsg());
        }
        return result;
    }

    @Override
    public boolean putConfigIfAbsent(String dataId, String content, long timeoutMills) {
        throw new NotSupportYetException("not support atomic operation putConfigIfAbsent");
    }

    @Override
    public boolean removeConfig(String dataId, long timeoutMills) {
        boolean result = false;
        try {
            result = configService.removeConfig(dataId, SEATA_GROUP);
        } catch (NacosException exx) {
            LOGGER.error(exx.getErrMsg());
        }
        return result;
    }

    @Override
    public void addConfigListener(String dataId, Listener listener) {
        try {
            configService.addListener(dataId, SEATA_GROUP, listener);
        } catch (NacosException exx) {
            LOGGER.error(exx.getErrMsg());
        }
    }

    @Override
    public void removeConfigListener(String dataId, Listener listener) {
        configService.removeListener(dataId, SEATA_GROUP, listener);
    }

    @Override
    public List<Listener> getConfigListeners(String dataId) {
        throw new NotSupportYetException("not support getConfigListeners");
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

        if (null != System.getProperty(PRO_NAMESPACE_KEY)) {
            properties.setProperty(PRO_NAMESPACE_KEY, System.getProperty(PRO_NAMESPACE_KEY));
        } else {
            String namespace = FILE_CONFIG.getConfig(getNacosNameSpaceFileKey());
            if (null == namespace) {
                namespace = DEFAULT_NAMESPACE;
            }
            properties.setProperty(PRO_NAMESPACE_KEY, namespace);
        }
        return properties;
    }

    private static String getNacosNameSpaceFileKey() {
        return ConfigurationKeys.FILE_ROOT_CONFIG + ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR + CONFIG_TYPE
                + ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR
                + PRO_NAMESPACE_KEY;
    }

    private static String getNacosAddrFileKey() {
        return ConfigurationKeys.FILE_ROOT_CONFIG + ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR + CONFIG_TYPE
            + ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR
            + PRO_SERVER_ADDR_KEY;
    }

    @Override
    public String getTypeName() {
        return CONFIG_TYPE;
    }
}
