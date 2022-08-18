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
package io.seata.config.polaris.client;

import io.seata.common.util.JacksonUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import io.netty.util.concurrent.DefaultThreadFactory;
import io.seata.common.util.StringUtils;
import io.seata.config.polaris.PolarisConfigChangeListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.seata.config.polaris.client.PolarisConfigClient.PolarisServerRequests.GET_CONFIG_FILE_BY_LONG_PULLING;
import static io.seata.config.polaris.client.PolarisConfigClient.Request.ACCESS_TOKEN_HEADER;
import static io.seata.config.polaris.client.PolarisConfigClient.Request.LONG_PULL_REQUEST_TIMEOUT;
import static io.seata.config.polaris.client.PolarisConfigClient.Request.SERVER_DEFAULT_LONG_PULL_REQUEST_TIMEOUT;
import static io.seata.config.polaris.client.SimpleHttpRequest.CHARSET_UTF8;
import static io.seata.config.polaris.client.SimpleHttpRequest.CONTENT_TYPE_JSON;
import static java.net.HttpURLConnection.HTTP_OK;

/**
 * {@link PolarisConfigClient} Definition .
 *
 * @author <a href="mailto:iskp.me@gmail.com">Palmer Xu</a> 2022-08-18
 */
public final class PolarisConfigClient {

	/**
	 * Logging instance .
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(PolarisConfigClient.class);

	/**
	 * Global single instance object of {@link PolarisConfigClient} .
	 */
	private static volatile PolarisConfigClient instance;

	/**
	 * Polaris Config Client Properties .
	 */
	private static PolarisConfigProperties properties;

	/**
	 * Initialized config-listener worker .
	 */
	private final ConfigListenerWorker listenerWorker = new ConfigListenerWorker();

	/**
	 * Get or create new {@link PolarisConfigClient} instance with {@link PolarisConfigClient}.
	 *
	 * @param properties {@link PolarisConfigClient} build config properties instance of {@link PolarisConfigProperties}
	 * @return instance of {@link PolarisConfigClient}
	 */
	public static PolarisConfigClient getClient(PolarisConfigProperties properties) {
		if (instance == null) {
			synchronized (PolarisConfigClient.class) {
				if (instance == null) {
					instance = new PolarisConfigClient(properties);
				}
			}
		}
		return instance;
	}

	private PolarisConfigClient(PolarisConfigProperties properties) {
		PolarisConfigClient.properties = properties;
	}

	/**
	 * Get config.
	 *
	 * @param namespace      namespace
	 * @param group          group
	 * @param configFileName configFileName
	 * @return config value
	 * @throws PolarisConfigException exception
	 */
	public Optional<String> getConfig(String namespace, String group, String configFileName) throws PolarisConfigException {
		return getConfig(namespace, group, configFileName, 3000);
	}

	/**
	 * Get config.
	 *
	 * @param namespace      namespace
	 * @param group          group
	 * @param configFileName configFileName
	 * @param timeoutMs      read timeout
	 * @return config value
	 * @throws PolarisConfigException exception
	 */
	public Optional<String> getConfig(String namespace, String group, String configFileName, int timeoutMs) throws PolarisConfigException {
		try {

			if (StringUtils.isBlank(namespace) || StringUtils.isBlank(group) || StringUtils.isBlank(configFileName)) {
				throw new PolarisConfigException("get polaris config request param namespace | group | configFileName must not be null and blank .");
			}

			LocalConfigStorage.ConfigFileKey fileKey = LocalConfigStorage.ConfigFileKey.build(namespace, group, configFileName);

			LocalConfigStorage.ConfigFileValue fileValue = LocalConfigStorage.get(fileKey);

			if(fileValue != null) {
				return Optional.of(fileValue.getContent());
			} else {
				// get config from remote server
				PolarisConfigClient.GetConfigFileResponse response = (PolarisConfigClient.GetConfigFileResponse) PolarisConfigClient.PolarisServerRequests.GET_CONFIG_FILE
						.execute(new PolarisConfigClient.GetConfigFileRequest(
								namespace, group, configFileName, 0), new PolarisConfigRequest.RequestOptions().readTimeout(timeoutMs));

				if(response != null && response.getCode() == Request.RESPONSE_OK) {
					ConfigFile configFile = response.getConfigFile();
					if(configFile != null) {
						fileValue = LocalConfigStorage.ConfigFileValue.build(configFile.getContent(), Integer.parseInt(configFile.getVersion()), configFile.getMd5());
						// save local-storage-cache
						LocalConfigStorage.put(fileKey, fileValue);

						// return config content
						return Optional.of(configFile.getContent());
					}
				}
			}
		} catch (PolarisConfigException e) {
			throw e;
		} catch (Exception e) {
			throw new PolarisConfigException("get polaris config failed", e);
		}
		// Default response
		return Optional.empty();
	}

