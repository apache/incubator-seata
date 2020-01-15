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
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.netty.util.internal.ConcurrentSet;
import io.seata.common.thread.NamedThreadFactory;
import io.seata.config.ConfigFuture.ConfigOperation;
import org.apache.commons.lang.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type FileConfiguration.
 *
 * @author slievrly
 */
public class FileConfiguration extends AbstractConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileConfiguration.class);

    private Config fileConfig;

    private ExecutorService configOperateExecutor;

    private static final int CORE_CONFIG_OPERATE_THREAD = 1;

    private static final int MAX_CONFIG_OPERATE_THREAD = 2;

    private static final long LISTENER_CONFIG_INTERNAL = 1 * 1000;

    private static final String REGISTRY_TYPE = "file";

    private static final String SYS_FILE_RESOURCE_PREFIX = "file:";

    private final ConcurrentMap<String, Set<ConfigurationChangeListener>> configListenersMap = new ConcurrentHashMap<>(
        8);

    private final Map<String, String> listenedConfigMap = new HashMap<>(8);

    private final String targetFilePath;

    private volatile long targetFileLastModified;

    private final String name;

    private final boolean allowDynamicRefresh;

    /**
     * Note that:this constructor is only used to create proxy with CGLIB
     * see io.seata.spring.boot.autoconfigure.provider.SpringBootConfigurationProvider#provide
     */
    public FileConfiguration() {
        this.name = null;
        this.targetFilePath = null;
        this.allowDynamicRefresh = false;
    }

    /**
     * Instantiates a new File configuration.
     *
     * @param name the name
     */
    public FileConfiguration(String name) {
        this(name, true);
    }

    /**
     * Instantiates a new File configuration.
     *
     * @param name                the name
     * @param allowDynamicRefresh the allow dynamic refresh
     */
    public FileConfiguration(String name, boolean allowDynamicRefresh) {
        LOGGER.info("The file name of the operation is {}", name);
        if (null == name) {
            throw new IllegalArgumentException("name can't be null");
        } else if (name.startsWith(SYS_FILE_RESOURCE_PREFIX)) {
            File targetFile = new File(name.substring(SYS_FILE_RESOURCE_PREFIX.length()));
            if (targetFile.exists()) {
                targetFilePath = targetFile.getPath();
                Config appConfig = ConfigFactory.parseFileAnySyntax(targetFile);
                fileConfig = ConfigFactory.load(appConfig);
            } else {
                targetFilePath = null;
            }
        } else {
            URL resource = this.getClass().getClassLoader().getResource(name);
            if (null != resource) {
                targetFilePath = resource.getPath();
                fileConfig = ConfigFactory.load(name);

            } else {
                targetFilePath = null;
            }
        }
        /**
         * For seata-server side the conf file should always exists.
         * For application(or client) side,conf file may not exists when using seata-spring-boot-starter
         */
        if (null == targetFilePath) {
            fileConfig = ConfigFactory.load();
            this.allowDynamicRefresh = false;
        } else {
            targetFileLastModified = new File(targetFilePath).lastModified();
            this.allowDynamicRefresh = allowDynamicRefresh;
        }

        this.name = name;
        configOperateExecutor = new ThreadPoolExecutor(CORE_CONFIG_OPERATE_THREAD, MAX_CONFIG_OPERATE_THREAD,
            Integer.MAX_VALUE, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
            new NamedThreadFactory("configOperate", MAX_CONFIG_OPERATE_THREAD));
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
    public void addConfigListener(String dataId, ConfigurationChangeListener listener) {
        if (null == dataId || null == listener) {
            return;
        }
        configListenersMap.putIfAbsent(dataId, new ConcurrentSet<>());
        configListenersMap.get(dataId).add(listener);
        listenedConfigMap.put(dataId, getConfig(dataId));
        FileListener fileListener = new FileListener(dataId, listener);
        fileListener.onProcessEvent(new ConfigurationChangeEvent());
    }

    @Override
    public void removeConfigListener(String dataId, ConfigurationChangeListener listener) {
        Set<ConfigurationChangeListener> configChangeListeners = getConfigListeners(dataId);
        if (dataId == null || configChangeListeners == null) {
            return;
        }
        if (configListenersMap.containsKey(dataId)) {
            configListenersMap.get(dataId).remove(listener);
            if (configListenersMap.get(dataId).isEmpty()) {
                configListenersMap.remove(dataId);
                listenedConfigMap.remove(dataId);
            }
        }
        listener.onShutDown();
    }

    @Override
    public Set<ConfigurationChangeListener> getConfigListeners(String dataId) {
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
                    if (allowDynamicRefresh) {
                        long tempLastModified = new File(targetFilePath).lastModified();
                        if (tempLastModified > targetFileLastModified) {
                            Config tempConfig;
                            if (name.startsWith(SYS_FILE_RESOURCE_PREFIX)) {
                                Config appConfig = ConfigFactory.parseFileAnySyntax(new File(targetFilePath));
                                tempConfig = ConfigFactory.load(appConfig);
                            } else {
                                tempConfig = ConfigFactory.load(name);
                            }
                            if (null != tempConfig) {
                                fileConfig = tempConfig;
                                targetFileLastModified = tempLastModified;
                            }
                        }
                    }
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
     * The type FileListener.
     */
    class FileListener implements ConfigurationChangeListener {

        private final String dataId;
        private final ConfigurationChangeListener listener;
        private final ExecutorService executor = new ThreadPoolExecutor(CORE_LISTENER_THREAD, MAX_LISTENER_THREAD, 0L,
            TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
            new NamedThreadFactory("fileListener", MAX_LISTENER_THREAD));

        /**
         * Instantiates a new FileListener.
         *
         * @param dataId   the data id
         * @param listener the listener
         */
        public FileListener(String dataId, ConfigurationChangeListener listener) {
            this.dataId = dataId;
            this.listener = listener;
        }

        @Override
        public void onChangeEvent(ConfigurationChangeEvent event) {
            while (true) {
                try {
                    String currentConfig = getConfig(dataId);
                    String oldConfig = listenedConfigMap.get(dataId);
                    if (ObjectUtils.notEqual(currentConfig, oldConfig)) {
                        listenedConfigMap.put(dataId, currentConfig);
                        event.setDataId(dataId).setNewValue(currentConfig).setOldValue(oldConfig);
                        listener.onChangeEvent(event);
                    }
                    Thread.sleep(LISTENER_CONFIG_INTERNAL);
                } catch (Exception exx) {
                    LOGGER.error("fileListener execute error:{}", exx.getMessage(), exx);
                }
            }
        }

        @Override
        public ExecutorService getExecutorService() {
            return executor;
        }
    }

}
