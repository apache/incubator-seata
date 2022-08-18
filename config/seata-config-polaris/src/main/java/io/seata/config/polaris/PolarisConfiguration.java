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
package io.seata.config.polaris;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.seata.common.ConfigurationKeys;
import io.seata.common.exception.NotSupportYetException;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.StringUtils;
import io.seata.config.AbstractConfiguration;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationChangeEvent;
import io.seata.config.ConfigurationChangeListener;
import io.seata.config.ConfigurationFactory;
import io.seata.config.polaris.client.PolarisConfigClient;
import io.seata.config.polaris.client.PolarisConfigProperties;
import io.seata.config.processor.ConfigProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Polaris configuration.
 *
 * @author <a href="mailto:iskp.me@gmail.com">Palmer Xu</a> 2022-08-17
 */
public class PolarisConfiguration extends AbstractConfiguration {

	private static volatile PolarisConfiguration instance;

	private static final Logger LOGGER = LoggerFactory.getLogger(PolarisConfiguration.class);

	private static volatile PolarisConfigClient client;

	private static volatile Properties polarisConfig = new Properties();

	/**
	 * Seata Config.
	 */
	private static volatile Properties seataConfig = new Properties();

	private static final Configuration FILE_CONFIG = ConfigurationFactory.CURRENT_FILE_INSTANCE;

	private static final String DEFAULT_FILE_NAME = "seata.properties";

	private static final String CONFIG_TYPE = "polaris";

	private static final String DEFAULT_NAMESPACE = "default";

	private static final String POLARIS_NAMESPACE_KEY = "namespace";

	private static final String POLARIS_GROUP_KEY = "group";

	private static final String POLARIS_SERVER_KEY = "serverAddr";

	private static final String POLARIS_SERVER_ACCESS_TOKEN = "token";

	private static final String POLARIS_SERVER_PULL_INTERVAL_TIME = "pullIntervalTime";

	private static final String POLARIS_SERVER_CONNECT_TIME = "connectTimeout";

	private static final String POLARIS_SERVER_READ_TIME = "readTimeout";

	private static final String POLARIS_FILE_KEY = "file";

	private static final int MAP_INITIAL_CAPACITY = 8;

	private static final ConcurrentMap<String, ConcurrentMap<ConfigurationChangeListener, PolarisConfigListener>> CONFIG_LISTENERS_MAP = new ConcurrentHashMap<>(MAP_INITIAL_CAPACITY);

	/**
	 * Get instance of PolarisConfiguration
	 *
	 * @return instance
	 */
	public static PolarisConfiguration getInstance() {
		if (instance == null) {
			synchronized (PolarisConfiguration.class) {
				if (instance == null) {
					instance = new PolarisConfiguration();
				}
			}
		}
		return instance;
	}

