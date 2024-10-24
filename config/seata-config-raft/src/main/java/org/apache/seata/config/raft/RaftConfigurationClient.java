/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.config.raft;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.ContentType;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.common.config.ConfigDataResponse;
import org.apache.seata.common.exception.AuthenticationFailedException;
import org.apache.seata.common.exception.ErrorCode;
import org.apache.seata.common.exception.NotSupportYetException;
import org.apache.seata.common.exception.RetryableException;
import org.apache.seata.common.exception.SeataRuntimeException;
import org.apache.seata.common.metadata.Metadata;
import org.apache.seata.common.metadata.MetadataResponse;
import org.apache.seata.common.metadata.Node;
import org.apache.seata.common.thread.NamedThreadFactory;
import org.apache.seata.common.util.CollectionUtils;
import org.apache.seata.common.util.HttpClientUtil;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.config.AbstractConfiguration;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationChangeEvent;
import org.apache.seata.config.ConfigurationChangeListener;
import org.apache.seata.config.ConfigurationChangeType;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.config.dto.ConfigurationInfoDto;
import org.apache.seata.config.dto.ConfigurationItem;
import org.apache.seata.config.store.ConfigStoreManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.seata.common.ConfigurationKeys.CONFIG_STORE_DATA_ID;
import static org.apache.seata.common.ConfigurationKeys.CONFIG_STORE_NAMESPACE;
import static org.apache.seata.common.Constants.DEFAULT_STORE_DATA_ID;
import static org.apache.seata.common.Constants.DEFAULT_STORE_NAMESPACE;
import static org.apache.seata.common.Constants.RAFT_CONFIG_GROUP;
import static org.apache.seata.common.DefaultValues.DEFAULT_SEATA_GROUP;

/**
 * The type Raft configuration of client.
 *
 */
