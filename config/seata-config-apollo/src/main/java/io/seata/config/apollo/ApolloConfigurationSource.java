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
import io.seata.common.thread.NamedThreadFactory;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.StringUtils;
import io.seata.config.ConfigFuture;
import io.seata.config.ConfigurationChangeEvent;
import io.seata.config.ConfigurationChangeType;
import io.seata.config.ConfigurationFactory;
import io.seata.config.listener.ConfigListenerManager;
import io.seata.config.listener.ConfigurationChangeListener;
import io.seata.config.source.RemoteConfigurationSource;

import static io.seata.common.ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR;
import static io.seata.common.ConfigurationKeys.FILE_ROOT_CONFIG;

/**
 * The type Apollo configuration source.
 *
 * @author kl @kailing.pub
 */
public class ApolloConfigurationSource implements RemoteConfigurationSource, ConfigListenerManager {

    private static final String REGISTRY_TYPE = "apollo";
    private static final String APP_ID = "appId";
    private static final String APOLLO_META = "apolloMeta";
    private static final String APOLLO_SECRET = "apolloAccessKeySecret";
    private static final String APOLLO_CLUSTER = "cluster";
    private static final String APOLLO_CONFIG_SERVICE = "apolloConfigService";
    private static final String PROP_APP_ID = "app.id";
    private static final String PROP_APOLLO_META = "apollo.meta";
    private static final String PROP_APOLLO_CONFIG_SERVICE = "apollo.configService";
    private static final String PROP_APOLLO_SECRET = "apollo.accesskey.secret";
    private static final String PROP_APOLLO_CLUSTER = "apollo.cluster";
    private static final String NAMESPACE = "namespace";
    private static final String DEFAULT_NAMESPACE = "application";
    private static volatile Config config;
    private ExecutorService configOperateExecutor;
    private static final int CORE_CONFIG_OPERATE_THREAD = 1;
    private static final ConcurrentMap<String, Set<ConfigurationChangeListener>> LISTENER_SERVICE_MAP = new ConcurrentHashMap<>();
    private static final int MAX_CONFIG_OPERATE_THREAD = 2;
    private static volatile ApolloConfigurationSource instance;

    @SuppressWarnings("lgtm[java/unsafe-double-checked-locking-init-order]")
    private ApolloConfigurationSource() {
        readyApolloConfig();
        if (config == null) {
            synchronized (ApolloConfigurationSource.class) {
                if (config == null) {
                    config = ConfigService.getConfig(ConfigurationFactory.getInstance().getString(getApolloNamespaceKey(), DEFAULT_NAMESPACE));
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
                                    change.getOldValue(), change.getNewValue(), getChangeType(change.getChangeType()), this);
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
    public static ApolloConfigurationSource getInstance() {
        if (instance == null) {
            synchronized (ApolloConfigurationSource.class) {
                if (instance == null) {
                    instance = new ApolloConfigurationSource();
                }
            }
        }
        return instance;
    }

    @Override
    public String getLatestConfig(String dataId, long timeoutMills) {
        ConfigFuture configFuture = new ConfigFuture(dataId, null, ConfigFuture.ConfigOperation.GET,
                timeoutMills);
        configOperateExecutor.submit(() -> {
            String result = config.getProperty(dataId, null);
            configFuture.setResult(result);
        });
        return (String)configFuture.get();
    }

    @Override
    public void addConfigListener(String dataId, ConfigurationChangeListener listener) {
        if (StringUtils.isBlank(dataId) || listener == null) {
            return;
        }
        LISTENER_SERVICE_MAP.computeIfAbsent(dataId, key -> ConcurrentHashMap.newKeySet())
                .add(listener);
    }

    @Override
    public void removeConfigListener(String dataId, ConfigurationChangeListener listener) {
        if (StringUtils.isBlank(dataId) || listener == null) {
            return;
        }
        Set<ConfigurationChangeListener> configListeners = getConfigListeners(dataId);
        if (CollectionUtils.isNotEmpty(configListeners)) {
            configListeners.remove(listener);
        }
    }

    @Override
    public Set<String> getListenedConfigDataIds() {
        return LISTENER_SERVICE_MAP.keySet();
    }

    @Override
    public Set<ConfigurationChangeListener> getConfigListeners(String dataId) {
        return LISTENER_SERVICE_MAP.get(dataId);
    }

    private void readyApolloConfig() {
        Properties properties = System.getProperties();
        if (!properties.containsKey(PROP_APP_ID)) {
            String appId = ConfigurationFactory.getInstance().getString(getApolloAppIdFileKey());
            if (StringUtils.isNotBlank(appId)) {
                System.setProperty(PROP_APP_ID, appId);
            }
        }
        if (!properties.containsKey(PROP_APOLLO_META)) {
            String apolloMeta = ConfigurationFactory.getInstance().getString(getApolloMetaFileKey());
            if (StringUtils.isNotBlank(apolloMeta)) {
                System.setProperty(PROP_APOLLO_META, apolloMeta);
            }
        }
        if (!properties.containsKey(PROP_APOLLO_SECRET)) {
            String apolloAccesskeySecret = ConfigurationFactory.getInstance().getString(getApolloSecretFileKey());
            if (StringUtils.isNotBlank(apolloAccesskeySecret)) {
                System.setProperty(PROP_APOLLO_SECRET, apolloAccesskeySecret);
            }
        }
        if (!properties.containsKey(APOLLO_CLUSTER)) {
            String apolloCluster = ConfigurationFactory.getInstance().getString(getApolloCluster());
            if (StringUtils.isNotBlank(apolloCluster)) {
                System.setProperty(PROP_APOLLO_CLUSTER, apolloCluster);
            }
        }
        if (!properties.containsKey(APOLLO_CONFIG_SERVICE)) {
            String apolloConfigService = ConfigurationFactory.getInstance().getString(getApolloConfigService());
            if (StringUtils.isNotBlank(apolloConfigService)) {
                System.setProperty(PROP_APOLLO_CONFIG_SERVICE, apolloConfigService);
            } else {
                if (StringUtils.isBlank(System.getProperty(PROP_APOLLO_META))) {
                    throw new RuntimeException("Apollo configuration initialized failed,please check the value of apolloMeta and apolloConfigService");
                }
            }
        }
    }

    @Override
    public String getTypeName() {
        return REGISTRY_TYPE;
    }

    public static String getApolloMetaFileKey() {
        return String.join(FILE_CONFIG_SPLIT_CHAR, FILE_ROOT_CONFIG, REGISTRY_TYPE, APOLLO_META);
    }

    public static String getApolloSecretFileKey() {
        return String.join(FILE_CONFIG_SPLIT_CHAR, FILE_ROOT_CONFIG, REGISTRY_TYPE, APOLLO_SECRET);
    }

    public static String getApolloAppIdFileKey() {
        return String.join(FILE_CONFIG_SPLIT_CHAR, FILE_ROOT_CONFIG, REGISTRY_TYPE, APP_ID);
    }

    public static String getApolloNamespaceKey() {
        return String.join(FILE_CONFIG_SPLIT_CHAR, FILE_ROOT_CONFIG, REGISTRY_TYPE, NAMESPACE);
    }

    public static String getApolloCluster() {
        return String.join(FILE_CONFIG_SPLIT_CHAR, FILE_ROOT_CONFIG, REGISTRY_TYPE, APOLLO_CLUSTER);
    }

    public static String getApolloConfigService() {
        return String.join(FILE_CONFIG_SPLIT_CHAR, FILE_ROOT_CONFIG, REGISTRY_TYPE, APOLLO_CONFIG_SERVICE);
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
