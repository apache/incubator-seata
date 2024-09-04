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
package io.seata.config.extend;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.AbstractSharedListener;
import com.alibaba.nacos.api.exception.NacosException;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.seata.config.ConfigurationChangeEvent;
import io.seata.config.ConfigurationChangeListener;
import org.apache.seata.common.exception.NotSupportYetException;
import org.apache.seata.common.util.CollectionUtils;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.config.ConfigurationKeys;
import org.apache.seata.config.processor.ConfigProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test for io.seata.config SPI impl
 */
public class NacosConfiguration extends io.seata.config.AbstractConfiguration {
    private static volatile NacosConfiguration instance;

    private static final Logger LOGGER = LoggerFactory.getLogger(NacosConfiguration.class);
    private static final String DEFAULT_GROUP = "SEATA_GROUP";
    private static final String DEFAULT_DATA_ID = "seata.properties";
    private static final String GROUP_KEY = "group";
    private static final String PRO_SERVER_ADDR_KEY = "serverAddr";
    private static final String NACOS_DATA_ID_KEY = "dataId";
    private static final String CONFIG_TYPE = "test";
    private static final String DEFAULT_NAMESPACE = "";
    private static final String PRO_NAMESPACE_KEY = "namespace";
    private static final String USER_NAME = "username";
    private static final String PASSWORD = "password";
    private static final String ACCESS_KEY = "accessKey";
    private static final String SECRET_KEY = "secretKey";
    private static final String RAM_ROLE_NAME_KEY = "ramRoleName";
    private static final String USE_PARSE_RULE = "false";
    private static final String CONTEXT_PATH = "contextPath";
    private static final Config FILE_CONFIG = ConfigFactory.load("registry-test.conf");
    private static volatile ConfigService configService;
    private static final int MAP_INITIAL_CAPACITY = 8;
    private static final ConcurrentMap<String, ConcurrentMap<ConfigurationChangeListener, NacosListener>>
        CONFIG_LISTENERS_MAP = new ConcurrentHashMap<>(MAP_INITIAL_CAPACITY);
    private static volatile Properties seataConfig = new Properties();

    /**
     * Get instance of NacosConfiguration
     *
     * @return instance
     */
    public static NacosConfiguration getInstance() {
        if (instance == null) {
            synchronized (NacosConfiguration.class) {
                if (instance == null) {
                    instance = new NacosConfiguration();
                }
            }
        }
        return instance;
    }

