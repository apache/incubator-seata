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
package io.seata.config.etcd;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.KeyValue;
import io.etcd.jetcd.Watch;
import io.etcd.jetcd.kv.DeleteResponse;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.kv.PutResponse;
import io.etcd.jetcd.kv.TxnResponse;
import io.etcd.jetcd.op.Cmp;
import io.etcd.jetcd.op.CmpTarget;
import io.etcd.jetcd.op.Op;
import io.etcd.jetcd.options.PutOption;
import io.etcd.jetcd.watch.WatchResponse;
import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.common.thread.NamedThreadFactory;
import io.seata.common.util.CollectionUtils;
import io.seata.config.AbstractConfiguration;
import io.seata.config.ConfigChangeListener;
import io.seata.config.ConfigFuture;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static io.netty.util.CharsetUtil.UTF_8;
import static io.seata.config.ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR;
import static io.seata.config.ConfigurationKeys.FILE_ROOT_CONFIG;

/**
 * @author xingfudeshi@gmail.com
 * @date 2019/05/10
 */
public class EtcdConfiguration extends AbstractConfiguration<ConfigChangeListener> {
    private static final Logger LOGGER = LoggerFactory.getLogger(EtcdConfiguration.class);
    private static volatile EtcdConfiguration instance;
    private static volatile Client client;

    private static final Configuration FILE_CONFIG = ConfigurationFactory.getInstance();
    private static final String SERVER_ADDR_KEY = "serverAddr";
    private static final String CONFIG_TYPE = "etcd3";
    private static final String FILE_CONFIG_KEY_PREFIX = FILE_ROOT_CONFIG + FILE_CONFIG_SPLIT_CHAR + CONFIG_TYPE + FILE_CONFIG_SPLIT_CHAR;
    private static final int THREAD_POOL_NUM = 1;
    private static final int MAP_INITIAL_CAPACITY = 8;
    private static ExecutorService etcdConfigExecutor = null;
    private static ExecutorService etcdNotifierExecutor = null;
    private static ConcurrentMap<String, List<ConfigChangeListener>> configListenersMap = null;
    private static ConcurrentHashMap<String, List<ConfigChangeNotifier>> configChangeNotifiersMap = null;

    private static final long VERSION_NOT_EXIST = 0;

    private EtcdConfiguration() {
    }