	/**
	 * Add a listener to the configuration, after the server modified the configuration, the client will use the
	 * incoming listener callback. Recommended asynchronous processing, the application can implement the getExecutor
	 * method in the ManagerListener, provide a thread pool of execution. If not provided, use the main thread callback, May
	 * block other configurations or be blocked by other configurations.
	 *
	 * @param namespace      namespace
	 * @param group          group
	 * @param configFileName configFileName
	 * @param listener       listener
	 * @throws PolarisConfigException exception
	 */
	public void addListener(String namespace, String group, String configFileName, PolarisConfigChangeListener listener) throws PolarisConfigException {

		if (StringUtils.isBlank(namespace) || StringUtils.isBlank(group) || StringUtils.isBlank(configFileName)) {
			throw new PolarisConfigException("get polaris config request param namespace | group | configFileName must not be null and blank .");
		}

		LocalConfigStorage.ConfigFileKey fileKey = LocalConfigStorage.ConfigFileKey.build(namespace, group, configFileName);

		// submit long-pull task for listener
		listenerWorker.addListener(fileKey, listener);

	}

	/**
	 * Remove config listener
	 *
	 * @param namespace      namespace
	 * @param group          group
	 * @param configFileName configFileName
	 * @param listener       listener
	 * @throws PolarisConfigException exception
	 */
	public void removeListener(String namespace, String group, String configFileName, PolarisConfigChangeListener listener) throws PolarisConfigException {

		if (StringUtils.isBlank(namespace) || StringUtils.isBlank(group) || StringUtils.isBlank(configFileName)) {
			throw new PolarisConfigException("get polaris config request param namespace | group | configFileName must not be null and blank .");
		}

		LocalConfigStorage.ConfigFileKey fileKey = LocalConfigStorage.ConfigFileKey.build(namespace, group, configFileName);

		// submit long-pull task for listener
		listenerWorker.removeListener(fileKey, listener);
	}

	/**
	 * Get config and register Listener.
	 *
	 * @param namespace      namespace
	 * @param group          group
	 * @param configFileName configFileName
	 * @param timeoutMs      read timeout
	 * @param listener       {@link PolarisConfigChangeListener}
	 * @return config value
	 * @throws PolarisConfigException exception
	 */
	public Optional<String> getConfigAndRegisterListener(String namespace, String group, String configFileName, int timeoutMs, PolarisConfigChangeListener listener) throws PolarisConfigException {

		try {
			Optional<String> opt = getConfig(namespace, group, configFileName, timeoutMs);
			this.addListener(namespace, group, configFileName, listener);
			return opt;
		} catch (PolarisConfigException e) {
			throw e;
		} catch (Exception e) {
			throw new PolarisConfigException("get polaris config & add listener execute failed", e);
		}
	}

	// ~~

	/**
	 * Defined All Polaris Server Request(s).
	 */
	enum PolarisServerRequests implements PolarisConfigRequest<Request, Response> {