	/**
	 * Instantiates a new PolarisConfiguration.
	 */
	private PolarisConfiguration() {
		if (client == null) {
			try {
				// Read polaris center config properties
				PolarisConfigProperties properties = getConfigProperties();
				// build context
				client = PolarisConfigClient.getClient(properties);

				initSeataConfig();

			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	private static void initSeataConfig() {
		try {

			Optional<String> opt = client.getConfig(polarisConfig.getProperty(POLARIS_NAMESPACE_KEY),
				polarisConfig.getProperty(POLARIS_GROUP_KEY), polarisConfig.getProperty(POLARIS_FILE_KEY), (int) DEFAULT_CONFIG_TIMEOUT);

			if (opt.isPresent()) {
				seataConfig = ConfigProcessor.processConfig(opt.get(), getPolarisDataType());
			}
		} catch (Exception e) {
			LOGGER.error("init config properties error", e);
		}
	}

	private static String getPolarisFileName() {
		return FILE_CONFIG.getConfig(getPolarisFileKey(), DEFAULT_FILE_NAME);
	}

	private static String getPolarisDataType() {
		return ConfigProcessor.resolverConfigDataType(getPolarisFileName());
	}

	public static String getPolarisNameSpaceFileKey() {
		return String.join(ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR, ConfigurationKeys.FILE_ROOT_CONFIG, CONFIG_TYPE, POLARIS_NAMESPACE_KEY);
	}

	public static String getPolarisAddrFileKey() {
		return String.join(ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR, ConfigurationKeys.FILE_ROOT_CONFIG, CONFIG_TYPE, POLARIS_SERVER_KEY);
	}

	public static String getPolarisAccessTokenKey() {
		return String.join(ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR, ConfigurationKeys.FILE_ROOT_CONFIG, CONFIG_TYPE, POLARIS_SERVER_ACCESS_TOKEN);
	}

	public static String getPolarisGroupKey() {
		return String.join(ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR, ConfigurationKeys.FILE_ROOT_CONFIG, CONFIG_TYPE, POLARIS_GROUP_KEY);
	}

	public static String getPolarisFileKey() {
		return String.join(ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR, ConfigurationKeys.FILE_ROOT_CONFIG, CONFIG_TYPE, POLARIS_FILE_KEY);
	}

	public static String getPolarisConnectTimeoutKey() {
		return String.join(ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR, ConfigurationKeys.FILE_ROOT_CONFIG, CONFIG_TYPE, POLARIS_SERVER_CONNECT_TIME);
	}

	public static String getPolarisPullIntervalTimeKey() {
		return String.join(ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR, ConfigurationKeys.FILE_ROOT_CONFIG, CONFIG_TYPE, POLARIS_SERVER_PULL_INTERVAL_TIME);
	}

	public static String getPolarisReadTimeoutKey() {
		return String.join(ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR, ConfigurationKeys.FILE_ROOT_CONFIG, CONFIG_TYPE, POLARIS_SERVER_READ_TIME);
	}

	private static PolarisConfigProperties getConfigProperties() {

		PolarisConfigProperties properties = new PolarisConfigProperties();

		// address
		String address = System.getProperty(POLARIS_SERVER_KEY);
		if (StringUtils.isBlank(address)) {
			address = FILE_CONFIG.getConfig(getPolarisAddrFileKey());
		}

		if (StringUtils.isNotBlank(address)) {
			polarisConfig.setProperty(POLARIS_SERVER_KEY, address);
		} else {
			throw new RuntimeException("Config server address is blank. Please check your config .");
		}

		properties.address(address);

		// token
		String token = System.getProperty(POLARIS_SERVER_ACCESS_TOKEN);
		if (StringUtils.isBlank(token)) {
			token = FILE_CONFIG.getConfig(getPolarisAccessTokenKey());
		}

		if (StringUtils.isNotBlank(token)) {
			polarisConfig.setProperty(POLARIS_SERVER_ACCESS_TOKEN, token);
		} else {
			throw new RuntimeException("Config server access token is blank. Please check your config .");
		}

		properties.token(token);

		// readTimeout
		String connectTimeout = System.getProperty(POLARIS_SERVER_CONNECT_TIME);
		if (StringUtils.isBlank(connectTimeout)) {
			connectTimeout = FILE_CONFIG.getConfig(getPolarisConnectTimeoutKey());
		}

		if (StringUtils.isNotBlank(connectTimeout)) {
			polarisConfig.setProperty(POLARIS_SERVER_CONNECT_TIME, connectTimeout);
			properties.connectTimeout(Integer.parseInt(connectTimeout));
		}

		// pullIntervalTime
		String pullIntervalTime = System.getProperty(POLARIS_SERVER_PULL_INTERVAL_TIME);
		if (StringUtils.isBlank(pullIntervalTime)) {
			pullIntervalTime = FILE_CONFIG.getConfig(getPolarisPullIntervalTimeKey());
		}

		if (StringUtils.isNotBlank(pullIntervalTime)) {
			polarisConfig.setProperty(POLARIS_SERVER_PULL_INTERVAL_TIME, pullIntervalTime);
			properties.pullIntervalTime(Long.parseLong(pullIntervalTime));
		}


		// readTimeout
		String readTimeout = System.getProperty(POLARIS_SERVER_READ_TIME);
		if (StringUtils.isBlank(readTimeout)) {
			readTimeout = FILE_CONFIG.getConfig(getPolarisReadTimeoutKey());
		}

		if (StringUtils.isNotBlank(readTimeout)) {
			polarisConfig.setProperty(POLARIS_SERVER_READ_TIME, readTimeout);
			properties.readTimeout(Integer.parseInt(readTimeout));
		}

		// namespace
		String namespace = System.getProperty(POLARIS_NAMESPACE_KEY);
		if (StringUtils.isBlank(namespace)) {
			namespace = FILE_CONFIG.getConfig(getPolarisNameSpaceFileKey());
			if (namespace == null) {
				namespace = DEFAULT_NAMESPACE;
			}
		}

		if (StringUtils.isNotBlank(namespace)) {
			polarisConfig.setProperty(POLARIS_NAMESPACE_KEY, namespace);
		}

		// group
		String group = System.getProperty(POLARIS_GROUP_KEY);
		if (StringUtils.isBlank(group)) {
			group = FILE_CONFIG.getConfig(getPolarisGroupKey());
		}

		if (StringUtils.isNotBlank(group)) {
			polarisConfig.setProperty(POLARIS_GROUP_KEY, group);
		}
		else {
			throw new RuntimeException("Config server group is blank. Please check your config .");
		}

		// file
		String file = System.getProperty(POLARIS_FILE_KEY);
		if (StringUtils.isBlank(file)) {
			file = FILE_CONFIG.getConfig(getPolarisFileKey());
			if (StringUtils.isBlank(file)) {
				file = DEFAULT_FILE_NAME;
			}
		}

		if (StringUtils.isNotBlank(file)) {
			polarisConfig.setProperty(POLARIS_FILE_KEY, file);
		}

		return properties;
	}

	/**
	 * Gets type name.
	 *
	 * @return the type name
	 */
	@Override
	public String getTypeName() {
		return CONFIG_TYPE;
	}

	/**
	 * Put config boolean.
	 *
	 * @param dataId       the data id
	 * @param content      the content
	 * @param timeoutMills the timeout mills
	 * @return the boolean
	 */
	@Override
	public boolean putConfig(String dataId, String content, long timeoutMills) {
		throw new NotSupportYetException("not support putConfig");
	}

	/**
	 * Get latest config.
	 *
	 * @param dataId       the data id
	 * @param defaultValue the default value
	 * @param timeoutMills the timeout mills
	 * @return the Latest config
	 */
	@Override
	public String getLatestConfig(String dataId, String defaultValue, long timeoutMills) {
		String value = seataConfig.getProperty(dataId);
		if (null == value) {
			try {
				Optional<String> opt = client.getConfig(polarisConfig.getProperty(POLARIS_NAMESPACE_KEY),
					polarisConfig.getProperty(POLARIS_GROUP_KEY), polarisConfig.getProperty(POLARIS_FILE_KEY), (int) DEFAULT_CONFIG_TIMEOUT);
				if (opt.isPresent()) {
					seataConfig = ConfigProcessor.processConfig(opt.get(), getPolarisDataType());
					value = seataConfig.getProperty(dataId);
				}
			} catch (Exception e) {
				LOGGER.error(e.getMessage());
			}
		}
		return value == null ? defaultValue : value;
	}

	/**
	 * Put config if absent boolean.
	 *
	 * @param dataId       the data id
	 * @param content      the content
	 * @param timeoutMills the timeout mills
	 * @return the boolean
	 */
	@Override
	public boolean putConfigIfAbsent(String dataId, String content, long timeoutMills) {
		throw new NotSupportYetException("not support atomic operation putConfigIfAbsent");
	}

	/**
	 * Remove config boolean.
	 *
	 * @param dataId       the data id
	 * @param timeoutMills the timeout mills
	 * @return the boolean
	 */
	@Override
	public boolean removeConfig(String dataId, long timeoutMills) {
		throw new NotSupportYetException("not support removeConfig");
	}

	/**
	 * Add config listener.
	 *
	 * @param dataId   the data id
	 * @param listener the listener
	 */
	@Override
	public void addConfigListener(String dataId, ConfigurationChangeListener listener) {
		if (StringUtils.isBlank(dataId) || listener == null) {
			return;
		}
		try {
			PolarisConfigListener polarisConfigListener = new PolarisConfigListener(polarisConfig.getProperty(POLARIS_NAMESPACE_KEY),
				polarisConfig.getProperty(POLARIS_GROUP_KEY), dataId, listener);
			CONFIG_LISTENERS_MAP.computeIfAbsent(dataId, key -> new ConcurrentHashMap<>())
				.put(listener, polarisConfigListener);
			client.addListener(polarisConfig.getProperty(POLARIS_NAMESPACE_KEY), polarisConfig.getProperty(POLARIS_GROUP_KEY), dataId, polarisConfigListener);
		} catch (Exception e) {
			LOGGER.error("add polaris config listener error:{}", e.getMessage(), e);
		}
	}

	/**
	 * Remove config listener.
	 *
	 * @param dataId   the data id
	 * @param listener the listener
	 */
	@Override
	public void removeConfigListener(String dataId, ConfigurationChangeListener listener) {
		if (StringUtils.isBlank(dataId) || listener == null) {
			return;
		}
		Set<ConfigurationChangeListener> configChangeListeners = getConfigListeners(dataId);
		if (CollectionUtils.isNotEmpty(configChangeListeners)) {
			for (ConfigurationChangeListener entry : configChangeListeners) {
				if (listener.equals(entry)) {
					PolarisConfigListener polarisConfigListener = null;
					Map<ConfigurationChangeListener, PolarisConfigListener> configListeners = CONFIG_LISTENERS_MAP.get(dataId);
					if (configListeners != null) {
						polarisConfigListener = configListeners.get(listener);
						configListeners.remove(entry);
					}
					if (polarisConfigListener != null) {
						client.removeListener(polarisConfig.getProperty(POLARIS_NAMESPACE_KEY),
							polarisConfig.getProperty(POLARIS_GROUP_KEY), dataId, polarisConfigListener);
					}
					break;
				}
			}
		}
	}

	/**
	 * Gets config listeners.
	 *
	 * @param dataId the data id
	 * @return the config listeners
	 */
	@Override
	public Set<ConfigurationChangeListener> getConfigListeners(String dataId) {
		Map<ConfigurationChangeListener, PolarisConfigListener> configListeners = CONFIG_LISTENERS_MAP.get(dataId);
		if (CollectionUtils.isNotEmpty(configListeners)) {
			return configListeners.keySet();
		} else {
			return null;
		}
	}


	private static class PolarisConfigListener implements PolarisConfigChangeListener {

		private final String namespace;

		private final String group;

		private final String fileName;

		private final ConfigurationChangeListener listener;

		public PolarisConfigListener(String namespace, String group, String fileName,
			ConfigurationChangeListener listener) {
			this.namespace = namespace;
			this.group = group;
			this.fileName = fileName;
			this.listener = listener;
		}

		/**
		 * Receive config info.
		 *
		 * @param configInfo config info
		 */
		@Override public void receiveConfigInfo(final String namespace, final String group, final String fileName, String configInfo) {
			//The new configuration method to puts all configurations into a dateId
			if (fileName.equals(this.fileName) && namespace.equals(this.namespace) && group.equals(this.group)) {
				Properties seataConfigNew = new Properties();
				if (StringUtils.isNotBlank(configInfo)) {
					try {
						seataConfigNew = ConfigProcessor.processConfig(configInfo, getPolarisDataType());
					} catch (IOException e) {
						LOGGER.error("load config properties error", e);
						return;
					}
				}

				//Get all the monitored dataids and judge whether it has been modified
				for (Map.Entry<String, ConcurrentMap<ConfigurationChangeListener, PolarisConfigListener>> entry : CONFIG_LISTENERS_MAP.entrySet()) {
					String listenedDataId = entry.getKey();
					String propertyOld = seataConfig.getProperty(listenedDataId, "");
					String propertyNew = seataConfigNew.getProperty(listenedDataId, "");
					if (!propertyOld.equals(propertyNew)) {
						ConfigurationChangeEvent event = new ConfigurationChangeEvent()
							.setDataId(listenedDataId)
							.setNewValue(propertyNew)
							.setNamespace(group);

						ConcurrentMap<ConfigurationChangeListener, PolarisConfigListener> configListeners = entry.getValue();
						for (ConfigurationChangeListener configListener : configListeners.keySet()) {
							configListener.onProcessEvent(event);
						}
					}
				}

				seataConfig = seataConfigNew;
				return;
			}

			//Compatible with old writing
			ConfigurationChangeEvent event = new ConfigurationChangeEvent().setDataId(fileName).setNewValue(configInfo)
				.setNamespace(group);
			listener.onProcessEvent(event);
		}
	}
}
