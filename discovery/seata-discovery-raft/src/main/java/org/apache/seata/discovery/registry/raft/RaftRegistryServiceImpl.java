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
package org.apache.seata.discovery.registry.raft;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Response;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.ContentType;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.common.exception.AuthenticationFailedException;
import org.apache.seata.common.exception.NotSupportYetException;
import org.apache.seata.common.exception.ParseEndpointException;
import org.apache.seata.common.exception.RetryableException;
import org.apache.seata.common.executor.HttpCallback;
import org.apache.seata.common.metadata.Metadata;
import org.apache.seata.common.metadata.MetadataResponse;
import org.apache.seata.common.metadata.Node;
import org.apache.seata.common.thread.NamedThreadFactory;
import org.apache.seata.common.util.CollectionUtils;
import org.apache.seata.common.util.Http2ClientUtil;
import org.apache.seata.common.util.HttpClientUtil;
import org.apache.seata.common.util.NetUtil;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.config.ConfigChangeListener;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.discovery.registry.RegistryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * The type File registry service.
 */
public class RaftRegistryServiceImpl implements RegistryService<ConfigChangeListener> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RaftRegistryServiceImpl.class);

    private static final String REGISTRY_TYPE = "raft";

    private static final String PRO_SERVER_ADDR_KEY = "serverAddr";

    private static final String PRO_USERNAME_KEY = "username";

    private static final String PRO_PASSWORD_KEY = "password";

    private static final String AUTHORIZATION_HEADER = "Authorization";

    private static final String TOKEN_VALID_TIME_MS_KEY = "tokenValidityInMilliseconds";

    private static final String META_DATA_MAX_AGE_MS = "metadataMaxAgeMs";

    private static final long TOKEN_EXPIRE_TIME_IN_MILLISECONDS;

    private static final String USERNAME;

    private static final String PASSWORD;

    public static String jwtToken;

    private static long tokenTimeStamp = -1;

    private static volatile RaftRegistryServiceImpl instance;

    private static final Configuration CONFIG = ConfigurationFactory.CURRENT_FILE_INSTANCE;

    private static final String IP_PORT_SPLIT_CHAR = ":";

    private static final Map<String, List<InetSocketAddress>> INIT_ADDRESSES = new HashMap<>();

    private static final Metadata METADATA = new Metadata();

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static volatile String CURRENT_TRANSACTION_SERVICE_GROUP;

    private static volatile String CURRENT_TRANSACTION_CLUSTER_NAME;

    private static volatile ThreadPoolExecutor REFRESH_METADATA_EXECUTOR;

    private static final AtomicBoolean CLOSED = new AtomicBoolean(false);

    /**
     * Service node health check
     */
    private static final Map<String, List<InetSocketAddress>> ALIVE_NODES = new ConcurrentHashMap<>();

    private static final String PREFERRED_NETWORKS;

    static {
        TOKEN_EXPIRE_TIME_IN_MILLISECONDS = CONFIG.getLong(getTokenExpireTimeInMillisecondsKey(), 29 * 60 * 1000L);
        USERNAME = CONFIG.getConfig(getRaftUserNameKey());
        PASSWORD = CONFIG.getConfig(getRaftPassWordKey());
        PREFERRED_NETWORKS = CONFIG.getConfig(getPreferredNetworks());
    }

    private RaftRegistryServiceImpl() {}

    /**
     * Gets instance.
     *
     * @return the instance
     */
    static RaftRegistryServiceImpl getInstance() {
        if (instance == null) {
            synchronized (RaftRegistryServiceImpl.class) {
                if (instance == null) {
                    instance = new RaftRegistryServiceImpl();
                }
            }
        }
        return instance;
    }

    @Override
    public void register(InetSocketAddress address) throws Exception {}

    @Override
    public void unregister(InetSocketAddress address) throws Exception {}

    @Override
    public void subscribe(String cluster, ConfigChangeListener listener) throws Exception {}

    @Override
    public void unsubscribe(String cluster, ConfigChangeListener listener) throws Exception {}

    protected static void startQueryMetadata() {
        if (REFRESH_METADATA_EXECUTOR == null) {
            synchronized (INIT_ADDRESSES) {
                if (REFRESH_METADATA_EXECUTOR == null) {
                    REFRESH_METADATA_EXECUTOR = new ThreadPoolExecutor(
                            1,
                            1,
                            0L,
                            TimeUnit.MILLISECONDS,
                            new LinkedBlockingQueue<>(),
                            new NamedThreadFactory("refreshMetadata", 1, true));
                    REFRESH_METADATA_EXECUTOR.execute(() -> {
                        long metadataMaxAgeMs = CONFIG.getLong(getMetadataMaxAgeMs(), 30000L);
                        long currentTime = System.currentTimeMillis();
                        while (!CLOSED.get()) {
                            try {
                                // Forced refresh of metadata information after set age
                                boolean fetch = System.currentTimeMillis() - currentTime > metadataMaxAgeMs;
                                String clusterName = CURRENT_TRANSACTION_CLUSTER_NAME;
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
                                                LOGGER.error(
                                                        "failed to get the leader address,error: {}", e.getMessage());
                                            }
                                        }
                                    }
                                    currentTime = System.currentTimeMillis();
                                    if (LOGGER.isDebugEnabled()) {
                                        LOGGER.debug("refresh seata cluster metadata time: {}", currentTime);
                                    }
                                }
                            } catch (RetryableException e) {
                                LOGGER.error(e.getMessage(), e);
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException ignored) {
                                }
                            }
                        }
                    });
                    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                        CLOSED.compareAndSet(false, true);
                        REFRESH_METADATA_EXECUTOR.shutdown();
                    }));
                }
            }
        }
    }

    private static Node selectNodeForHttpAddress(String clusterName, String group) {
        List<Node> nodeList = METADATA.getNodes(clusterName, group);

        if (CollectionUtils.isNotEmpty(nodeList)) {
            List<InetSocketAddress> inetSocketAddresses = ALIVE_NODES.get(CURRENT_TRANSACTION_SERVICE_GROUP);

            if (CollectionUtils.isEmpty(inetSocketAddresses)) {
                return nodeList.get(ThreadLocalRandom.current().nextInt(nodeList.size()));
            }

            Map<String, Node> map = new HashMap<>();
            for (Node node : nodeList) {
                InetSocketAddress inetSocketAddress = selectTransactionEndpoint(node);
                map.put(inetSocketAddress.getHostString() + IP_PORT_SPLIT_CHAR + inetSocketAddress.getPort(), node);
            }

            List<Node> aliveNodes = inetSocketAddresses.stream()
                    .map(addr -> map.get(NetUtil.toStringHost(addr) + IP_PORT_SPLIT_CHAR + addr.getPort()))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (!aliveNodes.isEmpty()) {
                return aliveNodes.get(ThreadLocalRandom.current().nextInt(aliveNodes.size()));
            }
        } else {
            List<InetSocketAddress> initAddresses = INIT_ADDRESSES.get(clusterName);
            if (CollectionUtils.isNotEmpty(initAddresses)) {

                return null;
            }
        }
        return null;
    }

    private static String queryHttpAddress(String clusterName, Node selectedNode) {
        String address = extractHttpAddressFromNode(selectedNode);
        if (address != null) {
            return address;
        }
        List<InetSocketAddress> initAddresses = INIT_ADDRESSES.get(clusterName);
        if (CollectionUtils.isNotEmpty(initAddresses)) {
            InetSocketAddress inetSocketAddress =
                    initAddresses.get(ThreadLocalRandom.current().nextInt(initAddresses.size()));
            return NetUtil.toStringAddress(inetSocketAddress);
        }
        return null;
    }

    private static String extractHttpAddressFromNode(Node node) {
        if (node == null) {
            return null;
        }
        return selectControlEndpointStr(node);
    }

    private static String getRaftAddrFileKey() {
        return String.join(
                ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR,
                ConfigurationKeys.FILE_ROOT_REGISTRY,
                REGISTRY_TYPE,
                PRO_SERVER_ADDR_KEY);
    }

    private static String getRaftUserNameKey() {
        return String.join(
                ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR,
                ConfigurationKeys.FILE_ROOT_REGISTRY,
                REGISTRY_TYPE,
                PRO_USERNAME_KEY);
    }

    private static String getRaftPassWordKey() {
        return String.join(
                ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR,
                ConfigurationKeys.FILE_ROOT_REGISTRY,
                REGISTRY_TYPE,
                PRO_PASSWORD_KEY);
    }

    private static String getPreferredNetworks() {
        return String.join(
                ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR, ConfigurationKeys.FILE_ROOT_REGISTRY, "preferredNetworks");
    }

    private static String getTokenExpireTimeInMillisecondsKey() {
        return String.join(
                ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR,
                ConfigurationKeys.FILE_ROOT_REGISTRY,
                REGISTRY_TYPE,
                TOKEN_VALID_TIME_MS_KEY);
    }

    private static boolean isTokenExpired() {
        if (tokenTimeStamp == -1) {
            return true;
        }
        long tokenExpiredTime = tokenTimeStamp + TOKEN_EXPIRE_TIME_IN_MILLISECONDS;
        return System.currentTimeMillis() >= tokenExpiredTime;
    }

    private static String selectControlEndpointStr(Node node) {
        InetSocketAddress control = selectControlEndpoint(node);
        return NetUtil.toStringAddress(control);
    }

    private static String selectTransactionEndpointStr(Node node) {
        InetSocketAddress transaction = selectTransactionEndpoint(node);
        return NetUtil.toStringAddress(transaction);
    }

    private static InetSocketAddress selectControlEndpoint(Node node) {
        return selectEndpoint("control", node);
    }

    private static InetSocketAddress selectTransactionEndpoint(Node node) {
        return selectEndpoint("transaction", node);
    }

    private static InetSocketAddress selectEndpoint(String type, Node node) {
        if (StringUtils.isBlank(PREFERRED_NETWORKS)) {
            // Use the default method, directly using node.control and node.transaction
            switch (type) {
                case "control":
                    return new InetSocketAddress(
                            node.getControl().getHost(), node.getControl().getPort());
                case "transaction":
                    return new InetSocketAddress(
                            node.getTransaction().getHost(),
                            node.getTransaction().getPort());
                default:
                    throw new NotSupportYetException("SelectEndpoint is not support type: " + type);
            }
        }
        Node.ExternalEndpoint externalEndpoint = selectExternalEndpoint(node, PREFERRED_NETWORKS.split(";"));
        switch (type) {
            case "control":
                return new InetSocketAddress(externalEndpoint.getHost(), externalEndpoint.getControlPort());
            case "transaction":
                return new InetSocketAddress(externalEndpoint.getHost(), externalEndpoint.getTransactionPort());
            default:
                throw new NotSupportYetException("SelectEndpoint is not support type: " + type);
        }
    }

    private static Node.ExternalEndpoint selectExternalEndpoint(Node node, String[] preferredNetworks) {
        Map<String, Object> metadata = node.getMetadata();
        if (CollectionUtils.isEmpty(metadata)) {
            throw new ParseEndpointException("Node metadata is empty.");
        }

        Object external = metadata.get("external");

        if (external instanceof List<?>) {
            List<LinkedHashMap<String, Object>> externalEndpoints = (List<LinkedHashMap<String, Object>>) external;

            if (CollectionUtils.isEmpty(externalEndpoints)) {
                throw new ParseEndpointException("ExternalEndpoints should not be empty.");
            }

            for (LinkedHashMap<String, Object> externalEndpoint : externalEndpoints) {
                String ip = Optional.ofNullable(externalEndpoint.get("host"))
                        .map(Object::toString)
                        .orElse("");

                if (isPreferredNetwork(ip, Arrays.asList(preferredNetworks))) {
                    return createExternalEndpoint(externalEndpoint, ip);
                }
            }
        }
        throw new ParseEndpointException("No ExternalEndpoints value matches.");
    }

    private static boolean isPreferredNetwork(String ip, List<String> preferredNetworks) {
        return preferredNetworks.stream()
                .anyMatch(regex -> StringUtils.isNotBlank(regex) && (ip.matches(regex) || ip.startsWith(regex)));
    }

    private static Node.ExternalEndpoint createExternalEndpoint(
            LinkedHashMap<String, Object> externalEndpoint, String ip) {
        int controlPort = Integer.parseInt(externalEndpoint.get("controlPort").toString());
        int transactionPort =
                Integer.parseInt(externalEndpoint.get("transactionPort").toString());
        return new Node.ExternalEndpoint(ip, controlPort, transactionPort);
    }

    @Override
    public void close() {
        CLOSED.compareAndSet(false, true);
    }

    @Override
    public List<InetSocketAddress> aliveLookup(String transactionServiceGroup) {
        if (METADATA.isRaftMode()) {
            String clusterName = getServiceGroup(transactionServiceGroup);
            Node leader = METADATA.getLeader(clusterName);
            if (leader != null) {
                return Collections.singletonList(selectTransactionEndpoint(leader));
            }
        }
        return RegistryService.super.aliveLookup(transactionServiceGroup);
    }

    private static boolean watch() throws RetryableException {
        Map<String, String> header = new HashMap<>();
        header.put(HTTP.CONTENT_TYPE, ContentType.APPLICATION_FORM_URLENCODED.getMimeType());
        Map<String, String> param = new HashMap<>();
        String clusterName = CURRENT_TRANSACTION_CLUSTER_NAME;
        Map<String, Long> groupTerms = METADATA.getClusterTerm(clusterName);
        groupTerms.forEach((k, v) -> param.put(k, String.valueOf(v)));
        for (String group : groupTerms.keySet()) {
            Node selectedNode = selectNodeForHttpAddress(clusterName, group);
            String tcAddress = queryHttpAddress(clusterName, selectedNode);

            if (isTokenExpired()) {
                refreshToken(clusterName, selectedNode);
            }
            if (StringUtils.isNotBlank(jwtToken)) {
                header.put(AUTHORIZATION_HEADER, jwtToken);
            }

            ResponseProcessor processor = (responseBody, error) -> {
                if (error != null) {
                    LOGGER.error("Watch request failed: {}", error.getMessage(), error);
                } else if (StringUtils.isNotBlank(responseBody)) {
                    try {
                        processWatchResponse(responseBody);
                    } catch (Exception e) {
                        LOGGER.error("Error processing watch response: {}", e.getMessage(), e);
                    }
                }
            };

            if (selectedNode != null && selectedNode.isHttp2Supported()) {
                executeHttp2WatchRequest(tcAddress, param, header, processor);
            } else {
                executeHttpWatchRequest(tcAddress, param, header, processor);
            }
            return true;
        }
        return false;
    }

    private static void executeHttpWatchRequest(
            String tcAddress, Map<String, String> param, Map<String, String> header, ResponseProcessor processor) {
        try (CloseableHttpResponse response =
                HttpClientUtil.doPost("http://" + tcAddress + "/metadata/v1/watch", param, header, 30000)) {
            if (response != null) {
                StatusLine statusLine = response.getStatusLine();
                if (statusLine != null && statusLine.getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
                    processor.process(null, new RetryableException("Authentication failed!"));
                } else if (statusLine != null && statusLine.getStatusCode() == HttpStatus.SC_OK) {
                    String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                    processor.process(responseBody, null);
                } else {
                    processor.process(
                            null,
                            new RetryableException("Invalid response status: "
                                    + (statusLine != null ? statusLine.getStatusCode() : "unknown")));
                }
            } else {
                processor.process(null, new RetryableException("Null response"));
            }
        } catch (Exception e) {
            processor.process(null, new RetryableException(e.getMessage(), e));
        }
    }

    private static void executeHttp2WatchRequest(
            String tcAddress, Map<String, String> param, Map<String, String> header, ResponseProcessor processor) {
        Http2ClientUtil.doPost(
                "http://" + tcAddress + "/metadata/v1/watch", param, header, new HttpCallback<Response>() {
                    @Override
                    public void onSuccess(Response response) {
                        try {
                            String responseBody = response.body().string();
                            processor.process(responseBody, null);
                        } catch (IOException e) {
                            processor.process(null, e);
                        }
                    }

                    @Override
                    public void onFailure(Throwable e) {
                        processor.process(null, e);
                    }

                    @Override
                    public void onCancelled() {
                        processor.process(null, new RetryableException("Request cancelled"));
                    }
                });
    }

    private static void processWatchResponse(String responseBody) {
        try {
            JsonNode jsonNode = OBJECT_MAPPER.readTree(responseBody);
            boolean success = jsonNode.path("success").asBoolean(false);
            if (success) {
                LOGGER.info("Watch request successful");
            } else {
                String message = jsonNode.path("message").asText("Unknown error");
                LOGGER.warn("Watch request failed: {}", message);
            }
        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to parse watch response: {}", e.getMessage(), e);
        }
    }

    @FunctionalInterface
    private interface ResponseProcessor {
        void process(String responseBody, Throwable error);
    }

    @Override
    public List<InetSocketAddress> refreshAliveLookup(
            String transactionServiceGroup, List<InetSocketAddress> aliveAddress) {
        if (METADATA.isRaftMode()) {
            Node leader = METADATA.getLeader(getServiceGroup(transactionServiceGroup));
            InetSocketAddress leaderAddress = selectTransactionEndpoint(leader);
            return ALIVE_NODES.put(
                    transactionServiceGroup,
                    aliveAddress.isEmpty()
                            ? aliveAddress
                            : aliveAddress.parallelStream()
                                    .filter(inetSocketAddress -> {
                                        // Since only follower will turn into leader, only the follower node needs to be
                                        // listened to
                                        return inetSocketAddress.getPort() != leaderAddress.getPort()
                                                || !inetSocketAddress
                                                        .getAddress()
                                                        .getHostAddress()
                                                        .equals(leaderAddress
                                                                .getAddress()
                                                                .getHostAddress());
                                    })
                                    .collect(Collectors.toList()));
        } else {
            return RegistryService.super.refreshAliveLookup(transactionServiceGroup, aliveAddress);
        }
    }

    private static void acquireClusterMetaDataByClusterName(String clusterName) {
        try {
            acquireClusterMetaData(clusterName, "");
        } catch (RetryableException e) {
            LOGGER.warn(e.getMessage(), e);
        }
    }

    private static void acquireClusterMetaData(String clusterName, String group) throws RetryableException {
        Node selectedNode = selectNodeForHttpAddress(clusterName, group);

        if (selectedNode == null) {
            LOGGER.warn("No available node found for cluster: {}, group: {}", clusterName, group);
            return;
        }

        String tcAddress = queryHttpAddress(clusterName, selectedNode);
        Map<String, String> header = new HashMap<>();
        header.put(HTTP.CONTENT_TYPE, ContentType.APPLICATION_FORM_URLENCODED.getMimeType());
        if (isTokenExpired()) {
            refreshToken(clusterName, selectedNode);
        }
        if (StringUtils.isNotBlank(jwtToken)) {
            header.put(AUTHORIZATION_HEADER, jwtToken);
        }
        if (StringUtils.isNotBlank(tcAddress)) {
            Map<String, String> param = new HashMap<>();
            param.put("group", group);

            if (selectedNode.isHttp2Supported()) {
                Http2ClientUtil.doGet(
                        "http://" + tcAddress + "/metadata/v1/cluster",
                        header,
                        new HttpCallback<Response>() {
                            @Override
                            public void onSuccess(Response result) {
                                try {
                                    String responseBody = result.body().string();
                                    if (StringUtils.isNotBlank(responseBody)) {
                                        MetadataResponse metadataResponse =
                                                OBJECT_MAPPER.readValue(responseBody, MetadataResponse.class);
                                        METADATA.refreshMetadata(clusterName, metadataResponse);
                                        LOGGER.debug("Metadata refreshed via HTTP/2 for cluster: {}", clusterName);
                                    }
                                } catch (Exception e) {
                                    LOGGER.error("Error processing metadata response: {}", e.getMessage(), e);
                                }
                            }

                            @Override
                            public void onFailure(Throwable t) {
                                LOGGER.error("Metadata request failed: {}", t.getMessage(), t);
                            }

                            @Override
                            public void onCancelled() {
                                LOGGER.warn("Metadata request was cancelled");
                            }
                        },
                        1000);
                return;
            }
            String response = null;
            try (CloseableHttpResponse httpResponse =
                    HttpClientUtil.doGet("http://" + tcAddress + "/metadata/v1/cluster", param, header, 1000)) {
                if (httpResponse != null) {
                    int statusCode = httpResponse.getStatusLine().getStatusCode();
                    if (statusCode == HttpStatus.SC_OK) {
                        response = EntityUtils.toString(httpResponse.getEntity(), StandardCharsets.UTF_8);
                    } else if (statusCode == HttpStatus.SC_UNAUTHORIZED) {
                        if (StringUtils.isNotBlank(USERNAME) && StringUtils.isNotBlank(PASSWORD)) {
                            refreshToken(tcAddress, selectedNode);
                            throw new RetryableException("Token refreshed, retrying request.");
                        } else {
                            throw new AuthenticationFailedException(
                                    "Authentication failed! you should configure the correct username and password.");
                        }
                    } else {
                        throw new AuthenticationFailedException(
                                "Authentication failed! you should configure the correct username and password.");
                    }
                }
                MetadataResponse metadataResponse;
                if (StringUtils.isNotBlank(response)) {
                    try {
                        metadataResponse = OBJECT_MAPPER.readValue(response, MetadataResponse.class);
                        METADATA.refreshMetadata(clusterName, metadataResponse);
                    } catch (JsonProcessingException e) {
                        LOGGER.error(e.getMessage(), e);
                    }
                }
            } catch (IOException e) {
                throw new RetryableException(e.getMessage(), e);
            }
        }
    }

    private static void refreshToken(String clusterName, Node selectedNode) throws RetryableException {
        // if username and password is not in config , return
        if (StringUtils.isBlank(USERNAME) || StringUtils.isBlank(PASSWORD)) {
            return;
        }
        String tcAddress = queryHttpAddress(clusterName, selectedNode);
        // get token and set it in cache
        Map<String, String> param = new HashMap<>();
        param.put(PRO_USERNAME_KEY, USERNAME);
        param.put(PRO_PASSWORD_KEY, PASSWORD);
        Map<String, String> header = new HashMap<>();
        header.put(HTTP.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
        String response = null;

        requestJwtToken(selectedNode, tcAddress, param, header);
    }

    private static void requestJwtToken(
            Node selectedNode, String tcAddress, Map<String, String> param, Map<String, String> header)
            throws RetryableException {

        if (selectedNode == null) {
            LOGGER.warn("No available node found for token request.");
            return;
        }
        if (selectedNode.isHttp2Supported()) {
            Http2ClientUtil.doPost(
                    "http://" + tcAddress + "/api/v1/auth/login", param, header, new HttpCallback<Response>() {
                        @Override
                        public void onSuccess(Response result) {
                            try {
                                String responseBody = result.body().string();
                                JsonNode jsonNode = OBJECT_MAPPER.readTree(responseBody);
                                String codeStatus = jsonNode.get("code").asText();
                                if (StringUtils.equals(codeStatus, "200")) {
                                    jwtToken = jsonNode.get("data").asText();
                                    tokenTimeStamp = System.currentTimeMillis();
                                    LOGGER.info("JWT token refreshed successfully via HTTP/2");
                                } else {
                                    LOGGER.error("Authentication failed with code: {}", codeStatus);
                                }
                            } catch (Exception e) {
                                LOGGER.error("Error processing JWT token response: {}", e.getMessage(), e);
                            }
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            LOGGER.error("JWT token request failed: {}", t.getMessage(), t);
                        }

                        @Override
                        public void onCancelled() {
                            LOGGER.warn("JWT token request was cancelled");
                        }
                    });
            return;
        }

        String response = null;
        try (CloseableHttpResponse httpResponse =
                HttpClientUtil.doPost("http://" + tcAddress + "/api/v1/auth/login", param, header, 1000)) {
            if (httpResponse != null) {
                if (httpResponse.getStatusLine() != null
                        && httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                    response = EntityUtils.toString(httpResponse.getEntity(), StandardCharsets.UTF_8);
                    JsonNode jsonNode = OBJECT_MAPPER.readTree(response);
                    String codeStatus = jsonNode.get("code").asText();
                    if (!StringUtils.equals(codeStatus, "200")) {
                        // authorized failed,throw exception to kill process
                        throw new AuthenticationFailedException(
                                "Authentication failed! you should configure the correct username and password.");
                    }
                    jwtToken = jsonNode.get("data").asText();
                    tokenTimeStamp = System.currentTimeMillis();
                } else {
                    // authorized failed,throw exception to kill process
                    throw new AuthenticationFailedException(
                            "Authentication failed! you should configure the correct username and password.");
                }
            }
        } catch (IOException e) {
            throw new RetryableException(e.getMessage(), e);
        }
    }

    @Override
    public List<InetSocketAddress> lookup(String key) throws Exception {
        String clusterName = getServiceGroup(key);
        if (clusterName == null) {
            return null;
        }
        CURRENT_TRANSACTION_SERVICE_GROUP = key;
        CURRENT_TRANSACTION_CLUSTER_NAME = clusterName;
        if (!METADATA.containsGroup(clusterName)) {
            String raftClusterAddress = CONFIG.getConfig(getRaftAddrFileKey());
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
                    return null;
                }
                INIT_ADDRESSES.put(clusterName, list);
                // init jwt token
                try {
                    refreshToken(clusterName, selectNodeForHttpAddress(clusterName, key));
                } catch (Exception e) {
                    throw new RuntimeException("Init fetch token failed!", e);
                }
                // Refresh the metadata by initializing the address
                acquireClusterMetaDataByClusterName(clusterName);
                startQueryMetadata();
            }
        }
        List<Node> nodes = METADATA.getNodes(clusterName);
        if (CollectionUtils.isNotEmpty(nodes)) {
            return nodes.parallelStream()
                    .map(RaftRegistryServiceImpl::selectTransactionEndpoint)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private static String getMetadataMaxAgeMs() {
        return String.join(
                ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR,
                ConfigurationKeys.FILE_ROOT_REGISTRY,
                REGISTRY_TYPE,
                META_DATA_MAX_AGE_MS);
    }
}
