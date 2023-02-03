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
package io.seata.config.source.impl;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import io.seata.common.ConfigurationKeys;
import io.seata.common.thread.NamedThreadFactory;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.StringUtils;
import io.seata.config.ConfigFuture;
import io.seata.config.ConfigFuture.ConfigOperation;
import io.seata.config.ConfigurationFactory;
import io.seata.config.changelistener.ConfigChangeListenerUtils;
import io.seata.config.changelistener.ConfigurationChangeEvent;
import io.seata.config.changelistener.ConfigurationChangeListener;
import io.seata.config.changelistener.ConfigurationChangeListenerManager;
import io.seata.config.file.FileConfig;
import io.seata.config.file.FileConfigFactory;
import io.seata.config.source.ConfigSource;
import io.seata.config.source.LocalConfigSource;
import io.seata.config.source.UpdatableConfigSource;
import org.apache.commons.lang.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type File config source.
 *
 * @author slievrly
 */
public class FileConfigSource implements LocalConfigSource
        , UpdatableConfigSource, ConfigurationChangeListenerManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileConfigSource.class);

    private FileConfig fileConfig;

    private ExecutorService configOperateExecutor;

    private static final int CORE_CONFIG_OPERATE_THREAD = 1;

    private static final int MAX_CONFIG_OPERATE_THREAD = 2;

    private static final long LISTENER_CONFIG_INTERVAL = 1 * 1000;

    private static final String CONFIG_TYPE = "file";

    public static final String SYS_FILE_RESOURCE_PREFIX = "file:";

    private final Map<String, Set<ConfigurationChangeListener>> configListenersMap = new ConcurrentHashMap<>(8);

    private final Map<String, String> listenedConfigMap = new HashMap<>(8);

    private final String targetFilePath;

    private volatile long targetFileLastModified;

    private final String name;

    private final FileListener fileListener = new FileListener(this);

    private final boolean allowDynamicRefresh;

    /**
     * Note that:this constructor is only used to create proxy with CGLIB
     * see io.seata.spring.boot.autoconfigure.provider.SpringBootConfigurationProvider#provide
     */
    public FileConfigSource() {
        this.name = null;
        this.targetFilePath = null;
        this.allowDynamicRefresh = false;
    }

    /**
     * Instantiates a new File configuration.
     *
     * @param name the name
     */
    public FileConfigSource(String name) {
        this(name, true);
    }

    /**
     * Instantiates a new File configuration.
     * For seata-server side the conf file should always exists.
     * For application(or client) side,conf file may not exists when using seata-spring-boot-starter
     *
     * @param name                the name
     * @param allowDynamicRefresh the allow dynamic refresh
     */
    public FileConfigSource(String name, boolean allowDynamicRefresh) {
        File file = getConfigFile(name);
        if (file == null) {
            targetFilePath = null;
            fileConfig = FileConfigFactory.load();
            this.allowDynamicRefresh = false;
        } else {
            targetFilePath = file.getPath();
            fileConfig = FileConfigFactory.load(file, name);
            targetFileLastModified = new File(targetFilePath).lastModified();
            this.allowDynamicRefresh = allowDynamicRefresh;
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("The file name of the operation is {}", name);
            }
        }
        this.name = name;
        configOperateExecutor = new ThreadPoolExecutor(CORE_CONFIG_OPERATE_THREAD, MAX_CONFIG_OPERATE_THREAD,
                Integer.MAX_VALUE, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
                new NamedThreadFactory("configOperate", MAX_CONFIG_OPERATE_THREAD));
    }

    private File getConfigFile(String name) {
        try {
            if (name == null) {
                throw new IllegalArgumentException("name can't be null");
            }

            boolean filePathCustom = name.startsWith(SYS_FILE_RESOURCE_PREFIX);
            String filePath = filePathCustom ? name.substring(SYS_FILE_RESOURCE_PREFIX.length()) : name;
            String decodedPath = URLDecoder.decode(filePath, StandardCharsets.UTF_8.name());

            File targetFile = getFileFromFileSystem(decodedPath);
            if (targetFile != null) {
                return targetFile;
            }

            if (!filePathCustom) {
                targetFile = getFileFromClasspath(name);
                if (targetFile != null) {
                    return targetFile;
                }
            }
        } catch (UnsupportedEncodingException e) {
            LOGGER.error("decode name error: {}", e.getMessage(), e);
        }

        return null;
    }

    private File getFileFromFileSystem(String decodedPath) {
        // run with jar file and not package third lib into jar file, this.getClass().getClassLoader() will be null
        URL resourceUrl = this.getClass().getClassLoader().getResource("");
        String[] tryPaths;
        if (resourceUrl != null) {
            tryPaths = new String[]{
                // first: project dir
                resourceUrl.getPath() + decodedPath,
                // second: system path
                decodedPath
            };
        } else {
            tryPaths = new String[]{
                decodedPath
            };
        }


        for (String tryPath : tryPaths) {
            File targetFile = new File(tryPath);
            if (targetFile.exists()) {
                return targetFile;
            }

            // try to append config suffix
            for (String s : FileConfigFactory.getSuffixSet()) {
                targetFile = new File(tryPath + ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR + s);
                if (targetFile.exists()) {
                    return targetFile;
                }
            }
        }

        return null;
    }

    private File getFileFromClasspath(String name) throws UnsupportedEncodingException {
        URL resource = this.getClass().getClassLoader().getResource(name);
        if (resource == null) {
            for (String s : FileConfigFactory.getSuffixSet()) {
                resource = this.getClass().getClassLoader().getResource(name + ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR + s);
                if (resource != null) {
                    String path = resource.getPath();
                    path = URLDecoder.decode(path, StandardCharsets.UTF_8.name());
                    return new File(path);
                }
            }
        } else {
            String path = resource.getPath();
            path = URLDecoder.decode(path, StandardCharsets.UTF_8.name());
            return new File(path);
        }

        return null;
    }

    @Override
    public String getLatestConfig(String dataId, long timeoutMills) {
        ConfigFuture configFuture = new ConfigFuture(dataId, null, ConfigOperation.GET, timeoutMills);
        configOperateExecutor.submit(new ConfigOperateRunnable(configFuture));
        Object getValue = configFuture.get();
        return getValue == null ? null : String.valueOf(getValue);
    }

    @Override
    public boolean putConfig(String dataId, String content, long timeoutMills) {
        ConfigFuture configFuture = new ConfigFuture(dataId, content, ConfigOperation.PUT, timeoutMills);
        configOperateExecutor.submit(new ConfigOperateRunnable(configFuture));
        return (Boolean) configFuture.get();
    }

    @Override
    public boolean putConfigIfAbsent(String dataId, String content, long timeoutMills) {
        ConfigFuture configFuture = new ConfigFuture(dataId, content, ConfigOperation.PUTIFABSENT, timeoutMills);
        configOperateExecutor.submit(new ConfigOperateRunnable(configFuture));
        return (Boolean) configFuture.get();
    }

    @Override
    public boolean removeConfig(String dataId, long timeoutMills) {
        ConfigFuture configFuture = new ConfigFuture(dataId, null, ConfigOperation.REMOVE, timeoutMills);
        configOperateExecutor.submit(new ConfigOperateRunnable(configFuture));
        return (Boolean) configFuture.get();
    }

    @Override
    public void addConfigListener(String dataId, ConfigurationChangeListener listener) {
        if (StringUtils.isBlank(dataId) || listener == null) {
            return;
        }
        configListenersMap.computeIfAbsent(dataId, key -> ConcurrentHashMap.newKeySet())
                .add(listener);
        listenedConfigMap.put(dataId, ConfigurationFactory.getInstance().getString(dataId));

        // Start config change listener for the dataId.
        fileListener.addListener(dataId, listener);
    }

    @Override
    public void removeConfigListener(String dataId, ConfigurationChangeListener listener) {
        if (StringUtils.isBlank(dataId) || listener == null) {
            return;
        }
        Set<ConfigurationChangeListener> configListeners = getConfigListeners(dataId);
        if (CollectionUtils.isNotEmpty(configListeners)) {
            configListeners.remove(listener);
            if (configListeners.isEmpty()) {
                configListenersMap.remove(dataId);
                listenedConfigMap.remove(dataId);
            }
        }
        listener.onShutDown();
    }

    @Override
    public Set<String> getListenedConfigDataIds() {
        return configListenersMap.keySet();
    }

    @Override
    public Set<ConfigurationChangeListener> getConfigListeners(String dataId) {
        return configListenersMap.get(dataId);
    }

    @Nonnull
    @Override
    public String getName() {
        return CONFIG_TYPE + ":" + name;
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
            if (configFuture != null) {
                if (configFuture.isTimeout()) {
                    setFailResult(configFuture);
                    return;
                }
                try {
                    if (allowDynamicRefresh) {
                        long tempLastModified = new File(targetFilePath).lastModified();
                        if (tempLastModified > targetFileLastModified) {
                            fileConfig = FileConfigFactory.load(new File(targetFilePath), name);
                            targetFileLastModified = tempLastModified;
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
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Could not found property '{}', try to use default value instead. exception: {}",
                                configFuture.getDataId(), e.getMessage());
                    }
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

        private final ConfigSource source;

        private final Map<String, Set<ConfigurationChangeListener>> dataIdMap = new HashMap<>();

        private final ExecutorService executor = new ThreadPoolExecutor(CORE_LISTENER_THREAD, MAX_LISTENER_THREAD, 0L,
                TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
                new NamedThreadFactory("fileListener", MAX_LISTENER_THREAD));

        /**
         * Instantiates a new FileListener.
         */
        FileListener(ConfigSource source) {
            this.source = source;
        }

        public synchronized void addListener(String dataId, ConfigurationChangeListener listener) {
            // only the first time add listener will trigger on process event
            if (dataIdMap.isEmpty()) {
                fileListener.onProcessEvent(new ConfigurationChangeEvent(source));
            }

            dataIdMap.computeIfAbsent(dataId, value -> new HashSet<>()).add(listener);
        }

        @Override
        public void onChangeEvent(ConfigurationChangeEvent event) {
            boolean enabled = "true".equalsIgnoreCase(System.getProperty("file.listener.enabled", "true"));
            while (enabled) {
                for (String dataId : dataIdMap.keySet()) {
                    try {
                        String currentConfig = source.getLatestConfig(dataId);
                        if (StringUtils.isNotBlank(currentConfig)) {
                            String oldConfig = listenedConfigMap.get(dataId);
                            if (ObjectUtils.notEqual(currentConfig, oldConfig)) {
                                listenedConfigMap.put(dataId, currentConfig);
                                event.setDataId(dataId)
                                        .setOldValue(oldConfig)
                                        .setNewValue(currentConfig)
                                        .setChangeType(ConfigChangeListenerUtils.getChangeType(oldConfig, currentConfig));

                                for (ConfigurationChangeListener listener : dataIdMap.get(dataId)) {
                                    listener.onChangeEvent(event);
                                }
                            }
                        }
                    } catch (Exception exx) {
                        LOGGER.error("fileListener execute error, dataId :{}", dataId, exx);
                    }
                }
                try {
                    Thread.sleep(LISTENER_CONFIG_INTERVAL);
                } catch (InterruptedException e) {
                    LOGGER.error("fileListener thread sleep error:{}", e.getMessage());
                }
                enabled = "true".equalsIgnoreCase(System.getProperty("file.listener.enabled", "true"));
            }
        }

        @Override
        public ExecutorService getExecutorService() {
            return executor;
        }
    }

}
