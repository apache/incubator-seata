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

import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.kv.model.GetValue;
import com.ecwid.consul.v1.kv.model.PutParams;
import io.netty.util.internal.ConcurrentSet;
import io.seata.common.thread.NamedThreadFactory;
import io.seata.common.util.NetUtil;
import io.seata.config.AbstractConfiguration;
import io.seata.config.ConfigFuture;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationChangeEvent;
import io.seata.config.ConfigurationChangeListener;
import io.seata.config.ConfigurationFactory;

import static io.seata.config.ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR;
import static io.seata.config.ConfigurationKeys.FILE_ROOT_CONFIG;

/**
 * The type Consul configuration.
 *
 * @author xingfudeshi @gmail.com
 */
public class ConsulConfiguration extends AbstractConfiguration {
    private volatile static ConsulConfiguration instance;
    private volatile static ConsulClient client;

    private static final Configuration FILE_CONFIG = ConfigurationFactory.CURRENT_FILE_INSTANCE;
    private static final String SERVER_ADDR_KEY = "serverAddr";
    private static final String CONFIG_TYPE = "consul";
	private static final String ACL_TOKEN = "token";
    private static final String FILE_CONFIG_KEY_PREFIX = FILE_ROOT_CONFIG + FILE_CONFIG_SPLIT_CHAR + CONFIG_TYPE
        + FILE_CONFIG_SPLIT_CHAR;
    private static final int THREAD_POOL_NUM = 1;
    private static final int MAP_INITIAL_CAPACITY = 8;
    private ExecutorService consulNotifierExecutor;
    private ConcurrentMap<String, Set<ConfigurationChangeListener>> configListenersMap = new ConcurrentHashMap<>(
        MAP_INITIAL_CAPACITY);
	private static Optional<String> tokenOptional;

    /**
     * default watch timeout in second
     */
    private static final int DEFAULT_WATCH_TIMEOUT = 60;
    private static final long CAS = 0L;

    private ConsulConfiguration() {
        consulNotifierExecutor = new ThreadPoolExecutor(THREAD_POOL_NUM, THREAD_POOL_NUM, Integer.MAX_VALUE,
            TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
            new NamedThreadFactory("consul-config-executor", THREAD_POOL_NUM));
		tokenOptional = Optional.ofNullable(FILE_CONFIG.getConfig(FILE_CONFIG_KEY_PREFIX + ACL_TOKEN));
    }

    /**
     * get instance
     *
     * @return instance
     */
    public static ConsulConfiguration getInstance() {
        if (null == instance) {
            synchronized (ConsulConfiguration.class) {
                if (null == instance) {
                    instance = new ConsulConfiguration();
                }
            }
        }
		getConsulClient();
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
		consulNotifierExecutor.execute(() -> {
			tokenOptional.ifPresent((token) -> complete(client.getKVValue(dataId, token), configFuture));
			tokenOptional.orElseGet(() -> {
				complete(client.getKVValue(dataId), configFuture);
				return dataId;
			});
		});
		return (String)configFuture.get();
    }

    @Override
    public boolean putConfig(String dataId, String content, long timeoutMills) {
        ConfigFuture configFuture = new ConfigFuture(dataId, content, ConfigFuture.ConfigOperation.PUT, timeoutMills);
        consulNotifierExecutor.execute(() -> {
			tokenOptional.ifPresent((token) -> {
				PutParams putParams = new PutParams();
				putParams.setCas(CAS);
				complete(client.setKVValue(dataId, content, token, putParams), configFuture);
			});
			tokenOptional.orElseGet(() -> {
				complete(client.setKVValue(dataId, content), configFuture);
				return dataId;
			});
		});
		return (Boolean)configFuture.get();
    }

    @Override
    public boolean putConfigIfAbsent(String dataId, String content, long timeoutMills) {
        ConfigFuture configFuture = new ConfigFuture(dataId, content, ConfigFuture.ConfigOperation.PUTIFABSENT,
            timeoutMills);
        consulNotifierExecutor.execute(() -> {
            PutParams putParams = new PutParams();
            //Setting CAS to 0 means that this is an atomic operation, created when key does not exist.
            putParams.setCas(CAS);
			tokenOptional.ifPresent((token) -> {
				complete(client.setKVValue(dataId, content, token, putParams), configFuture);
			});
			tokenOptional.orElseGet(() -> {
				complete(client.setKVValue(dataId, content, putParams), configFuture);
				return dataId;
			});
		});
        return (Boolean)configFuture.get();
    }