    /**
     * Instantiates a new Nacos configuration.
     */
    private NacosConfiguration() {
        if (configService == null) {
            try {
                configService = NacosFactory.createConfigService(getConfigProperties());
            } catch (NacosException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public String getLatestConfig(String dataId, String defaultValue, long timeoutMills) {
        String value = seataConfig.getProperty(dataId);
        if (null == value) {
            try {
                value = configService.getConfig(dataId, getNacosGroup(), timeoutMills);
            } catch (NacosException exx) {
                LOGGER.error(exx.getErrMsg());
            }
        }

        return value == null ? defaultValue : value;
    }

    @Override
    public boolean putConfig(String dataId, String content, long timeoutMills) {
        boolean result = false;
        try {
            if (!seataConfig.isEmpty()) {
                seataConfig.setProperty(dataId, content);
                result = configService.publishConfig(getNacosDataId(), getNacosGroup(), getSeataConfigStr());
            } else {
                result = configService.publishConfig(dataId, getNacosGroup(), content);
            }
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
            if (!seataConfig.isEmpty()) {
                seataConfig.remove(dataId);
                result = configService.publishConfig(getNacosDataId(), getNacosGroup(), getSeataConfigStr());
            } else {
                result = configService.removeConfig(dataId, getNacosGroup());
            }
        } catch (NacosException exx) {
            LOGGER.error(exx.getErrMsg());
        }
        return result;
    }

    @Override
    public void addConfigListener(String dataId, ConfigurationChangeListener listener) {
        if (StringUtils.isBlank(dataId) || listener == null) {
            return;
        }
        try {
            NacosListener nacosListener = new NacosListener(dataId, listener);
            CONFIG_LISTENERS_MAP.computeIfAbsent(dataId, key -> new ConcurrentHashMap<>()).put(listener, nacosListener);
            configService.addListener(dataId, getNacosGroup(), nacosListener);
        } catch (Exception exx) {
            LOGGER.error("add nacos listener error:{}", exx.getMessage(), exx);
        }
    }

    @Override
    public void removeConfigListener(String dataId, ConfigurationChangeListener listener) {
        if (StringUtils.isBlank(dataId) || listener == null) {
            return;
        }
        Set<ConfigurationChangeListener> configChangeListeners = getConfigListeners(dataId);
        if (CollectionUtils.isNotEmpty(configChangeListeners)) {
            for (ConfigurationChangeListener entry : configChangeListeners) {
                if (listener.equals(entry)) {
                    NacosListener nacosListener = null;
                    Map<ConfigurationChangeListener, NacosListener> configListeners = CONFIG_LISTENERS_MAP.get(dataId);
                    if (configListeners != null) {
                        nacosListener = configListeners.get(listener);
                        configListeners.remove(entry);
                    }
                    if (nacosListener != null) {
                        configService.removeListener(dataId, getNacosGroup(), nacosListener);
                    }
                    break;
                }
            }
        }
    }

    @Override
    public Set<ConfigurationChangeListener> getConfigListeners(String dataId) {
        Map<ConfigurationChangeListener, NacosListener> configListeners = CONFIG_LISTENERS_MAP.get(dataId);
        if (CollectionUtils.isNotEmpty(configListeners)) {
            return configListeners.keySet();
        } else {
            return null;
        }
    }

    protected static Properties getConfigProperties() {
        Properties properties = new Properties();
        properties.setProperty(ConfigurationKeys.IS_USE_CLOUD_NAMESPACE_PARSING, USE_PARSE_RULE);
        properties.setProperty(ConfigurationKeys.IS_USE_ENDPOINT_PARSING_RULE, USE_PARSE_RULE);
        if (System.getProperty(PRO_SERVER_ADDR_KEY) != null) {
            properties.setProperty(PRO_SERVER_ADDR_KEY, System.getProperty(PRO_SERVER_ADDR_KEY));
        } else {
            String address = FILE_CONFIG.getString(getNacosAddrFileKey());
            if (address != null) {
                properties.setProperty(PRO_SERVER_ADDR_KEY, address);
            }
        }

        if (System.getProperty(PRO_NAMESPACE_KEY) != null) {
            properties.setProperty(PRO_NAMESPACE_KEY, System.getProperty(PRO_NAMESPACE_KEY));
        } else {
            String namespace = FILE_CONFIG.getString(getNacosNameSpaceFileKey());
            if (namespace == null) {
                namespace = DEFAULT_NAMESPACE;
            }
            properties.setProperty(PRO_NAMESPACE_KEY, namespace);
        }
        return properties;
    }

    public static String getNacosNameSpaceFileKey() {
        return String.join(ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR, ConfigurationKeys.FILE_ROOT_CONFIG, CONFIG_TYPE,
            PRO_NAMESPACE_KEY);
    }

    public static String getNacosAddrFileKey() {
        return String.join(ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR, ConfigurationKeys.FILE_ROOT_CONFIG, CONFIG_TYPE,
            PRO_SERVER_ADDR_KEY);
    }

    public static String getNacosGroupKey() {
        return String.join(ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR, ConfigurationKeys.FILE_ROOT_CONFIG, CONFIG_TYPE,
            GROUP_KEY);
    }

    public static String getNacosDataIdKey() {
        return String.join(ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR, ConfigurationKeys.FILE_ROOT_CONFIG, CONFIG_TYPE,
            NACOS_DATA_ID_KEY);
    }

    private static String getNacosGroup() {
        return FILE_CONFIG.getString(getNacosGroupKey());
    }

    private static String getNacosDataId() {
        return FILE_CONFIG.getString(getNacosDataIdKey());
    }

    private static String getNacosDataType() {
        return ConfigProcessor.resolverConfigDataType(getNacosDataId());
    }

    private static String getSeataConfigStr() {
        StringBuilder sb = new StringBuilder();

        Enumeration<?> enumeration = seataConfig.propertyNames();
        while (enumeration.hasMoreElements()) {
            String key = (String)enumeration.nextElement();
            String property = seataConfig.getProperty(key);
            sb.append(key).append("=").append(property).append("\n");
        }

        return sb.toString();
    }

    @Override
    public String getTypeName() {
        return CONFIG_TYPE;
    }

    /**
     * Non-blocking subscriptions prohibit adding subscriptions in the thread pool to prevent thread termination
     */
    public static class NacosListener extends AbstractSharedListener {
        private final ConfigurationChangeListener listener;

        /**
         * Instantiates a new Nacos listener.
         *
         * @param dataId   the data id
         * @param listener the listener
         */
        public NacosListener(String dataId, ConfigurationChangeListener listener) {
            this.listener = listener;
        }

        /**
         * Gets target listener.
         *
         * @return the target listener
         */
        public ConfigurationChangeListener getTargetListener() {
            return this.listener;
        }

        @Override
        public void innerReceive(String dataId, String group, String configInfo) {
            try {
                //The new configuration method to puts all configurations into a dateId
                if (getNacosDataId().equals(dataId)) {
                    Properties seataConfigNew = new Properties();
                    if (StringUtils.isNotBlank(configInfo)) {
                        try {
                            seataConfigNew = ConfigProcessor.processConfig(configInfo, getNacosDataType());
                        } catch (IOException e) {
                            LOGGER.error("load config properties error", e);
                            return;
                        }
                    }
                    //Get all the monitored dataids and judge whether it has been modified
                    for (Map.Entry<String, ConcurrentMap<ConfigurationChangeListener, NacosListener>> entry :
                        CONFIG_LISTENERS_MAP.entrySet()) {
                        String listenedDataId = entry.getKey();
                        String propertyOld = seataConfig.getProperty(listenedDataId, "");
                        String propertyNew = seataConfigNew.getProperty(listenedDataId, "");
                        if (!propertyOld.equals(propertyNew)) {
                            ConfigurationChangeEvent event = new ConfigurationChangeEvent().setDataId(listenedDataId)
                                .setNewValue(propertyNew).setNamespace(group);

                            ConcurrentMap<ConfigurationChangeListener, NacosListener> configListeners
                                = entry.getValue();
                            for (ConfigurationChangeListener configListener : configListeners.keySet()) {
                                configListener.onProcessEvent(event);
                            }
                        }
                    }

                    seataConfig = seataConfigNew;
                    return;
                }
            } catch (Exception e) {
                LOGGER.error("innerReceive error: {}", e.getMessage(), e);
            }
            //Compatible with old writing
            ConfigurationChangeEvent event = new ConfigurationChangeEvent().setDataId(dataId).setNewValue(configInfo)
                .setNamespace(group);
            listener.onProcessEvent(event);
        }
    }

}