		/**
		 * Get a single configuration file content.
		 */
		GET_CONFIG_FILE("/config/v1/GetConfigFile") {
			/**
			 * Execute Request
			 *
			 * @param request request instance
			 * @return request execute response .
			 * @throws Exception maybe throw exception
			 */
			@Override
			public GetConfigFileResponse execute(Request request, RequestOptions options) throws Exception {
				try {
					if (request instanceof GetConfigFileRequest) {
						GetConfigFileRequest configFileRequest = (GetConfigFileRequest) request;

						Map<String, Object> params = new HashMap<>();
						params.put("namespace", configFileRequest.getNamespace());
						params.put("group", configFileRequest.getGroup());
						params.put("fileName", configFileRequest.getFileName());
						params.put("version", configFileRequest.getVersion());

						int readTimeout = properties.readTimeout();
						if(options != null && options.readTimeout() > 0) {
							readTimeout = options.readTimeout();
						}

						SimpleHttpRequest simpleHttpRequest = SimpleHttpRequest.get(GET_CONFIG_FILE.uri(), params, true)
								.header(ACCESS_TOKEN_HEADER, properties.token()).trustAllCerts().trustAllHosts()
								.connectTimeout(properties.connectTimeout())
								.readTimeout(readTimeout);

						int code = simpleHttpRequest.code();
						if (HTTP_OK == code) {
							String body = simpleHttpRequest.body(CHARSET_UTF8);
							return JacksonUtils.json2JavaBean(body, GetConfigFileResponse.class);
						}
						else {
							LOGGER.warn("[Polaris-Config] invalid get config file response http-code : {}", code);
						}
					}
					else {
						LOGGER.warn("[Polaris-Config] invalid get config file request class type.");
					}
				}
				catch (Exception e) {
					LOGGER.warn("[Polaris-Config] Get config file request execute failed , err-msg : {}", e.getMessage());
				}
				// DEFAULT RETURN NULL
				return null;
			}
		},

		/**
		 * Watch Config Change Request .
		 */
		WATCH_CONFIG_FILE("/config/v1/WatchConfigFile") {
			/**
			 * Execute Request
			 *
			 * @param request request instance
			 * @return request execute response .
			 * @throws Exception maybe throw exception
			 */
			@Override
			public WatchConfigFileResponse execute(Request request, RequestOptions options) throws Exception {
				try {
					if (request instanceof WatchConfigFileRequest) {
						WatchConfigFileRequest configFileRequest = (WatchConfigFileRequest) request;

						int readTimeout = LONG_PULL_REQUEST_TIMEOUT;
						if(options != null && options.readTimeout() > SERVER_DEFAULT_LONG_PULL_REQUEST_TIMEOUT) {
							readTimeout = options.readTimeout();
						}

						SimpleHttpRequest simpleHttpRequest = SimpleHttpRequest.post(WATCH_CONFIG_FILE.uri())
								.header(ACCESS_TOKEN_HEADER, properties.token())
								.contentType(CONTENT_TYPE_JSON, CHARSET_UTF8)
								.trustAllCerts().trustAllHosts()
								.connectTimeout(properties.connectTimeout())
								.readTimeout(readTimeout)
								.send(JacksonUtils.serialize2Json(configFileRequest));

						int code = simpleHttpRequest.code();

						if (HTTP_OK == code) {
							String body = simpleHttpRequest.body(CHARSET_UTF8);
							return JacksonUtils.json2JavaBean(body, WatchConfigFileResponse.class);
						}
						else {
							LOGGER.warn("[Polaris-Config] invalid watch config file response http-code : {}", code);
						}
					}
					else {
						LOGGER.warn("[Polaris-Config] invalid watch config file request class type.");
					}
				}
				catch (Exception e) {
					LOGGER.warn("[Polaris-Config] Get config file request execute failed , err-msg : {}", e.getMessage());
				}
				// DEFAULT RETURN NULL
				return null;
			}
		},


