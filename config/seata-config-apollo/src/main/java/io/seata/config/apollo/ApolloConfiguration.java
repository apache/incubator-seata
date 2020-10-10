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
package io.seata.config.apollo;

import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigService;
import com.ctrip.framework.apollo.enums.PropertyChangeType;
import com.ctrip.framework.apollo.model.ConfigChange;
import io.netty.util.internal.ConcurrentSet;
import io.seata.common.exception.NotSupportYetException;
import io.seata.common.thread.NamedThreadFactory;
import io.seata.common.util.StringUtils;
import io.seata.config.AbstractConfiguration;
import io.seata.config.ConfigFuture;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationChangeEvent;
import io.seata.config.ConfigurationChangeListener;
import io.seata.config.ConfigurationChangeType;
import io.seata.config.ConfigurationFactory;

import static io.seata.config.ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR;
import static io.seata.config.ConfigurationKeys.FILE_ROOT_CONFIG;

/**
 * The type Apollo configuration.
 *
 * @author: kl @kailing.pub
 */
public class ApolloConfiguration extends AbstractConfiguration {

    private static final String REGISTRY_TYPE = "apollo";
    private static final String APP_ID = "appId";
    private static final String APOLLO_META = "apolloMeta";
    private static final String APOLLO_SECRET = "apolloAccesskeySecret";
    private static final String PROP_APP_ID = "app.id";
    private static final String PROP_APOLLO_META = "apollo.meta";
    private static final String PROP_APOLLO_SECRET = "apollo.accesskey.secret";
    private static final String NAMESPACE = "namespace";
    private static final String DEFAULT_NAMESPACE = "application";
    private static final Configuration FILE_CONFIG = ConfigurationFactory.CURRENT_FILE_INSTANCE;
    private static volatile Config config;
    private ExecutorService configOperateExecutor;
    private static final int CORE_CONFIG_OPERATE_THREAD = 1;
    private static final ConcurrentMap<String, Set<ConfigurationChangeListener>> LISTENER_SERVICE_MAP
        = new ConcurrentHashMap<>();
    private static final int MAX_CONFIG_OPERATE_THREAD = 2;
    private static volatile ApolloConfiguration instance;

    private ApolloConfiguration() {
        readyApolloConfig();
        if (config == null) {
            synchronized (ApolloConfiguration.class) {
                if (config == null) {
                    config = ConfigService.getConfig(FILE_CONFIG.getConfig(getApolloNamespaceKey(), DEFAULT_NAMESPACE));
                    configOperateExecutor = new ThreadPoolExecutor(CORE_CONFIG_OPERATE_THREAD,
                        MAX_CONFIG_OPERATE_THREAD, Integer.MAX_VALUE, TimeUnit.MILLISECONDS,
                        new LinkedBlockingQueue<>(),
                        new NamedThreadFactory("apolloConfigOperate", MAX_CONFIG_OPERATE_THREAD));
                    config.addChangeListener(changeEvent -> {
                        for (String key : changeEvent.changedKeys()) {
                            if (!LISTENER_SERVICE_MAP.containsKey(key)) {
                                continue;
                            }
                            ConfigChange change = changeEvent.getChange(key);
                            ConfigurationChangeEvent event = new ConfigurationChangeEvent(key, change.getNamespace(),
                                change.getOldValue(), change.getNewValue(), getChangeType(change.getChangeType()));
                            LISTENER_SERVICE_MAP.get(key).forEach(listener -> listener.onProcessEvent(event));
                        }
                    });
                }
            }
        }
    }

    /**
     * Gets instance.
     *
     * @return the instance
     */
    public static ApolloConfiguration getInstance() {
        if (instance == null) {
            synchronized (ApolloConfiguration.class) {
                if (instance == null) {
                    instance = new ApolloConfiguration();
                }
            }
        }
        return instance;
    }

    @Override
    public String getLatestConfig(String dataId, String defaultValue, long timeoutMills) {
        String value;
        if ((value = getConfigFromSysPro(dataId)) != null) {
            return value;
        }
        ConfigFuture configFuture = new ConfigFuture(dataId, defaultValue, ConfigFuture.ConfigOperation.GET,
            timeoutMills);
        configOperateExecutor.submit(() -> {
            String result = config.getProperty(dataId, defaultValue);
            configFuture.setResult(result);
        });
        return (String)configFuture.get();
    }

    @Override
    public boolean putConfig(String dataId, String content, long timeoutMills) {
        throw new NotSupportYetException("not support putConfig");
    }

    @Override
    public boolean putConfigIfAbsent(String dataId, String content, long timeoutMills) {
        throw new NotSupportYetException("not support atomic operation putConfigIfAbsent");
    }

    @Override
    public boolean removeConfig(String dataId, long timeoutMills) {
        throw new NotSupportYetException("not support removeConfig");
    }

    @Override
    public void addConfigListener(String dataId, ConfigurationChangeListener listener) {
        if (dataId == null || listener == null) {
            return;
        }
        LISTENER_SERVICE_MAP.putIfAbsent(dataId, new ConcurrentSet<>());
        LISTENER_SERVICE_MAP.get(dataId).add(listener);
    }

    @Override
    public void removeConfigListener(String dataId, ConfigurationChangeListener listener) {
        if (!LISTENER_SERVICE_MAP.containsKey(dataId) || listener == null) {
            return;
        }
        LISTENER_SERVICE_MAP.get(dataId).remove(listener);
    }

    @Override
    public Set<ConfigurationChangeListener> getConfigListeners(String dataId) {
        return LISTENER_SERVICE_MAP.get(dataId);
    }

    private void readyApolloConfig() {
        Properties properties = System.getProperties();
        if (!properties.containsKey(PROP_APP_ID)) {
            System.setProperty(PROP_APP_ID, FILE_CONFIG.getConfig(getApolloAppIdFileKey()));
        }
        if (!properties.containsKey(PROP_APOLLO_META)) {
            System.setProperty(PROP_APOLLO_META, FILE_CONFIG.getConfig(getApolloMetaFileKey()));
        }
        if (!properties.containsKey(PROP_APOLLO_SECRET)) {
            String secretKey = FILE_CONFIG.getConfig(getApolloSecretFileKey());
            if (!StringUtils.isBlank(secretKey)) {
                System.setProperty(PROP_APOLLO_SECRET, secretKey);
            }
        }
    }

    @Override
    public String getTypeName() {
        return REGISTRY_TYPE;
    }

    private static String getApolloMetaFileKey() {
        return String.join(FILE_CONFIG_SPLIT_CHAR, FILE_ROOT_CONFIG, REGISTRY_TYPE, APOLLO_META);
    }

    private static String getApolloSecretFileKey() {
        return String.join(FILE_CONFIG_SPLIT_CHAR, FILE_ROOT_CONFIG, REGISTRY_TYPE, APOLLO_SECRET);
    }

    private static String getApolloAppIdFileKey() {
        return String.join(FILE_CONFIG_SPLIT_CHAR, FILE_ROOT_CONFIG, REGISTRY_TYPE, APP_ID);
    }

    private static String getApolloNamespaceKey() {
        return String.join(FILE_CONFIG_SPLIT_CHAR, FILE_ROOT_CONFIG, REGISTRY_TYPE, NAMESPACE);
    }

    private ConfigurationChangeType getChangeType(PropertyChangeType changeType) {
        switch (changeType) {
            case ADDED:
                return ConfigurationChangeType.ADD;
            case DELETED:
                return ConfigurationChangeType.DELETE;
            default:
                return ConfigurationChangeType.MODIFY;
        }
    }
}