    @Override
    public boolean removeConfig(String dataId, long timeoutMills) {
        ConfigFuture configFuture = new ConfigFuture(dataId, null, ConfigFuture.ConfigOperation.REMOVE, timeoutMills);
        tokenOptional.ifPresent((token) -> {
			consulNotifierExecutor.execute(() -> complete(client.deleteKVValue(dataId, token), configFuture));
		});
		tokenOptional.orElseGet(() -> {
			consulNotifierExecutor.execute(() -> complete(client.deleteKVValue(dataId), configFuture));
			return dataId;
		});
		return (Boolean)configFuture.get();
    }

    @Override
    public void addConfigListener(String dataId, ConfigurationChangeListener listener) {
        if (null == dataId || null == listener) {
            return;
        }
        configListenersMap.putIfAbsent(dataId, new ConcurrentSet<>());
        ConsulListener consulListener = new ConsulListener(dataId, listener);
        configListenersMap.get(dataId).add(consulListener);
        consulListener.onProcessEvent(new ConfigurationChangeEvent());

    }

    @Override
    public void removeConfigListener(String dataId, ConfigurationChangeListener listener) {
        Set<ConfigurationChangeListener> configChangeListeners = getConfigListeners(dataId);
        if (configChangeListeners == null || listener == null) {
            return;
        }
        for (ConfigurationChangeListener entry : configChangeListeners) {
            ConfigurationChangeListener target = ((ConsulListener)entry).getTargetListener();
            if (listener.equals(target)) {
                entry.onShutDown();
                configChangeListeners.remove(entry);
                break;
            }
        }
    }

    @Override
    public Set<ConfigurationChangeListener> getConfigListeners(String dataId) {
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
                    String serverAddr = FILE_CONFIG.getConfig(FILE_CONFIG_KEY_PREFIX + SERVER_ADDR_KEY);
                    InetSocketAddress inetSocketAddress = NetUtil.toInetSocketAddress(serverAddr);
                    client = new ConsulClient(inetSocketAddress.getHostName(), inetSocketAddress.getPort());
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
    private void complete(Response<?> response, ConfigFuture configFuture) {
        if (null != response && null != response.getValue()) {
            Object value = response.getValue();
            if (value instanceof GetValue) {
                configFuture.setResult(((GetValue)value).getDecodedValue());
            } else {
                configFuture.setResult(value);
            }
        }
    }

    /**
     * The type Consul listener.
     */
    public static class ConsulListener implements ConfigurationChangeListener {

        private final ConfigurationChangeListener listener;
        private final String dataId;
        private long consulIndex;
        private final ExecutorService executor = new ThreadPoolExecutor(CORE_LISTENER_THREAD, MAX_LISTENER_THREAD, 0L,
            TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
            new NamedThreadFactory("consulListener", MAX_LISTENER_THREAD));

        /**
         * Instantiates a new Consul listener.
         *
         * @param dataId   the data id
         * @param listener the listener
         */
        public ConsulListener(String dataId, ConfigurationChangeListener listener) {
            this.dataId = dataId;
            this.listener = listener;
			tokenOptional.ifPresent((token) -> {
				this.consulIndex = client.getKVValue(dataId, token).getConsulIndex();
			});
			tokenOptional.orElseGet(() -> {
				this.consulIndex = client.getKVValue(dataId).getConsulIndex();
				return dataId;
			});
		}

        @Override
        public void onChangeEvent(ConfigurationChangeEvent event) {
            if (null != listener) {
                while (true) {
					QueryParams queryParams = new QueryParams(DEFAULT_WATCH_TIMEOUT, consulIndex);
					tokenOptional.ifPresent((token) -> {
						Response<GetValue> response = client.getKVValue(this.dataId, token, queryParams);
						Long currentIndex = response.getConsulIndex();
						if (currentIndex != null && currentIndex > consulIndex) {
							GetValue getValue = response.getValue();
							consulIndex = currentIndex;
							event.setDataId(dataId).setNewValue(getValue.getDecodedValue());
							listener.onChangeEvent(event);
						}
					});
					tokenOptional.orElseGet(() -> {
						Response<GetValue> response = client.getKVValue(this.dataId, queryParams);
						Long currentIndex = response.getConsulIndex();
						if (currentIndex != null && currentIndex > consulIndex) {
							GetValue getValue = response.getValue();
							consulIndex = currentIndex;
							event.setDataId(dataId).setNewValue(getValue.getDecodedValue());
							listener.onChangeEvent(event);
						}
						return dataId;
					});
				}
            }
        }

        @Override
        public ExecutorService getExecutorService() {
            return executor;
        }

        /**
         * Gets target listener.
         *
         * @return the target listener
         */
        public ConfigurationChangeListener getTargetListener() {
            return this.listener;
        }
    }
}