		GET_CONFIG_FILE_BY_LONG_PULLING("CONSTANTS_LONG_PULLING") {
			/**
			 * Execute Request
			 *
			 * @param request request instance
			 * @param options request option settings
			 * @return request execute response .
			 * @throws Exception maybe throw exception
			 */
			@Override public GetConfigFileResponse execute(Request request, RequestOptions options) throws Exception {
				try {
					if (request instanceof GetConfigFileRequest) {
						GetConfigFileRequest configFileRequest = (GetConfigFileRequest) request;

						LocalConfigStorage.ConfigFileKey fileKey = LocalConfigStorage.ConfigFileKey.build(configFileRequest.getNamespace(),
							configFileRequest.getGroup(), configFileRequest.getFileName());

						LocalConfigStorage.ConfigFileValue localConfigFile = LocalConfigStorage.get(fileKey);
						if (localConfigFile != null) {
							configFileRequest.setVersion(localConfigFile.getVersion());
						}

						WatchConfigFileRequest watchConfigFileRequest = new WatchConfigFileRequest();
						watchConfigFileRequest.addWatchFile(configFileRequest.getNamespace(), configFileRequest.getGroup(),
							configFileRequest.getFileName(), configFileRequest.getVersion());

						try{
							WatchConfigFileResponse watchConfigFileResponse = (WatchConfigFileResponse) WATCH_CONFIG_FILE.execute(watchConfigFileRequest, options);

							if (watchConfigFileResponse != null) {

								// Code: 200000 , ok
								if (watchConfigFileResponse.getCode() == Request.RESPONSE_OK) {
									// Remote configuration update, pull and update local configuration
									int remoteConfigVersion = Integer.parseInt(watchConfigFileResponse.getConfigFile().getVersion());
									if (remoteConfigVersion > configFileRequest.getVersion()) {
										configFileRequest.setVersion(remoteConfigVersion);
										GetConfigFileResponse configFileResponse = (GetConfigFileResponse) GET_CONFIG_FILE.execute(configFileRequest);
										// check response code
										if (configFileResponse != null && configFileResponse.getCode() == Request.RESPONSE_OK) {
											ConfigFile configFile = configFileResponse.getConfigFile();
											if(configFile != null) {
												LocalConfigStorage.ConfigFileValue newConfigValue = LocalConfigStorage.ConfigFileValue.build(configFile.getContent(),
													Integer.parseInt(configFile.getVersion()), configFile.getMd5());
												// save
												LocalConfigStorage.put(fileKey, newConfigValue);

												// return new config
												return configFileResponse;
											}
										}
									}
								}

								// Code: 200001 , no_change
								if (watchConfigFileResponse.getCode() == Request.RESPONSE_NO_CHANGE) {
									LOGGER.debug("[Polaris-Config] namespace: {}, group: {}, file: {}, version: {} is up to date !", configFileRequest.getNamespace(),
										configFileRequest.getGroup(), configFileRequest.getFileName(), configFileRequest.getVersion());
								}
							}
						} catch (Exception e){
							LOGGER.warn("[Polaris-Config] long-pulling request execute failed, ignore ~ ");
						}
					}
					else {
						LOGGER.warn("[Polaris-Config] invalid get config file request class type.");
					}
				}
				catch (Exception e) {
					LOGGER.warn("[Polaris-Config] Get config file request execute failed , err-msg : {}", e.getMessage());
				}
				// DEFAULT RETURN NULL
				return null;
			}
		}

		;

		/**
		 * Request URL.
		 */
		private final String uri;

		PolarisServerRequests(String uri) {
			this.uri = uri;
		}

		public String uri() {
			return properties.address().concat(uri);
		}
	}

	/**
	 * Target Config File Bean.
	 */
	static class ConfigFile {

		/**
		 * Config file namespace.
		 */
		private String namespace;

		/**
		 * Config file group.
		 */
		private String group;

		/**
		 * Config file name.
		 */
		private String fileName;

		/**
		 * Config file content.
		 */
		private String content;

		/**
		 * Config file upgrade version.
		 */
		private String version;

		/**
		 * Config file content md5.
		 */
		private String md5;

		public String getNamespace() {
			return namespace;
		}

		public void setNamespace(String namespace) {
			this.namespace = namespace;
		}

		public String getGroup() {
			return group;
		}

		public void setGroup(String group) {
			this.group = group;
		}

		public String getFileName() {
			return fileName;
		}

		public void setFileName(String fileName) {
			this.fileName = fileName;
		}

		public String getContent() {
			return content;
		}

		public void setContent(String content) {
			this.content = content;
		}

		public String getVersion() {
			return version;
		}

		public void setVersion(String version) {
			this.version = version;
		}

		public String getMd5() {
			return md5;
		}

		public void setMd5(String md5) {
			this.md5 = md5;
		}
	}

	/**
	 * Get Config File Request Info.
	 */
	static class GetConfigFileRequest extends Request {

		private String namespace;

		private String group;

		private String fileName;

		private int version;

		public GetConfigFileRequest(String namespace, String group, String fileName, int version) {
			this.namespace = namespace;
			this.group = group;
			this.fileName = fileName;
			this.version = version;
		}

		public String getNamespace() {
			return namespace;
		}

		public void setNamespace(String namespace) {
			this.namespace = namespace;
		}

		public String getGroup() {
			return group;
		}

		public void setGroup(String group) {
			this.group = group;
		}

		public String getFileName() {
			return fileName;
		}

		public void setFileName(String fileName) {
			this.fileName = fileName;
		}

		public int getVersion() {
			return version;
		}

		public void setVersion(int version) {
			this.version = version;
		}
	}

