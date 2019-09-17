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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.seata.common.thread.NamedThreadFactory;
import io.seata.config.ConfigFuture.ConfigOperation;
import org.apache.commons.lang.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type FileConfiguration.
 *
 * @author jimin.jm @alibaba-inc.com
 * @date 2018 /9/10
 */
public class FileConfiguration extends AbstractConfiguration<ConfigChangeListener> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileConfiguration.class);

    private final Config fileConfig;

    private ExecutorService configOperateExecutor;

    private ExecutorService configChangeExecutor;

    private static final int CORE_CONFIG_OPERATE_THREAD = 1;

    private static final int CORE_CONFIG_CHANGE_THREAD = 1;

    private static final int MAX_CONFIG_OPERATE_THREAD = 2;

    private static final long LISTENER_CONFIG_INTERNAL = 1 * 1000;

    private static final String REGISTRY_TYPE = "file";
    
    private static final String SYS_FILE_RESOURCE_PREFIX = "file:";

    private final ConcurrentMap<String, List<ConfigChangeListener>> configListenersMap = new ConcurrentHashMap<>(8);

    private final ConcurrentMap<String, String> listenedConfigMap = new ConcurrentHashMap<>(8);

    /**
     * Instantiates a new File configuration.
     */
    public FileConfiguration() {
        this(null);
    }

    /**
     * Instantiates a new File configuration.
     *
     * @param name the name
     */
    public FileConfiguration(String name) {
        if (null == name) {
            fileConfig = ConfigFactory.load();
        }
        else if (name.startsWith(SYS_FILE_RESOURCE_PREFIX)) {
            Config appConfig = ConfigFactory.parseFileAnySyntax(new File(name.substring(SYS_FILE_RESOURCE_PREFIX.length())));
            fileConfig = ConfigFactory.load(appConfig);
        } else {
            fileConfig = ConfigFactory.load(name);
        }
        configOperateExecutor = new ThreadPoolExecutor(CORE_CONFIG_OPERATE_THREAD, MAX_CONFIG_OPERATE_THREAD,
            Integer.MAX_VALUE, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(),
            new NamedThreadFactory("configOperate", MAX_CONFIG_OPERATE_THREAD));
        configChangeExecutor = new ThreadPoolExecutor(CORE_CONFIG_CHANGE_THREAD, CORE_CONFIG_CHANGE_THREAD,
            Integer.MAX_VALUE, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(),
            new NamedThreadFactory("configChange", CORE_CONFIG_CHANGE_THREAD));
        configChangeExecutor.submit(new ConfigChangeRunnable());
    }

    @Override
    public String getConfig(String dataId, String defaultValue, long timeoutMills) {
        String value;
        if ((value = getConfigFromSysPro(dataId)) != null) {
            return value;
        }
        ConfigFuture configFuture = new ConfigFuture(dataId, defaultValue, ConfigOperation.GET, timeoutMills);
        configOperateExecutor.submit(new ConfigOperateRunnable(configFuture));
        return (String)configFuture.get();
    }

    @Override
    public boolean putConfig(String dataId, String content, long timeoutMills) {
        ConfigFuture configFuture = new ConfigFuture(dataId, content, ConfigOperation.PUT, timeoutMills);
        configOperateExecutor.submit(new ConfigOperateRunnable(configFuture));
        return (Boolean)configFuture.get();
    }

    @Override
    public boolean putConfigIfAbsent(String dataId, String content, long timeoutMills) {
        ConfigFuture configFuture = new ConfigFuture(dataId, content, ConfigOperation.PUTIFABSENT, timeoutMills);
        configOperateExecutor.submit(new ConfigOperateRunnable(configFuture));
        return (Boolean)configFuture.get();
    }

    @Override
    public boolean removeConfig(String dataId, long timeoutMills) {
        ConfigFuture configFuture = new ConfigFuture(dataId, null, ConfigOperation.REMOVE, timeoutMills);
        configOperateExecutor.submit(new ConfigOperateRunnable(configFuture));
        return (Boolean)configFuture.get();
    }

    @Override
    public void addConfigListener(String dataId, ConfigChangeListener listener) {
        configListenersMap.putIfAbsent(dataId, new ArrayList<ConfigChangeListener>());
        configListenersMap.get(dataId).add(listener);
        listenedConfigMap.putIfAbsent(dataId, getConfig(dataId));
        if (null != listener.getExecutor()) {
            ConfigChangeRunnable configChangeTask = new ConfigChangeRunnable(dataId, listener);
            listener.getExecutor().submit(configChangeTask);
        }
    }

    @Override
    public void removeConfigListener(String dataId, ConfigChangeListener listener) {
        List<ConfigChangeListener> configChangeListeners = getConfigListeners(dataId);
        if (configChangeListeners == null) {
            return;
        }
        List<ConfigChangeListener> newChangeListenerList = new ArrayList<>();
        for (ConfigChangeListener changeListener : configChangeListeners) {
            if (!changeListener.equals(listener)) {
                newChangeListenerList.add(changeListener);
            }
        }
        configListenersMap.put(dataId, newChangeListenerList);
        if (newChangeListenerList.isEmpty()) {
            listenedConfigMap.remove(dataId);
        }
        if (null != listener.getExecutor()) {
            listener.getExecutor().shutdownNow();
        }

    }

    @Override
    public List<ConfigChangeListener> getConfigListeners(String dataId) {
        return configListenersMap.get(dataId);
    }

    @Override
    public String getTypeName() {
        return REGISTRY_TYPE;
    }

    /**
     * The type Config operate runnable.
     */
    class ConfigOperateRunnable implements Runnable {

        private ConfigFuture configFuture;

        /**
         * Instantiates a new Config operate runnable.
         *
         * @param configFuture the config future
         */
        public ConfigOperateRunnable(ConfigFuture configFuture) {
            this.configFuture = configFuture;
        }

        @Override
        public void run() {
            if (null != configFuture) {
                if (configFuture.isTimeout()) {
                    setFailResult(configFuture);
                    return;
                }
                try {
                    if (configFuture.getOperation() == ConfigOperation.GET) {
                        String result = fileConfig.getString(configFuture.getDataId());
                        configFuture.setResult(result);
                    } else if (configFuture.getOperation() == ConfigOperation.PUT) {
                        //todo
                        configFuture.setResult(Boolean.TRUE);
                    } else if (configFuture.getOperation() == ConfigOperation.PUTIFABSENT) {
                        //todo
                        configFuture.setResult(Boolean.TRUE);
                    } else if (configFuture.getOperation() == ConfigOperation.REMOVE) {
                        //todo
                        configFuture.setResult(Boolean.TRUE);
                    }
                } catch (Exception e) {
                    setFailResult(configFuture);
                    LOGGER.warn("Could not found property {}, try to use default value instead.",
                        configFuture.getDataId());
                }
            }
        }

        private void setFailResult(ConfigFuture configFuture) {
            if (configFuture.getOperation() == ConfigOperation.GET) {
                String result = configFuture.getContent();
                configFuture.setResult(result);
            } else {
                configFuture.setResult(Boolean.FALSE);
            }
        }

    }

    /**
     * The type Config change runnable.
     */
    class ConfigChangeRunnable implements Runnable {

        private String dataId;
        private ConfigChangeListener listener;

        /**
         * Instantiates a new Config change runnable.
         */
        public ConfigChangeRunnable() {
        }

        /**
         * Instantiates a new Config change runnable.
         *
         * @param dataId   the data id
         * @param listener the listener
         */
        public ConfigChangeRunnable(String dataId, ConfigChangeListener listener) {

            if (null == listener.getExecutor()) {
                throw new IllegalArgumentException("getExecutor is null.");
            }
            this.dataId = dataId;
            this.listener = listener;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    Map<String, List<ConfigChangeListener>> configListenerMap;
                    if (null != dataId && null != listener) {
                        configListenerMap = new ConcurrentHashMap<>(8);
                        configListenerMap.put(dataId, new ArrayList<>());
                        configListenerMap.get(dataId).add(listener);
                    } else {
                        configListenerMap = configListenersMap;
                    }
                    for (Map.Entry<String, List<ConfigChangeListener>> entry : configListenerMap.entrySet()) {
                        String configId = entry.getKey();
                        String currentConfig = getConfig(configId);
                        if (ObjectUtils.notEqual(currentConfig, listenedConfigMap.get(configId))) {
                            listenedConfigMap.put(configId, currentConfig);
                            notifyAllListener(configId, configListenerMap.get(configId));

                        }
                    }
                    Thread.sleep(LISTENER_CONFIG_INTERNAL);
                } catch (Exception exx) {
                    LOGGER.error(exx.getMessage());
                }

            }
        }

        private void notifyAllListener(String dataId, List<ConfigChangeListener> configChangeListeners) {
            List<ConfigChangeListener> needNotifyListeners = new ArrayList<>();
            if (null != dataId && null != listener) {
                needNotifyListeners.addAll(configChangeListeners);
            } else {
                for (ConfigChangeListener configChangeListener : configChangeListeners) {
                    if (null == configChangeListener.getExecutor()) {
                        needNotifyListeners.add(configChangeListener);
                    }
                }
            }
            for (ConfigChangeListener configChangeListener : needNotifyListeners) {
                configChangeListener.receiveConfigInfo(listenedConfigMap.get(dataId));
            }
        }

    }

}
