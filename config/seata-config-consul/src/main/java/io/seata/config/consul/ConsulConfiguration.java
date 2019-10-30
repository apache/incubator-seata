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
package io.seata.config.consul;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.kv.model.GetValue;
import com.ecwid.consul.v1.kv.model.PutParams;
import io.seata.common.thread.NamedThreadFactory;
import io.seata.config.AbstractConfiguration;
import io.seata.config.ConfigChangeListener;
import io.seata.config.ConfigFuture;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static io.seata.config.ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR;
import static io.seata.config.ConfigurationKeys.FILE_ROOT_CONFIG;

/**
 * @author xingfudeshi@gmail.com
 * @date 2019/05/05
 */
public class ConsulConfiguration extends AbstractConfiguration<ConfigChangeListener> {
    private volatile static ConsulConfiguration instance;
    private volatile static ConsulClient client;

    private static final Configuration FILE_CONFIG = ConfigurationFactory.CURRENT_FILE_INSTANCE;
    private static final String SERVER_ADDR_KEY = "serverAddr";
    private static final String CONFIG_TYPE = "consul";
    private static final String FILE_CONFIG_KEY_PREFIX = FILE_ROOT_CONFIG + FILE_CONFIG_SPLIT_CHAR + CONFIG_TYPE + FILE_CONFIG_SPLIT_CHAR;
    private static final int THREAD_POOL_NUM = 1;
    private static final int MAP_INITIAL_CAPACITY = 8;
    private ExecutorService consulNotifierExecutor;
    private ConcurrentMap<String, List<ConfigChangeListener>> configListenersMap;
    private ConcurrentMap<String, List<ConfigChangeNotifier>> configChangeNotifiersMap;

    /**
     * default watch timeout in second
     */
    private static final int DEFAULT_WATCH_TIMEOUT = 60;
    private static final long CAS = 0L;