	/**
	 * Get Config File Response Scheme.
	 */
	static class GetConfigFileResponse extends Response {

		/**
		 * Config File Instance of {@link ConfigFile}.
		 */
		private ConfigFile configFile;

		public ConfigFile getConfigFile() {
			return configFile;
		}

		public void setConfigFile(ConfigFile configFile) {
			this.configFile = configFile;
		}
	}

	/**
	 * Watch Config File Request.
	 */
	static class WatchConfigFileRequest extends Request {

		/**
		 * Defined need watch change config metadata .
		 */
		private List<WatchFile> watchFiles = new ArrayList<>();

		public List<WatchFile> getWatchFiles() {
			return watchFiles;
		}

		public void setWatchFiles(List<WatchFile> watchFiles) {
			this.watchFiles = watchFiles;
		}

		public void addWatchFile(String namespace, String group, String fileName, int version) {
			WatchFile file = new WatchFile();
			file.setNamespace(namespace);
			file.setGroup(group);
			file.setFileName(fileName);
			file.setVersion(String.valueOf(version));
			watchFiles.add(file);
		}

		private static class WatchFile {

			/**
			 * Config file namespace.
			 */
			private String namespace;

			/**
			 * Config file group.
			 */
			private String group;

			/**
			 * Config file name.
			 */
			private String fileName;

			/**
			 * Config file version.
			 */
			private String version;

			public String getNamespace() {
				return namespace;
			}

			public void setNamespace(String namespace) {
				this.namespace = namespace;
			}

			public String getGroup() {
				return group;
			}

			public void setGroup(String group) {
				this.group = group;
			}

			public String getFileName() {
				return fileName;
			}

			public void setFileName(String fileName) {
				this.fileName = fileName;
			}

			public String getVersion() {
				return version;
			}

			public void setVersion(String version) {
				this.version = version;
			}
		}
	}

	/**
	 * Watch Config File Response .
	 */
	static class WatchConfigFileResponse extends Response {

		/**
		 * Target Config File Bean.
		 */
		private ConfigFile configFile;

		public ConfigFile getConfigFile() {
			return configFile;
		}

		public void setConfigFile(ConfigFile configFile) {
			this.configFile = configFile;
		}
	}

	/**
	 * Base Request Interface Defined .
	 */
	static class Request {

		/**
		 * Remote server access token header .
		 */
		public static final String ACCESS_TOKEN_HEADER = "X-Polaris-Token";

		/**
		 * Default Long Pull read timeout is 40000 ms, remote server holding timeout 300000 ms .
		 * <p>
		 * Reference Polaris Doc: <a href="https://polarismesh.cn/zh/doc/%E5%8F%82%E8%80%83%E6%96%87%E6%A1%A3/%E6%8E%A5%E5%8F%A3%E6%96%87%E6%A1%A3/%E9%85%8D%E7%BD%AE%E7%AE%A1%E7%90%86.html#%E9%85%8D%E7%BD%AE%E7%AE%A1%E7%90%86">监听配置</a>
		 * </p>
		 */
		public static final int LONG_PULL_REQUEST_TIMEOUT = 40000;

		/**
		 * Default Server Long Pulling Request Hold Timeout , default value : 30000 ms.
		 */
		public static final int SERVER_DEFAULT_LONG_PULL_REQUEST_TIMEOUT = 30000;

		/**
		 * Request Success Code Defined .
		 */
		public static final int RESPONSE_OK = 200000;

		/**
		 * Remote Config File NO_CHANGE Response Code.
		 */
		public static final int RESPONSE_NO_CHANGE = 200001;
	}

	/**
	 * Base Request Response Defined.
	 */
	static class Response {

		/**
		 * Response Code.
		 */
		protected int code;

		/**
		 * Response Message Info.
		 */
		protected String info;

		protected int getCode() {
			return code;
		}

		protected void setCode(int code) {
			this.code = code;
		}

		protected String getInfo() {
			return info;
		}

		protected void setInfo(String info) {
			this.info = info;
		}
	}

	/**
	 * Polaris Config Request Executor Interface .
	 *
	 * @param <REQ> sub-instance of {@link Request}
	 * @param <RES> sub-instance of {@link Response}
	 */
	private interface PolarisConfigRequest<REQ extends Request, RES extends Response> {

		/**
		 * Request Options.
		 */
		class RequestOptions {
			private int readTimeout = 0;

