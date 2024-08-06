package org.apache.seata.config.raft;

import org.apache.seata.common.exception.NotSupportYetException;
import org.apache.seata.common.util.CollectionUtils;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.config.*;
import org.apache.seata.config.store.ConfigStoreManager;
import org.apache.seata.config.store.ConfigStoreManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.apache.seata.common.ConfigurationKeys.*;
import static org.apache.seata.common.Constants.*;



public class RaftConfigurationServer extends AbstractConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(RaftConfigurationServer.class);
    private static volatile RaftConfigurationServer instance;
    private static final Configuration FILE_CONFIG = ConfigurationFactory.CURRENT_FILE_INSTANCE;
    private static ConfigStoreManager configStoreManager;
    private static String CURRENT_NAMESPACE;
    private static String CURRENT_DATA_ID;
    private static final String CONFIG_TYPE = "raft";
    private static volatile Properties seataConfig = new Properties();
    private static final int MAP_INITIAL_CAPACITY = 8;
    private static final ConcurrentMap<String, ConcurrentMap<ConfigurationChangeListener, ConfigStoreListener>> CONFIG_LISTENERS_MAP
            = new ConcurrentHashMap<>(MAP_INITIAL_CAPACITY);

    private static void initServerConfig() {
        configStoreManager = ConfigStoreManagerFactory.getInstance();
        CURRENT_NAMESPACE = FILE_CONFIG.getConfig(CONFIG_STORE_NAMESPACE, DEFAULT_STORE_NAMESPACE);
        CURRENT_DATA_ID = FILE_CONFIG.getConfig(CONFIG_STORE_DATA_ID, DEFAULT_STORE_DATA_ID);
        // load config from store
        Map<String, Object> configMap = configStoreManager.getAll(CURRENT_NAMESPACE, CURRENT_DATA_ID);
        seataConfig.putAll(configMap);
        // build listener
        ConfigStoreListener storeListener = new ConfigStoreListener(CURRENT_NAMESPACE, CURRENT_DATA_ID, null);
        configStoreManager.addConfigListener(CURRENT_NAMESPACE, CURRENT_DATA_ID, storeListener);
    }


    public static RaftConfigurationServer getInstance() {
        if (instance == null) {
            synchronized (RaftConfigurationServer.class) {
                if (instance == null) {
                    instance = new RaftConfigurationServer();
                }
            }
        }
        return instance;
    }

    private RaftConfigurationServer() {
        initServerConfig();
    }

    @Override
    public String getTypeName() {
        return CONFIG_TYPE;
    }

    @Override
    public boolean putConfig(String dataId, String content, long timeoutMills) {
        throw new NotSupportYetException("not support operation putConfig");
    }

    @Override
    public String getLatestConfig(String dataId, String defaultValue, long timeoutMills) {
        String value = seataConfig.getProperty(dataId);
        if (value == null) {
            value = configStoreManager.get(CURRENT_NAMESPACE, CURRENT_DATA_ID, dataId);
        }
        return value == null ? defaultValue : value;
    }

    @Override
    public boolean putConfigIfAbsent(String dataId, String content, long timeoutMills) {
        throw new NotSupportYetException("not support atomic operation putConfigIfAbsent");
    }

    @Override
    public boolean removeConfig(String dataId, long timeoutMills) {
        throw new NotSupportYetException("not support operation removeConfig");
    }

    @Override
    public void addConfigListener(String dataId, ConfigurationChangeListener listener) {
        if (StringUtils.isBlank(dataId) || listener == null) {
            return;
        }
        ConfigStoreListener storeListener = new ConfigStoreListener(CURRENT_NAMESPACE, dataId, listener);
        CONFIG_LISTENERS_MAP.computeIfAbsent(dataId, key -> new ConcurrentHashMap<>())
                .put(listener, storeListener);
        configStoreManager.addConfigListener(CURRENT_NAMESPACE, dataId, storeListener);
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
                    ConfigStoreListener storeListener = null;
                    Map<ConfigurationChangeListener, ConfigStoreListener> configListeners = CONFIG_LISTENERS_MAP.get(dataId);
                    if (configListeners != null) {
                        storeListener = configListeners.get(listener);
                        configListeners.remove(entry);
                    }
                    if (storeListener != null) {
                        configStoreManager.removeConfigListener(CURRENT_NAMESPACE, dataId, storeListener);
                    }
                    break;
                }
            }
        }
    }

    @Override
    public Set<ConfigurationChangeListener> getConfigListeners(String dataId) {
        ConcurrentMap<ConfigurationChangeListener, ConfigStoreListener> configListeners = CONFIG_LISTENERS_MAP.get(dataId);
        if (CollectionUtils.isNotEmpty(configListeners)){
            return configListeners.keySet();
        } else {
            return null;
        }
    }


    /**
     * the type config change listener for raft config store
     */
    private static class ConfigStoreListener implements ConfigurationChangeListener {
        private final String namespace;
        private final String dataId;
        private final ConfigurationChangeListener listener;

        public ConfigStoreListener(String namespace, String dataId, ConfigurationChangeListener listener) {
            this.namespace = namespace;
            this.dataId = dataId;
            this.listener = listener;
        }

        @Override
        public void onChangeEvent(ConfigurationChangeEvent event) {
            if (CURRENT_DATA_ID.equals(event.getDataId())) {
                Properties seataConfigNew = new Properties();
                seataConfigNew.putAll(configStoreManager.getAll(CURRENT_NAMESPACE, CURRENT_DATA_ID));

                //Get all the monitored dataids and judge whether it has been modified
                for (Map.Entry<String, ConcurrentMap<ConfigurationChangeListener, ConfigStoreListener>> entry : CONFIG_LISTENERS_MAP.entrySet()) {
                    String listenedDataId = entry.getKey();
                    String propertyOld = seataConfig.getProperty(listenedDataId, "");
                    String propertyNew = seataConfigNew.getProperty(listenedDataId, "");
                    if (!propertyOld.equals(propertyNew)) {
                        ConfigurationChangeEvent newEvent = new ConfigurationChangeEvent()
                                .setDataId(listenedDataId)
                                .setNewValue(propertyNew)
                                .setNamespace(CURRENT_NAMESPACE)
                                .setChangeType(ConfigurationChangeType.MODIFY);

                        ConcurrentMap<ConfigurationChangeListener, ConfigStoreListener> configListeners = entry.getValue();
                        for (ConfigurationChangeListener configListener : configListeners.keySet()) {
                            configListener.onProcessEvent(newEvent);
                        }
                    }
                }
                seataConfig = seataConfigNew;
                return;
            }
            // Compatible with old writing
            listener.onProcessEvent(event);
        }
    }
}