    /**
     * get instance
     *
     * @return
     */
    public static EtcdConfiguration getInstance() {
        if (null == instance) {
            synchronized (EtcdConfiguration.class) {
                if (null == instance) {
                    etcdConfigExecutor = new ThreadPoolExecutor(THREAD_POOL_NUM, THREAD_POOL_NUM,
                        Integer.MAX_VALUE, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), new NamedThreadFactory("etcd-config-executor", THREAD_POOL_NUM));
                    etcdNotifierExecutor = new ThreadPoolExecutor(THREAD_POOL_NUM, THREAD_POOL_NUM,
                        Integer.MAX_VALUE, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), new NamedThreadFactory("etcd-config-notifier-executor", THREAD_POOL_NUM));
                    configListenersMap = new ConcurrentHashMap<>(MAP_INITIAL_CAPACITY);
                    configChangeNotifiersMap = new ConcurrentHashMap<>(MAP_INITIAL_CAPACITY);
                    instance = new EtcdConfiguration();
                }
            }
        }
        return instance;
    }


    @Override
    public String getTypeName() {
        return CONFIG_TYPE;
    }

    @Override
    public String getConfig(String dataId, String defaultValue, long timeoutMills) {
        ConfigFuture configFuture = new ConfigFuture(dataId, defaultValue, ConfigFuture.ConfigOperation.GET, timeoutMills);
        etcdConfigExecutor.execute(() -> {
            complete(getClient().getKVClient().get(ByteSequence.from(dataId, UTF_8)), configFuture);
        });
        return (String) configFuture.get();
    }

    @Override
    public boolean putConfig(String dataId, String content, long timeoutMills) {
        ConfigFuture configFuture = new ConfigFuture(dataId, content, ConfigFuture.ConfigOperation.PUT, timeoutMills);
        etcdConfigExecutor.execute(() -> {
            complete(getClient().getKVClient().put(ByteSequence.from(dataId, UTF_8), ByteSequence.from(content, UTF_8)), configFuture);
        });
        return (Boolean) configFuture.get();
    }

    @Override
    public boolean putConfigIfAbsent(String dataId, String content, long timeoutMills) {
        ConfigFuture configFuture = new ConfigFuture(dataId, content, ConfigFuture.ConfigOperation.PUTIFABSENT, timeoutMills);
        etcdConfigExecutor.execute(() -> {
            //use etcd transaction to ensure the atomic operation
            complete(client.getKVClient().txn()
                //whether the key exists
                .If(new Cmp(ByteSequence.from(dataId, UTF_8), Cmp.Op.EQUAL, CmpTarget.version(VERSION_NOT_EXIST)))
                //not exist,then will create
                .Then(Op.put(ByteSequence.from(dataId, UTF_8), ByteSequence.from(content, UTF_8), PutOption.DEFAULT))
                .commit(), configFuture);
        });
        return (Boolean) configFuture.get();
    }

    @Override
    public boolean removeConfig(String dataId, long timeoutMills) {
        ConfigFuture configFuture = new ConfigFuture(dataId, null, ConfigFuture.ConfigOperation.REMOVE, timeoutMills);
        etcdConfigExecutor.execute(() -> {
            complete(getClient().getKVClient().delete(ByteSequence.from(dataId, UTF_8)), configFuture);
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
            etcdNotifierExecutor.submit(configChangeNotifier);
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
    public List getConfigListeners(String dataId) {
        return configListenersMap.get(dataId);
    }

    /**
     * get client
     *
     * @return client
     */
    private static Client getClient() {
        if (null == client) {
            synchronized (EtcdConfiguration.class) {
                if (null == client) {
                    client = Client.builder().endpoints(FILE_CONFIG.getConfig(FILE_CONFIG_KEY_PREFIX + SERVER_ADDR_KEY)).build();
                }
            }
        }
        return client;
    }

    /**
     * complete the future
     *
     * @param completableFuture
     * @param configFuture
     * @param <T>
     */
    private <T> void complete(CompletableFuture<T> completableFuture, ConfigFuture configFuture) {
        try {
            T response = completableFuture.get();
            if (response instanceof GetResponse) {
                List<KeyValue> keyValues = ((GetResponse) response).getKvs();
                if (CollectionUtils.isNotEmpty(keyValues)) {
                    ByteSequence value = keyValues.get(0).getValue();
                    if (null != value) {
                        configFuture.setResult(value.toString(UTF_8));
                    }
                }
            } else if (response instanceof PutResponse) {
                configFuture.setResult(Boolean.TRUE);
            } else if (response instanceof TxnResponse) {
                boolean result = ((TxnResponse) response).isSucceeded();
                //create key if file does not exist)
                if (result) {
                    configFuture.setResult(Boolean.TRUE);
                }
            } else if (response instanceof DeleteResponse) {
                configFuture.setResult(Boolean.TRUE);
            } else {
                throw new ShouldNeverHappenException("unsupported response type");
            }
        } catch (Exception e) {
            LOGGER.error("error occurred while completing the future{}", e.getMessage());
        }
    }

    /**
     * the type config change notifier
     */
    private static class ConfigChangeNotifier implements Runnable {
        private final String dataId;
        private final ConfigChangeListener listener;
        private Watch.Watcher watcher;

        ConfigChangeNotifier(String dataId, ConfigChangeListener listener) {
            this.dataId = dataId;
            this.listener = listener;
        }

        /**
         * get the listener
         *
         * @return ConfigChangeListener
         */
        ConfigChangeListener getListener() {
            return this.listener;
        }

        @Override
        public void run() {
            Watch watchClient = getClient().getWatchClient();
            watcher = watchClient.watch(ByteSequence.from(dataId, UTF_8), new Watch.Listener() {
                @Override
                public void onNext(WatchResponse response) {
                    notifyListeners();
                }

                @Override
                public void onError(Throwable throwable) {

                }

                @Override
                public void onCompleted() {

                }
            });
        }

        /**
         * notify listeners
         */
        private void notifyListeners() {
            try {
                GetResponse getResponse = getClient().getKVClient().get(ByteSequence.from(dataId, UTF_8)).get();
                List<KeyValue> keyValues = getResponse.getKvs();
                if (CollectionUtils.isNotEmpty(keyValues)) {
                    for (ConfigChangeListener listener : configListenersMap.get(this.dataId)) {
                        listener.receiveConfigInfo(keyValues.get(0).getValue().toString(UTF_8));
                    }
                }
            } catch (Exception e) {
                LOGGER.error("error occurred while getting value{}", e.getMessage());
            }
        }


        /**
         * stop the notifier
         */
        public void stop() {
            this.watcher.close();
        }
    }
}
