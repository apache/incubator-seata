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
    private static final String APP_ID = "app.id";
    private static final String APOLLO_META = "apollo.meta";
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
        if (null == config) {
            synchronized (ApolloConfiguration.class) {
                if (null == config) {
                    config = ConfigService.getAppConfig();
                    configOperateExecutor = new ThreadPoolExecutor(CORE_CONFIG_OPERATE_THREAD,
                        MAX_CONFIG_OPERATE_THREAD, Integer.MAX_VALUE, TimeUnit.MILLISECONDS,
                        new LinkedBlockingQueue<>(),
                        new NamedThreadFactory("apolloConfigOperate", MAX_CONFIG_OPERATE_THREAD));
                    config.addChangeListener((changeEvent) -> {
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
        if (null == instance) {
            synchronized (ApolloConfiguration.class) {
                if (null == instance) {
                    instance = new ApolloConfiguration();
                }
            }
        }
        return instance;
    }

    @Override
    public String getConfig(String dataId, String defaultValue, long timeoutMills) {
        String value;
        if ((value = getConfigFromSysPro(dataId)) != null) {
            return value;
        }
        ConfigFuture configFuture = new ConfigFuture(dataId, defaultValue, ConfigFuture.ConfigOperation.GET,
            timeoutMills);
        configOperateExecutor.submit(new Runnable() {
            @Override
            public void run() {
                String result = config.getProperty(dataId, defaultValue);
                configFuture.setResult(result);
            }
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
        if (null == dataId || null == listener) {
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
        if (!properties.containsKey(APP_ID)) {
            System.setProperty(APP_ID, FILE_CONFIG.getConfig(getApolloAppIdFileKey()));
        }
        if (!properties.containsKey(APOLLO_META)) {
            System.setProperty(APOLLO_META, FILE_CONFIG.getConfig(getApolloMetaFileKey()));
        }
    }

    @Override
    public String getTypeName() {
        return REGISTRY_TYPE;
    }

    private static String getApolloMetaFileKey() {
        return FILE_ROOT_CONFIG + FILE_CONFIG_SPLIT_CHAR + REGISTRY_TYPE + FILE_CONFIG_SPLIT_CHAR + APOLLO_META;
    }

    private static String getApolloAppIdFileKey() {
        return FILE_ROOT_CONFIG + FILE_CONFIG_SPLIT_CHAR + REGISTRY_TYPE + FILE_CONFIG_SPLIT_CHAR + APP_ID;
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