			public int readTimeout() {
				return readTimeout;
			}

			public RequestOptions readTimeout(int readTimeout) {
				this.readTimeout = readTimeout;
				return this;
			}
		}

		/**
		 * Execute Request
		 *
		 * @param request request instance
		 * @param options request option settings
		 * @return request execute response .
		 * @throws Exception maybe throw exception
		 */
		RES execute(REQ request, RequestOptions options) throws Exception;

		/**
		 * Execute Request
		 *
		 * @param request request instance
		 * @return request execute response .
		 * @throws Exception maybe throw exception
		 */
		default RES execute(REQ request) throws Exception {
			return execute(request, null);
		}
	}

	/**
	 * Local Storage For Config.
	 */
	private static final class LocalConfigStorage {

		private static final int MAP_INITIAL_CAPACITY = 8;

		private static class ConfigFileKey {

			private final String namespace;
			private final String group;
			private final String fileName;

			/**
			 * Build File Key.
			 * @param namespace config file namespace
			 * @param group config file group
			 * @param fileName config file name
			 * @return instance of {@link ConfigFileKey}
			 */
			public static ConfigFileKey build(String namespace, String group, String fileName) {
				return new ConfigFileKey(namespace, group, fileName);
			}

			ConfigFileKey(String namespace, String group, String fileName) {
				this.namespace = namespace;
				this.group = group;
				this.fileName = fileName;
			}

			public String getNamespace() {
				return namespace;
			}

			public String getGroup() {
				return group;
			}

			public String getFileName() {
				return fileName;
			}

			@Override
			public boolean equals(Object o) {
				if (this == o) {
					return true;
				}
				if (o == null || getClass() != o.getClass()) {
					return false;
				}
				ConfigFileKey configFileKey = (ConfigFileKey) o;
				return namespace.equals(configFileKey.namespace) && group.equals(configFileKey.group) && fileName.equals(configFileKey.fileName);
			}

			@Override
			public int hashCode() {
				return Objects.hash(namespace, group, fileName);
			}
		}

		private static class ConfigFileValue {
			private final String content;
			private final int version;
			private final String md5;

			ConfigFileValue(String content, int version, String md5) {
				this.content = content;
				this.version = version;
				this.md5 = md5;
			}

			/**
			 * Build Config File Value.
			 * @param content config content
			 * @param version config version
			 * @param md5 config content md5
			 * @return instance of {@link ConfigFileValue}
			 */
			public static ConfigFileValue build(String content, int version, String md5) {
				return new ConfigFileValue(content, version, md5);
			}

			public String getContent() {
				return content;
			}

			public int getVersion() {
				return version;
			}

			public String getMd5() {
				return md5;
			}
		}

		/**
		 * Config File Local Cache Storage.
		 */
		private static final Map<ConfigFileKey, ConfigFileValue> FILES = new ConcurrentHashMap<>(MAP_INITIAL_CAPACITY);

		/**
		 * Find local config from cache.
		 *
		 * @param namespace config file namespace
		 * @param group config file group
		 * @param fileName config file name
		 * @return Config file value holder instance.
		 */
		public static ConfigFileValue get(String namespace, String group, String fileName) {
			ConfigFileKey configFileKey = ConfigFileKey.build(namespace, group, fileName);
			return FILES.get(configFileKey);
		}

		/**
		 * Find local config from cache.
		 *
		 * @param configFileKey config file key
		 * @return Config file value holder instance.
		 */
		public static ConfigFileValue get(ConfigFileKey configFileKey) {
			return get(configFileKey.getNamespace(), configFileKey.getGroup(), configFileKey.getFileName());
		}

		/**
		 * Save Config Cache .
		 *
		 * @param configFileKey target config file key
		 * @param configFileValue target config file value
		 * @return origin {@link ConfigFileValue} of target config key.
		 */
		public static ConfigFileValue put(ConfigFileKey configFileKey, ConfigFileValue configFileValue) {
			ConfigFileValue originValue = FILES.get(configFileKey);
			FILES.put(configFileKey, configFileValue);
			return originValue;
		}
	}

	/**
	 * Config Change Long Pull Worker.
	 */
	private static class ConfigListenerWorker {

		/**
		 * Long-Pulling Task Scan-Executing Thread Pool .
		 */
		final ScheduledExecutorService executor;