    private ConsulConfiguration() {
        consulNotifierExecutor = new ThreadPoolExecutor(THREAD_POOL_NUM, THREAD_POOL_NUM,
            Integer.MAX_VALUE, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), new NamedThreadFactory("consul-config-executor", THREAD_POOL_NUM));
        configListenersMap = new ConcurrentHashMap<>(MAP_INITIAL_CAPACITY);
        configChangeNotifiersMap = new ConcurrentHashMap<>(MAP_INITIAL_CAPACITY);
    }

    /**
     * get instance
     *
     * @return
     */
    public static ConsulConfiguration getInstance() {
        if (null == instance) {
            synchronized (ConsulConfiguration.class) {
                if (null == instance) {
                    instance = new ConsulConfiguration();
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
        ConfigFuture configFuture = new ConfigFuture(dataId, defaultValue, ConfigFuture.ConfigOperation.GET, timeoutMills);
        consulNotifierExecutor.execute(() -> {
            complete(getConsulClient().getKVValue(dataId), configFuture);
        });
        return (String) configFuture.get();
    }

    @Override
    public boolean putConfig(String dataId, String content, long timeoutMills) {
        ConfigFuture configFuture = new ConfigFuture(dataId, content, ConfigFuture.ConfigOperation.PUT, timeoutMills);
        consulNotifierExecutor.execute(() -> {
            complete(getConsulClient().setKVValue(dataId, content), configFuture);
        });
        return (Boolean) configFuture.get();
    }

    @Override
    public boolean putConfigIfAbsent(String dataId, String content, long timeoutMills) {
        ConfigFuture configFuture = new ConfigFuture(dataId, content, ConfigFuture.ConfigOperation.PUTIFABSENT, timeoutMills);
        consulNotifierExecutor.execute(() -> {
            PutParams putParams = new PutParams();
            //Setting CAS to 0 means that this is an atomic operation, created when key does not exist.
            putParams.setCas(CAS);
            complete(getConsulClient().setKVValue(dataId, content, putParams), configFuture);
        });
        return (Boolean) configFuture.get();
    }

    @Override
    public boolean removeConfig(String dataId, long timeoutMills) {
        ConfigFuture configFuture = new ConfigFuture(dataId, null, ConfigFuture.ConfigOperation.REMOVE, timeoutMills);
        consulNotifierExecutor.execute(() -> {
            complete(getConsulClient().deleteKVValue(dataId), configFuture);
        });
        return (Boolean) configFuture.get();
    }

    @Override
    public void addConfigListener(String dataId, ConfigChangeListener listener) {
        configListenersMap.putIfAbsent(dataId, new ArrayList<>());
        configChangeNotifiersMap.putIfAbsent(dataId, new ArrayList<>());
        ConfigChangeNotifier configChangeNotifier = new ConfigChangeNotifier(dataId, listener);
        configChangeNotifiersMap.get(dataId).add(configChangeNotifier);
        if (null != listener.getExecutor()) {
            listener.getExecutor().submit(configChangeNotifier);
        } else {
            consulNotifierExecutor.submit(configChangeNotifier);
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
        if (null != listener.getExecutor()) {
            listener.getExecutor().shutdownNow();
        }
        //remove and stop the configChangeNotifier
        List<ConfigChangeNotifier> configChangeNotifiers = configChangeNotifiersMap.get(dataId);
        List<ConfigChangeNotifier> newConfigChangeNotifiers = new ArrayList<>();
        for (ConfigChangeNotifier configChangeNotifier : configChangeNotifiers) {
            if (!listener.equals(configChangeNotifier.getListener())) {
                newConfigChangeNotifiers.add(configChangeNotifier);
            } else {
                configChangeNotifier.stop();
            }
        }
        configChangeNotifiersMap.put(dataId, newConfigChangeNotifiers);
    }

    @Override
    public List<ConfigChangeListener> getConfigListeners(String dataId) {
        return configListenersMap.get(dataId);
    }

    @Override
    public String getTypeName() {
        return CONFIG_TYPE;
    }


    /**
     * get consul client
     *
     * @return client
     */
    private static ConsulClient getConsulClient() {
        if (null == client) {
            synchronized (ConsulConfiguration.class) {
                if (null == client) {
                    client = new ConsulClient(FILE_CONFIG.getConfig(FILE_CONFIG_KEY_PREFIX + SERVER_ADDR_KEY));
                }
            }
        }
        return client;
    }

    /**
     * complete the future
     *
     * @param response
     * @param configFuture
     */
    private void complete(Response response, ConfigFuture configFuture) {
        if (null != response && null != response.getValue()) {
            Object value = response.getValue();
            if (value instanceof GetValue) {
                configFuture.setResult(((GetValue) value).getDecodedValue());
            } else {
                configFuture.setResult(value);
            }
        }
    }

    /**
     * the type config change notifier
     */
    private class ConfigChangeNotifier implements Runnable {
        private final String dataId;
        private final ConfigChangeListener listener;
        private long consulIndex;
        private boolean running;

        public ConfigChangeNotifier(String dataId, ConfigChangeListener listener) {
            this.dataId = dataId;
            this.listener = listener;
            this.consulIndex = getConsulClient().getKVValue(this.dataId).getConsulIndex();
            this.running = true;
        }

        /**
         * get the listener
         *
         * @return
         */
        public ConfigChangeListener getListener() {
            return this.listener;
        }

        @Override
        public void run() {
            while (running) {
                process();
            }
        }

        /**
         * process
         */
        private void process() {
            QueryParams queryParams = new QueryParams(DEFAULT_WATCH_TIMEOUT, consulIndex);
            Response<GetValue> response = getConsulClient().getKVValue(this.dataId, queryParams);
            Long currentIndex = response.getConsulIndex();
            if (currentIndex != null && currentIndex > consulIndex) {
                GetValue getValue = response.getValue();
                consulIndex = currentIndex;
                for (ConfigChangeListener listener : configListenersMap.get(this.dataId)) {
                    listener.receiveConfigInfo(getValue.getDecodedValue());
                }
            }

        }

        /**
         * stop the notifier
         */
        public void stop() {
            this.running = false;
        }
    }
}