public class RaftConfigurationClient extends AbstractConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(RaftConfigurationClient.class);
    private static final String CONFIG_TYPE = "raft";
    private static final String SERVER_ADDR_KEY = "serverAddr";
    private static final String RAFT_GROUP = RAFT_CONFIG_GROUP;
    private static final String RAFT_CLUSTER = DEFAULT_SEATA_GROUP;
    private static final String CONFIG_NAMESPACE;
    private static final String CONFIG_DATA_ID;
    private static final String HTTP_PREFIX = "http://";
    private static final String USERNAME_KEY = "username";
    private static final String PASSWORD_KEY = "password";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String TOKEN_VALID_TIME_MS_KEY = "tokenValidityInMilliseconds";
    private static volatile RaftConfigurationClient instance;
    private static final Configuration FILE_CONFIG = ConfigurationFactory.CURRENT_FILE_INSTANCE;
    private static final String USERNAME;
    private static final String PASSWORD;
    private static final long TOKEN_EXPIRE_TIME_IN_MILLISECONDS;
    private static volatile long tokenTimeStamp = -1;
    private static volatile String jwtToken;
    private static final String IP_PORT_SPLIT_CHAR = ":";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Map<String, List<InetSocketAddress>> INIT_ADDRESSES = new HashMap<>();
    private static final Metadata METADATA = new Metadata();
    private static volatile ThreadPoolExecutor REFRESH_METADATA_EXECUTOR;
    private static volatile ThreadPoolExecutor REFRESH_CONFIG_EXECUTOR;
    private static final AtomicBoolean CLOSED = new AtomicBoolean(false);
    private static final AtomicBoolean CONFIG_CLOSED = new AtomicBoolean(false);
    private static volatile Properties seataConfig = new Properties();
    private static final AtomicLong CONFIG_VERSION = new AtomicLong(0);
    private static final int MAP_INITIAL_CAPACITY = 8;
    private static final ConcurrentMap<String, ConcurrentMap<ConfigurationChangeListener, ConfigStoreListener>> CONFIG_LISTENERS_MAP
            = new ConcurrentHashMap<>(MAP_INITIAL_CAPACITY);

    private static ConfigStoreListener CONFIG_LISTENER;
    static {
        USERNAME = FILE_CONFIG.getConfig(getRaftUsernameKey());
        PASSWORD = FILE_CONFIG.getConfig(getRaftPasswordKey());
        TOKEN_EXPIRE_TIME_IN_MILLISECONDS = FILE_CONFIG.getLong(getTokenExpireTimeInMillisecondsKey(), 29 * 60 * 1000L);
        CONFIG_NAMESPACE = FILE_CONFIG.getConfig(CONFIG_STORE_NAMESPACE, DEFAULT_STORE_NAMESPACE);
        CONFIG_DATA_ID = FILE_CONFIG.getConfig(CONFIG_STORE_DATA_ID, DEFAULT_STORE_DATA_ID);
    }

    public static RaftConfigurationClient getInstance() {
        if (instance == null) {
            synchronized (RaftConfigurationClient.class) {
                if (instance == null) {
                    instance = new RaftConfigurationClient();
                }
            }
        }
        return instance;
    }

    private RaftConfigurationClient() {
        initClusterMetaData();
        initClientConfig();
    }

    private static void initClientConfig() {
        try {
            Map<String, Object> configMap = acquireClusterConfigData(RAFT_CLUSTER, RAFT_GROUP, CONFIG_NAMESPACE, CONFIG_DATA_ID);
            if (configMap != null) {
                seataConfig.putAll(configMap);
            }
            CONFIG_LISTENER = new ConfigStoreListener(CONFIG_NAMESPACE, null);
            startQueryConfigData();
        } catch (RetryableException e) {
            LOGGER.error("init config properties error:{}", e.getMessage(), e);
        }

    }
    private static String queryHttpAddress(String clusterName, String group) {
        List<Node> nodeList = METADATA.getNodes(clusterName, group);
        List<String> addressList = null;
        Stream<InetSocketAddress> stream = null;
        if (CollectionUtils.isNotEmpty(nodeList)) {
            addressList =
                    nodeList.stream().map(node -> node.getControl().createAddress()).collect(Collectors.toList());
        } else {
            stream = INIT_ADDRESSES.get(clusterName).stream();
        }
        if (addressList != null) {
            return addressList.get(ThreadLocalRandom.current().nextInt(addressList.size()));
        } else {
            Map<String, Node> map = new HashMap<>();
            if (CollectionUtils.isNotEmpty(nodeList)) {
                for (Node node : nodeList) {
                    map.put(new InetSocketAddress(node.getTransaction().getHost(), node.getTransaction().getPort()).getAddress().getHostAddress()
                            + IP_PORT_SPLIT_CHAR + node.getTransaction().getPort(), node);
                }
            }
            addressList = stream.map(inetSocketAddress -> {
                String host = inetSocketAddress.getAddress().getHostAddress();
                Node node = map.get(host + IP_PORT_SPLIT_CHAR + inetSocketAddress.getPort());
                return host + IP_PORT_SPLIT_CHAR
                        + (node != null ? node.getControl().getPort() : inetSocketAddress.getPort());
            }).collect(Collectors.toList());
            return addressList.get(ThreadLocalRandom.current().nextInt(addressList.size()));
        }
    }
    private static void acquireClusterMetaData(String clusterName, String group) throws RetryableException {
        String tcAddress = queryHttpAddress(clusterName, group);
        Map<String, String> header = new HashMap<>();
        header.put(HTTP.CONTENT_TYPE, ContentType.APPLICATION_FORM_URLENCODED.getMimeType());
        if (isTokenExpired()) {
            refreshToken(tcAddress);
        }
        if (StringUtils.isNotBlank(jwtToken)) {
            header.put(AUTHORIZATION_HEADER, jwtToken);
        }
        if (StringUtils.isNotBlank(tcAddress)) {
            Map<String, String> param = new HashMap<>();
            // param.put("group", group);
            String response = null;
            try (CloseableHttpResponse httpResponse =
                         HttpClientUtil.doGet(HTTP_PREFIX + tcAddress + "/metadata/v1/config/cluster", param, header, 1000)) {
                if (httpResponse != null) {
                    if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                        response = EntityUtils.toString(httpResponse.getEntity(), StandardCharsets.UTF_8);
                    } else if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
                        if (StringUtils.isNotBlank(USERNAME) && StringUtils.isNotBlank(PASSWORD)) {
                            throw new RetryableException("Authentication failed!");
                        } else {
                            throw new AuthenticationFailedException("Authentication failed! you should configure the correct username and password.");
                        }
                    }
                }
                MetadataResponse metadataResponse;
                if (StringUtils.isNotBlank(response)) {
                    try {
                        metadataResponse = OBJECT_MAPPER.readValue(response, MetadataResponse.class);
                        METADATA.refreshMetadata(clusterName, metadataResponse);
                    } catch (JsonProcessingException e) {
                        LOGGER.error("acquireClusterMetaData:{}", e.getMessage(), e);
                    }
                }
            } catch (IOException e) {
                throw new RetryableException(e.getMessage(), e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> acquireClusterConfigData(String clusterName, String group, String configNamespace, String configDataId) throws RetryableException {
        String tcAddress = queryHttpAddress(clusterName, group);
        Map<String, String> header = new HashMap<>();
        header.put(HTTP.CONTENT_TYPE, ContentType.APPLICATION_FORM_URLENCODED.getMimeType());
        if (isTokenExpired()) {
            refreshToken(tcAddress);
        }
        if (StringUtils.isNotBlank(jwtToken)) {
            header.put(AUTHORIZATION_HEADER, jwtToken);
        }
        if (StringUtils.isNotBlank(tcAddress)) {
            Map<String, String> param = new HashMap<>();
            param.put("namespace", configNamespace);
            param.put("dataId", configDataId);
            String response = null;
            try (CloseableHttpResponse httpResponse =
                         HttpClientUtil.doGet(HTTP_PREFIX + tcAddress + "/metadata/v1/config/getAll", param, header, 1000)) {
                if (httpResponse != null) {
                    if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                        response = EntityUtils.toString(httpResponse.getEntity(), StandardCharsets.UTF_8);
                    } else if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
                        if (StringUtils.isNotBlank(USERNAME) && StringUtils.isNotBlank(PASSWORD)) {
                            throw new RetryableException("Authentication failed!");
                        } else {
                            throw new AuthenticationFailedException("Authentication failed! you should configure the correct username and password.");
                        }
                    }
                }

                ConfigDataResponse<ConfigurationInfoDto> configDataResponse;
                if (StringUtils.isNotBlank(response)) {
                    try {
                        configDataResponse = OBJECT_MAPPER.readValue(response, new TypeReference<ConfigDataResponse<ConfigurationInfoDto>>() {
                        });
                        if (configDataResponse.getSuccess()) {
                            ConfigurationInfoDto configurationInfoDto = configDataResponse.getResult();
                            Map<String, ConfigurationItem> configItemMap = configurationInfoDto.getConfig();
                            Map<String, Object> configMap = configItemMap.entrySet().stream()
                                    .collect(Collectors.toMap(
                                            Map.Entry::getKey, entry -> entry.getValue().getValue()));
                            Long version = configurationInfoDto.getVersion() == null ? -1 : configurationInfoDto.getVersion();
                            Long currentVersion = CONFIG_VERSION.get();
                            if (version < currentVersion) {
                                LOGGER.info("The configuration version: {} of the server is lower than the current configuration: {} , it may be expired configuration.", version, CONFIG_VERSION.get());
                                throw new RetryableException("Expired configuration!");
                            } else {
                                CONFIG_VERSION.set(version);
                                return configMap;
                            }
                        } else {
                            throw new RetryableException(configDataResponse.getErrMsg());
                        }
                    } catch (JsonProcessingException e) {
                        LOGGER.error("acquireClusterConfigData:{}", e.getMessage(), e);
                    }
                }
            } catch (IOException e) {
                throw new RetryableException(e.getMessage(), e);
            }
        }
        return null;
    }

    protected static void startQueryMetadata() {
        if (REFRESH_METADATA_EXECUTOR == null) {
            synchronized (INIT_ADDRESSES) {
                if (REFRESH_METADATA_EXECUTOR == null) {
                    REFRESH_METADATA_EXECUTOR = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                            new LinkedBlockingQueue<>(), new NamedThreadFactory("refreshMetadata", 1, true));
                    REFRESH_METADATA_EXECUTOR.execute(() -> {
                        long metadataMaxAgeMs = FILE_CONFIG.getLong(ConfigurationKeys.CLIENT_METADATA_MAX_AGE_MS, 30000L);
                        long currentTime = System.currentTimeMillis();
                        while (!CLOSED.get()) {
                            try {
                                // Forced refresh of metadata information after set age
                                boolean fetch = System.currentTimeMillis() - currentTime > metadataMaxAgeMs;
                                String clusterName = RAFT_CLUSTER;
                                if (!fetch) {
                                    fetch = watch();
                                }
                                // Cluster changes or reaches timeout refresh time
                                if (fetch) {
                                    for (String group : METADATA.groups(clusterName)) {
                                        try {
                                            acquireClusterMetaData(clusterName, group);
                                        } catch (Exception e) {
                                            // prevents an exception from being thrown that causes the thread to break
                                            if (e instanceof RetryableException) {
                                                throw e;
                                            } else {
                                                LOGGER.error("failed to get the leader address,error: {}", e.getMessage(), e);
                                            }
                                        }
                                    }
                                    currentTime = System.currentTimeMillis();
                                    if (LOGGER.isDebugEnabled()) {
                                        LOGGER.debug("refresh seata cluster metadata time: {}", currentTime);
                                    }
                                }
                            } catch (RetryableException e) {
                                LOGGER.error("startQueryMetadata:{}", e.getMessage(), e);
                                try {
                                    TimeUnit.MILLISECONDS.sleep(1000);
                                } catch (InterruptedException ignored) {
                                }
                            }
                        }
                    });
                    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                        if (CLOSED.compareAndSet(false, true)) {
                            REFRESH_METADATA_EXECUTOR.shutdown();
                        }
                    }));
                }
            }
        }
    }

    protected static void startQueryConfigData() {
        if (REFRESH_CONFIG_EXECUTOR == null) {
            synchronized (RaftConfigurationClient.class) {
                if (REFRESH_CONFIG_EXECUTOR == null) {
                    REFRESH_CONFIG_EXECUTOR = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                            new LinkedBlockingQueue<>(), new NamedThreadFactory("refreshConfig", 1, true));
                    REFRESH_CONFIG_EXECUTOR.execute(() -> {
                        long metadataMaxAgeMs = FILE_CONFIG.getLong(ConfigurationKeys.CLIENT_METADATA_MAX_AGE_MS, 30000L);
                        long currentTime = System.currentTimeMillis();
                        while (!CONFIG_CLOSED.get()) {
                            try {
                                // Forced refresh of metadata information after set age
                                boolean fetch = System.currentTimeMillis() - currentTime > metadataMaxAgeMs;
                                if (!fetch) {
                                    fetch = configWatch();
                                }
                                // Cluster config changes or reaches timeout refresh time
                                if (fetch) {
                                    try {
                                        Map<String, Object> configMap = acquireClusterConfigData(RAFT_CLUSTER, RAFT_GROUP, CONFIG_NAMESPACE, CONFIG_DATA_ID);
                                        if (CollectionUtils.isNotEmpty(configMap)) {
                                            notifyConfigMayChange(configMap);
                                        }
                                    } catch (Exception e) {
                                        // prevents an exception from being thrown that causes the thread to break
                                        if (e instanceof RetryableException) {
                                            throw e;
                                        } else {
                                            LOGGER.error("failed to get the config ,error: {}", e.getMessage(), e);
                                        }
                                    }

                                    currentTime = System.currentTimeMillis();
                                    if (LOGGER.isDebugEnabled()) {
                                        LOGGER.debug("refresh seata cluster config time: {}", currentTime);
                                    }
                                }
                            } catch (RetryableException e) {
                                LOGGER.error("startQueryConfigData:{}", e.getMessage(), e);
                                try {
                                    TimeUnit.MILLISECONDS.sleep(1000);
                                } catch (InterruptedException ignored) {
                                }
                            }
                        }
                    });
                    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                        if (CONFIG_CLOSED.compareAndSet(false, true)) {
                            REFRESH_CONFIG_EXECUTOR.shutdown();
                        }
                    }));
                }
            }
        }
    }
    private static boolean watch() throws RetryableException {
        Map<String, String> header = new HashMap<>();
        header.put(HTTP.CONTENT_TYPE, ContentType.APPLICATION_FORM_URLENCODED.getMimeType());
        Map<String, String> param = new HashMap<>();
        String clusterName = RAFT_CLUSTER;
        Map<String, Long> groupTerms = METADATA.getClusterTerm(clusterName);
        groupTerms.forEach((k, v) -> param.put(k, String.valueOf(v)));
        for (String group : groupTerms.keySet()) {
            String tcAddress = queryHttpAddress(clusterName, group);
            if (isTokenExpired()) {
                refreshToken(tcAddress);
            }
            if (StringUtils.isNotBlank(jwtToken)) {
                header.put(AUTHORIZATION_HEADER, jwtToken);
            }
            try (CloseableHttpResponse response =
                         HttpClientUtil.doPost(HTTP_PREFIX + tcAddress + "/metadata/v1/watch", param, header, 30000)) {
                if (response != null) {
                    StatusLine statusLine = response.getStatusLine();
                    if (statusLine != null && statusLine.getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
                        if (StringUtils.isNotBlank(USERNAME) && StringUtils.isNotBlank(PASSWORD)) {
                            throw new RetryableException("Authentication failed!");
                        } else {
                            throw new AuthenticationFailedException("Authentication failed! you should configure the correct username and password.");
                        }
                    }
                    return statusLine != null && statusLine.getStatusCode() == HttpStatus.SC_OK;
                }
            } catch (IOException e) {
                LOGGER.error("watch cluster node: {}, fail: {}", tcAddress, e.getMessage(), e);
                throw new RetryableException(e.getMessage(), e);
            }
            break;
        }
        return false;
    }

    private static boolean configWatch() throws RetryableException {
        Map<String, String> header = new HashMap<>();
        header.put(HTTP.CONTENT_TYPE, ContentType.APPLICATION_FORM_URLENCODED.getMimeType());
        String tcAddress = queryHttpAddress(RAFT_CLUSTER, RAFT_GROUP);
        Map<String, String> param = new HashMap<>();
        param.put("namespace", CONFIG_NAMESPACE);
        param.put("dataId", CONFIG_DATA_ID);
        param.put("version", CONFIG_VERSION.toString());
        if (isTokenExpired()) {
            refreshToken(tcAddress);
        }
        if (StringUtils.isNotBlank(jwtToken)) {
            header.put(AUTHORIZATION_HEADER, jwtToken);
        }
        try (CloseableHttpResponse response =
                     HttpClientUtil.doPost(HTTP_PREFIX + tcAddress + "/metadata/v1/config/watch", param, header, 30000)) {
            if (response != null) {
                StatusLine statusLine = response.getStatusLine();
                if (statusLine != null && statusLine.getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
                    if (StringUtils.isNotBlank(USERNAME) && StringUtils.isNotBlank(PASSWORD)) {
                        throw new RetryableException("Authentication failed!");
                    } else {
                        throw new AuthenticationFailedException("Authentication failed! you should configure the correct username and password.");
                    }
                }
                return statusLine != null && statusLine.getStatusCode() == HttpStatus.SC_OK;
            }
        } catch (IOException e) {
            LOGGER.error("watch cluster node: {}, fail: {}", tcAddress, e.getMessage(), e);
            throw new RetryableException(e.getMessage(), e);
        }
        return false;
    }
    private static void initClusterMetaData() {
        String clusterName = RAFT_CLUSTER;
        String group = RAFT_GROUP;
        if (!METADATA.containsGroup(clusterName)) {
            String raftClusterAddress = FILE_CONFIG.getConfig(getRaftServerAddrKey());
            if (StringUtils.isNotBlank(raftClusterAddress)) {
                List<InetSocketAddress> list = new ArrayList<>();
                String[] addresses = raftClusterAddress.split(",");
                for (String address : addresses) {
                    String[] endpoint = address.split(IP_PORT_SPLIT_CHAR);
                    String host = endpoint[0];
                    int port = Integer.parseInt(endpoint[1]);
                    list.add(new InetSocketAddress(host, port));
                }
                if (CollectionUtils.isEmpty(list)) {
                    throw new SeataRuntimeException(ErrorCode.ERR_CONFIG,
                            "There are no valid raft addr! you should configure the correct [config.raft.server-addr] in the config file");
                }
                INIT_ADDRESSES.put(clusterName, list);
                // init jwt token
                try {
                    refreshToken(queryHttpAddress(clusterName, group));
                } catch (Exception e) {
                    throw new RuntimeException("Init fetch token failed!", e);
                }
                // Refresh the metadata by initializing the address
                try {
                    acquireClusterMetaData(clusterName, group);
                } catch (RetryableException e) {
                    LOGGER.error("init cluster metadata fail: {}", e.getMessage(), e);
                }
                startQueryMetadata();
            }
        }
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
            try {
                Map<String, Object> configMap = acquireClusterConfigData(RAFT_CLUSTER, RAFT_GROUP, CONFIG_NAMESPACE, CONFIG_DATA_ID);
                if (CollectionUtils.isNotEmpty(configMap)) {
                    value = configMap.get(dataId) == null ? null : configMap.get(dataId).toString();
                }
            } catch (RetryableException e) {
                LOGGER.error(e.getMessage());
            }
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
        ConfigStoreListener storeListener = new ConfigStoreListener(dataId, listener);
        CONFIG_LISTENERS_MAP.computeIfAbsent(dataId, key -> new ConcurrentHashMap<>())
                .put(listener, storeListener);
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
                    Map<ConfigurationChangeListener, ConfigStoreListener> configListeners = CONFIG_LISTENERS_MAP.get(dataId);
                    if (configListeners != null) {
                        configListeners.remove(entry);
                    }
                    break;
                }
            }
        }
    }

    @Override
    public Set<ConfigurationChangeListener> getConfigListeners(String dataId) {
        ConcurrentMap<ConfigurationChangeListener, ConfigStoreListener> configListeners = CONFIG_LISTENERS_MAP.get(dataId);
        if (CollectionUtils.isNotEmpty(configListeners)) {
            return configListeners.keySet();
        } else {
            return null;
        }
    }

    private static void notifyConfigMayChange(Map<String, Object> configMap) {
        String configStr = ConfigStoreManager.convertConfig2Str(configMap);
        CONFIG_LISTENER.onChangeEvent(new ConfigurationChangeEvent(CONFIG_NAMESPACE, configStr));
    }


    private static String getRaftUsernameKey() {
        return String.join(ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR, ConfigurationKeys.FILE_ROOT_CONFIG, CONFIG_TYPE, USERNAME_KEY);
    }
    private static String getRaftPasswordKey() {
        return String.join(ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR, ConfigurationKeys.FILE_ROOT_CONFIG, CONFIG_TYPE, PASSWORD_KEY);
    }
    private static String getRaftServerAddrKey() {
        return String.join(ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR, ConfigurationKeys.FILE_ROOT_CONFIG, CONFIG_TYPE, SERVER_ADDR_KEY);
    }

    private static String getTokenExpireTimeInMillisecondsKey() {
        return String.join(ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR, ConfigurationKeys.FILE_ROOT_CONFIG, CONFIG_TYPE, TOKEN_VALID_TIME_MS_KEY);
    }

    private static boolean isTokenExpired() {
        if (tokenTimeStamp == -1) {
            return true;
        }
        long tokenExpiredTime = tokenTimeStamp + TOKEN_EXPIRE_TIME_IN_MILLISECONDS;
        return System.currentTimeMillis() >= tokenExpiredTime;
    }

    private static synchronized void refreshToken(String tcAddress) throws RetryableException {
        // double-check if the token is expired inside the synchronized method to avoid repeated token refreshes in multiple threads
        if (!isTokenExpired()) {
            return;
        }
        // if username and password is not in config , return
        if (StringUtils.isBlank(USERNAME) || StringUtils.isBlank(PASSWORD)) {
            return;
        }
        // get token and set it in cache
        Map<String, String> param = new HashMap<>();
        param.put(USERNAME_KEY, USERNAME);
        param.put(PASSWORD_KEY, PASSWORD);

        Map<String, String> header = new HashMap<>();
        header.put(HTTP.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
        String response = null;
        tokenTimeStamp = System.currentTimeMillis();
        try (CloseableHttpResponse httpResponse =
                     HttpClientUtil.doPost(HTTP_PREFIX + tcAddress + "/api/v1/auth/login", param, header, 1000)) {
            if (httpResponse != null) {
                if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                    response = EntityUtils.toString(httpResponse.getEntity(), StandardCharsets.UTF_8);
                    JsonNode jsonNode = OBJECT_MAPPER.readTree(response);
                    String codeStatus = jsonNode.get("code").asText();
                    if (!StringUtils.equals(codeStatus, "200")) {
                        //authorized failed,throw exception to kill process
                        throw new AuthenticationFailedException("Authentication failed! you should configure the correct username and password.");
                    }
                    jwtToken = jsonNode.get("data").asText();
                } else {
                    //authorized failed,throw exception to kill process
                    throw new AuthenticationFailedException("Authentication failed! you should configure the correct username and password.");
                }
            }
        } catch (IOException e) {
            throw new RetryableException(e.getMessage(), e);
        }
    }

    private static class ConfigStoreListener implements ConfigurationChangeListener {
        private final String dataId;
        private final ConfigurationChangeListener listener;

        public ConfigStoreListener(String dataId, ConfigurationChangeListener listener) {
            this.dataId = dataId;
            this.listener = listener;
        }
        @Override
        public void onChangeEvent(ConfigurationChangeEvent event) {
            if (CONFIG_NAMESPACE.equals(event.getDataId())) {
                Properties seataConfigNew = new Properties();
                Map<String, Object> newConfigMap = ConfigStoreManager.convertConfigStr2Map(event.getNewValue());
                if (CollectionUtils.isNotEmpty(newConfigMap)) {
                    seataConfigNew.putAll(newConfigMap);
                }
                //Get all the monitored dataids and judge whether it has been modified
                for (Map.Entry<String, ConcurrentMap<ConfigurationChangeListener, ConfigStoreListener>> entry : CONFIG_LISTENERS_MAP.entrySet()) {
                    String listenedDataId = entry.getKey();
                    String propertyOld = seataConfig.getProperty(listenedDataId, "");
                    String propertyNew = seataConfigNew.getProperty(listenedDataId, "");
                    if (!propertyOld.equals(propertyNew)) {
                        ConfigurationChangeEvent newEvent = new ConfigurationChangeEvent()
                                .setDataId(listenedDataId)
                                .setNewValue(propertyNew)
                                .setNamespace(CONFIG_NAMESPACE)
                                .setChangeType(ConfigurationChangeType.MODIFY);

                        // notify ConfigurationCache
                        ConcurrentMap<ConfigurationChangeListener, ConfigStoreListener> configListeners = entry.getValue();
                        for (ConfigurationChangeListener configListener : configListeners.keySet()) {
                            configListener.onProcessEvent(newEvent);
                        }
                    }
                }
                seataConfig = seataConfigNew;
            }
        }
    }

}