		/**
		 * Long-Pulling Task Real-Execute Thread Pool .
		 */
		final ScheduledExecutorService executorService;

		/**
		 * Config-File Listeners CacheMap.
		 */
		private final AtomicReference<Map<LocalConfigStorage.ConfigFileKey, CacheData>> configsCache = new AtomicReference<>(new HashMap<>());

		private double currentLongingTaskCount = 0;

		/**
		 * Long-Pulling Task Batch Size.
		 */
		private final int preTaskListenerSize = 300;

		/**
		 * Long-Pulling Re-Execute Time When Current-Executing Failed.
		 */
		private static final int TASK_RETRY_TIME = 3000;


		private ConfigListenerWorker() {
			this.executor =
				new ScheduledThreadPoolExecutor(1, new DefaultThreadFactory("POLARIS-CONFIG-CLIENT-WORKER-"));

			this.executorService =
				new ScheduledThreadPoolExecutor(
					Runtime.getRuntime().availableProcessors(),
					new DefaultThreadFactory("POLARIS-CONFIG-CLIENT-LONG-PULLING-WORKER-"));

			// Registry Shutdown Hook
			addShutdownHook(() -> shutdownThreadPool(executor));
			addShutdownHook(() -> shutdownThreadPool(executorService));

			// startup executor
			this.executor.scheduleWithFixedDelay(
				this::checkRemoteConfigFileChange, 1L, 10L, TimeUnit.MILLISECONDS);
		}

		private void checkRemoteConfigFileChange() {
			int listenerSize = configsCache.get().size();
			int longingTaskCount = (int) Math.ceil(listenerSize / (preTaskListenerSize * 1.0));
			if (longingTaskCount > currentLongingTaskCount) {
				for (int i = (int) currentLongingTaskCount; i < longingTaskCount; i++) {
					executorService.execute(new LongPullingRunnable(i));
				}
				currentLongingTaskCount = longingTaskCount;
			}
		}

		/**
		 * Add config-file change listener.
		 *
		 * @param fileKey config file key
		 * @param listener instance of {@link PolarisConfigChangeListener}
		 */
		void addListener(LocalConfigStorage.ConfigFileKey fileKey, PolarisConfigChangeListener listener) {
			CacheData cacheData = configsCache.get().get(fileKey);
			if (cacheData == null) {
				synchronized (configsCache) {
					CacheData temp = new CacheData(fileKey, listener);
					int taskId = configsCache.get().size() / preTaskListenerSize;
					temp.setTaskId(taskId);
					Map<LocalConfigStorage.ConfigFileKey, CacheData> copy = new HashMap<>(configsCache.get());
					copy.put(fileKey, temp);
					configsCache.set(copy);
				}
			} else {
				cacheData.addListener(listener);
			}
		}

		/**
		 * Removed config-file listener.
		 * @param fileKey config file key
		 * @param listener instance of {@link PolarisConfigChangeListener}
		 */
		void removeListener(LocalConfigStorage.ConfigFileKey fileKey, PolarisConfigChangeListener listener) {
			Map<LocalConfigStorage.ConfigFileKey, CacheData> listenerDataMap = configsCache.get();
			if(listenerDataMap != null) {
				CacheData data = listenerDataMap.get(fileKey);
				if(data != null && listener != null) {
					data.removeListener(listener);
				}
			}
		}

		/**
		 * Long-Pulling Runnable Defined.
		 */
		class LongPullingRunnable implements Runnable {
			private final int taskId;

			LongPullingRunnable(int taskId) {
				this.taskId = taskId;
			}

			@Override
			public void run() {
				try {
					for (CacheData cacheData : configsCache.get().values()) {
						if (cacheData.getTaskId() == taskId) {
							LocalConfigStorage.ConfigFileKey fileKey = cacheData.getFileKey();
							LocalConfigStorage.ConfigFileValue fileValue = LocalConfigStorage.get(fileKey);
							if (fileValue != null) {
								GetConfigFileRequest configFileRequest = new GetConfigFileRequest(fileKey.getNamespace(), fileKey.getGroup(), fileKey.getFileName(), fileValue.getVersion());
								GetConfigFileResponse configFileResponse = (GetConfigFileResponse) GET_CONFIG_FILE_BY_LONG_PULLING.execute(configFileRequest);
								if (configFileResponse != null && configFileResponse.getConfigFile() != null) {
									cacheData.onResponse(configFileResponse);
								}
							}
						}
					}

					// execute
					executorService.execute(this);

				} catch (Throwable e) {
					LOGGER.error("[Polaris Config Client] long polling error .", e);
					executorService.schedule(this, TASK_RETRY_TIME, TimeUnit.MILLISECONDS);
				}
			}
		}

