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
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.alibaba.fescar.common.exception.NotSupportYetException;
import com.alibaba.fescar.common.thread.NamedThreadFactory;

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigChangeListener;
import com.ctrip.framework.apollo.ConfigService;
import com.ctrip.framework.apollo.model.ConfigChangeEvent;
import com.google.common.collect.Lists;

import static com.alibaba.fescar.config.ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR;
import static com.alibaba.fescar.config.ConfigurationKeys.FILE_ROOT_CONFIG;

/**
 * The type Apollo configuration.
 *
 * @author: kl @kailing.pub
 * @date: 2019 /2/27
 */
public class ApolloConfiguration extends AbstractConfiguration<ConfigChangeListener> {

    private static final String REGISTRY_TYPE = "apollo";
    private static final String APP_ID = "app.id";
    private static final String APOLLO_META = "apollo.meta";
    private static final Configuration FILE_CONFIG = ConfigurationFactory.FILE_INSTANCE;
    private static volatile Config config;
    private ExecutorService configOperateExecutor;
    private static final int CORE_CONFIG_OPERATE_THREAD = 1;
    private static final ConcurrentMap<String, ConfigChangeListener> LISTENER_SERVICE_MAP = new ConcurrentHashMap<>();
    private static final int MAX_CONFIG_OPERATE_THREAD = 2;
    private static volatile ApolloConfiguration instance;

    private ApolloConfiguration() {
        readyApolloConfig();
        if (null == config) {
            synchronized (ApolloConfiguration.class) {
                if (null == config) {
                    config = ConfigService.getAppConfig();
                    configOperateExecutor = new ThreadPoolExecutor(CORE_CONFIG_OPERATE_THREAD,
                        MAX_CONFIG_OPERATE_THREAD,
                        Integer.MAX_VALUE, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(),
                        new NamedThreadFactory("apolloConfigOperate", MAX_CONFIG_OPERATE_THREAD));
                    config.addChangeListener(new ConfigChangeListener() {
                        @Override
                        public void onChange(ConfigChangeEvent changeEvent) {
                            for (Map.Entry<String, ConfigChangeListener> entry : LISTENER_SERVICE_MAP.entrySet()) {
                                if (changeEvent.isChanged(entry.getKey())) {
                                    entry.getValue().onChange(changeEvent);
                                }
                            }
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
        ConfigFuture configFuture = new ConfigFuture(dataId, defaultValue, ConfigFuture.ConfigOperation.GET,
            timeoutMills);
        configOperateExecutor.submit(new Runnable() {
            @Override
            public void run() {
                String result = config.getProperty(dataId, defaultValue);
                configFuture.setResult(result);
            }
        });
        return (String)configFuture.get(timeoutMills, TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean putConfig(String dataId, String content, long timeoutMills) {
        throw new NotSupportYetException("not support putConfig");
    }

    @Override
    public boolean putConfigIfAbsent(String dataId, String content, long timeoutMills) {
        throw new NotSupportYetException("not support putConfigIfAbsent");
    }

    @Override
    public boolean removeConfig(String dataId, long timeoutMills) {
        throw new NotSupportYetException("not support removeConfig");
    }

    @Override
    public void addConfigListener(String dataId, ConfigChangeListener listener) {
        LISTENER_SERVICE_MAP.put(dataId, listener);
    }

    @Override
    public void removeConfigListener(String dataId, ConfigChangeListener listener) {
        LISTENER_SERVICE_MAP.remove(dataId, listener);
    }

    @Override
    public List<ConfigChangeListener> getConfigListeners(String dataId) {
        return Lists.newArrayList(LISTENER_SERVICE_MAP.values());
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
        return FILE_ROOT_CONFIG + FILE_CONFIG_SPLIT_CHAR + REGISTRY_TYPE + FILE_CONFIG_SPLIT_CHAR
            + APOLLO_META;
    }

    private static String getApolloAppIdFileKey() {
        return FILE_ROOT_CONFIG + FILE_CONFIG_SPLIT_CHAR + REGISTRY_TYPE + FILE_CONFIG_SPLIT_CHAR
            + APP_ID;
    }
}