		/**
		 * Listener's Task Execute Metadata Instance .
		 */
		private static class CacheData {

			/**
			 * Config-File Key's Metadata.
			 */
			private final LocalConfigStorage.ConfigFileKey fileKey;

			/**
			 * Config=File All Listeners.
			 */
			private CopyOnWriteArrayList<PolarisConfigChangeListener> listeners = new CopyOnWriteArrayList<>();

			/**
			 * Config-File Cache Data Constructor.
			 * @param fileKey config file key.
			 * @param listener config change listener.
			 */
			public CacheData(LocalConfigStorage.ConfigFileKey fileKey, PolarisConfigChangeListener listener) {
				this.fileKey = fileKey;
				listeners.add(listener);
			}

			private int taskId;

			public int getTaskId() {
				return taskId;
			}

			public void setTaskId(int taskId) {
				this.taskId = taskId;
			}

			public LocalConfigStorage.ConfigFileKey getFileKey() {
				return fileKey;
			}

			/**
			 * Add Config File Change Listener.
			 * @param listener instance of {@link PolarisConfigChangeListener}
			 */
			public void addListener(PolarisConfigChangeListener listener) {
				this.listeners.add(listener);
			}

			/**
			 * Add Config File Change Listener.
			 * @param listener instance of {@link PolarisConfigChangeListener}
			 */
			public void removeListener(PolarisConfigChangeListener listener) {

				if (null == listener) {
					throw new IllegalArgumentException("listener is null");
				}
				if (listeners.remove(listener)) {
					LOGGER.info("[Polaris Config Listener] removed ok, namespace: {}, group: {}, fileName: {}, cnt: {}",
						fileKey.getNamespace(), fileKey.getGroup(), fileKey.getFileName(), listeners.size());
				}
			}

			/**
			 * Long-Pulling Executed Success Callback .
			 * @param response config-response body.
			 */
			void onResponse(GetConfigFileResponse response) {

				if(response != null) {
					for (PolarisConfigChangeListener changeListener : listeners) {
						Runnable job = () -> {
							try{
								ConfigFile configFile = response.getConfigFile();
								changeListener.receiveConfigInfo(configFile.getNamespace(), configFile.getGroup(), configFile.getFileName(), configFile.getContent());
							} catch (Throwable e) {
								LOGGER.error("[Polaris Config Listener] client side execute result happen-ed exception, ignore", e);
							}
						};

						final long startNotify = System.currentTimeMillis();
						try {
							if (null != changeListener.getExecutor()) {
								changeListener.getExecutor().execute(job);
							} else {
								job.run();
							}
						} catch (Throwable t) {
							LOGGER.error("[Polaris Config Listener] [notify-error] , listener={} throwable={}", changeListener, t.getCause());
						}
						final long finishNotify = System.currentTimeMillis();
						LOGGER.info("[Polaris Config Listener] [notify-listener] time cost={} ms in ClientWorker, listener={} ", (finishNotify - startNotify), changeListener);
					}
				}
			}
		}
	}

	/**
	 * Add JVM Shutdown Hook.
	 *
	 * @param runnable instance of thread {@link Runnable}
	 */
	private static void addShutdownHook(Runnable runnable) {
		Runtime.getRuntime().addShutdownHook(new Thread(runnable));
	}

	/**
	 * Shutdown Thread Pool.
	 * @param executor target thread pool execute.
	 */
	private static void shutdownThreadPool(ExecutorService executor) {
		// invoke shutdown
		executor.shutdown();
		int retry = 3;
		// retry shutdown again
		while(retry > 0) {
			--retry;

			try {
				if (executor.awaitTermination(1L, TimeUnit.SECONDS)) {
					return;
				}
			} catch (InterruptedException var4) {
				executor.shutdownNow();
				//noinspection ResultOfMethodCallIgnored
				Thread.interrupted();
			} catch (Throwable e) {
				LOGGER.error("ThreadPoolManager shutdown executor has error.", e);
			}
		}
		executor.shutdownNow();
	}
}
