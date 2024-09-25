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
import org.apache.http.Header;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.ContentType;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.common.exception.AuthenticationFailedException;
import org.apache.seata.common.exception.RetryableException;
import org.apache.seata.common.metadata.Metadata;
import org.apache.seata.common.metadata.MetadataResponse;
import org.apache.seata.common.metadata.Node;
import org.apache.seata.common.thread.NamedThreadFactory;
import org.apache.seata.common.util.CollectionUtils;
import org.apache.seata.common.util.HttpClientUtil;
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
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The type File registry service.
 *
 */
public class RaftRegistryServiceImpl implements RegistryService<ConfigChangeListener> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RaftRegistryServiceImpl.class);

    private static final String REGISTRY_TYPE = "raft";

    private static final String PRO_SERVER_ADDR_KEY = "serverAddr";

    private static final String PRO_USERNAME_KEY = "username";

    private static final String PRO_PASSWORD_KEY = "password";

    private static final String AUTHORIZATION_HEADER = "Authorization";

    private static final String ACCESS_TOKEN_EXPIRATION_HEADER = "Access_token_near_expiration";

    private static final String PRO_REFRESH_TOKEN = "refresh_token";

    private static final String USERNAME;

    private static final String PASSWORD;

    public static String accessToken;

    public static String refreshToken;

    public static boolean isAccessTokenNearExpiration = false;

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

    static {
        USERNAME = CONFIG.getConfig(getRaftUserNameKey());
        PASSWORD = CONFIG.getConfig(getRaftPassWordKey());
    }

    private RaftRegistryServiceImpl() {
    }

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
    public void register(InetSocketAddress address) throws Exception {

    }

    @Override
    public void unregister(InetSocketAddress address) throws Exception {

    }

    @Override
    public void subscribe(String cluster, ConfigChangeListener listener) throws Exception {

    }

    @Override
    public void unsubscribe(String cluster, ConfigChangeListener listener) throws Exception {

    }

    protected static void startQueryMetadata() {
        if (REFRESH_METADATA_EXECUTOR == null) {
            synchronized (INIT_ADDRESSES) {
                if (REFRESH_METADATA_EXECUTOR == null) {
                    REFRESH_METADATA_EXECUTOR = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                        new LinkedBlockingQueue<>(), new NamedThreadFactory("refreshMetadata", 1, true));
                    REFRESH_METADATA_EXECUTOR.execute(() -> {
                        long metadataMaxAgeMs = CONFIG.getLong(ConfigurationKeys.CLIENT_METADATA_MAX_AGE_MS, 30000L);
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
                                                LOGGER.error("failed to get the leader address,error: {}", e.getMessage());
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

    private static String queryHttpAddress(String clusterName, String group) {
        List<Node> nodeList = METADATA.getNodes(clusterName, group);
        List<String> addressList = null;
        Stream<InetSocketAddress> stream = null;
        if (CollectionUtils.isNotEmpty(nodeList)) {
            List<InetSocketAddress> inetSocketAddresses = ALIVE_NODES.get(CURRENT_TRANSACTION_SERVICE_GROUP);
            if (CollectionUtils.isEmpty(inetSocketAddresses)) {
                addressList =
                    nodeList.stream().map(node -> node.getControl().createAddress()).collect(Collectors.toList());
            } else {
                stream = inetSocketAddresses.stream();
            }
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

    private static String getRaftAddrFileKey() {
        return String.join(ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR, ConfigurationKeys.FILE_ROOT_REGISTRY,
            REGISTRY_TYPE, PRO_SERVER_ADDR_KEY);
    }

    private static String getRaftUserNameKey() {
        return String.join(ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR, ConfigurationKeys.FILE_ROOT_REGISTRY,
            REGISTRY_TYPE, PRO_USERNAME_KEY);
    }

    private static String getRaftPassWordKey() {
        return String.join(ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR, ConfigurationKeys.FILE_ROOT_REGISTRY,
            REGISTRY_TYPE, PRO_PASSWORD_KEY);
    }

    private InetSocketAddress convertInetSocketAddress(Node node) {
        Node.Endpoint endpoint = node.getTransaction();
        return new InetSocketAddress(endpoint.getHost(), endpoint.getPort());
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
                return Collections.singletonList(convertInetSocketAddress(leader));
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
            String tcAddress = queryHttpAddress(clusterName, group);
            addAuthDataToHeader(header, tcAddress);
            try (CloseableHttpResponse httpResponse =
                         HttpClientUtil.doPost("http://" + tcAddress + "/metadata/v1/watch", param, header, 30000)) {
                if (httpResponse != null) {
                    handleResponse(httpResponse);
                    return false;
                }
            } catch (IOException e) {
                LOGGER.error("watch cluster node: {}, fail: {}", tcAddress, e.getMessage());
                throw new RetryableException(e.getMessage(), e);
            }
            break;
        }
        return false;
    }

    @Override
    public List<InetSocketAddress> refreshAliveLookup(String transactionServiceGroup,
        List<InetSocketAddress> aliveAddress) {
        if (METADATA.isRaftMode()) {
            Node leader = METADATA.getLeader(getServiceGroup(transactionServiceGroup));
            InetSocketAddress leaderAddress = convertInetSocketAddress(leader);
            return ALIVE_NODES.put(transactionServiceGroup,
                aliveAddress.isEmpty() ? aliveAddress : aliveAddress.parallelStream().filter(inetSocketAddress -> {
                    // Since only follower will turn into leader, only the follower node needs to be listened to
                    return inetSocketAddress.getPort() != leaderAddress.getPort() || !inetSocketAddress.getAddress()
                        .getHostAddress().equals(leaderAddress.getAddress().getHostAddress());
                }).collect(Collectors.toList()));
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
        String tcAddress = queryHttpAddress(clusterName, group);
        Map<String, String> header = new HashMap<>();
        header.put(HTTP.CONTENT_TYPE, ContentType.APPLICATION_FORM_URLENCODED.getMimeType());
        addAuthDataToHeader(header, tcAddress);
        if (StringUtils.isNotBlank(tcAddress)) {
            Map<String, String> param = new HashMap<>();
            param.put("group", group);
            String response = null;
            try (CloseableHttpResponse httpResponse =
                         HttpClientUtil.doGet("http://" + tcAddress + "/metadata/v1/cluster", param, header, 3000)) {
                if (httpResponse != null) {
                    response = handleResponse(httpResponse);
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

    private static void refreshDoubleToken(String tcAddress) throws RetryableException {
        // if username and password is not in config , return
        if ((StringUtils.isBlank(USERNAME) || StringUtils.isBlank(PASSWORD)) && refreshToken == null) {
            return;
        }
        // get token and set it in cache
        Map<String, String> param = new HashMap<>();
        param.put(PRO_USERNAME_KEY, USERNAME);
        param.put(PRO_PASSWORD_KEY, PASSWORD);
        Map<String, String> header = new HashMap<>();
        header.put(HTTP.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
        String response;
        try (CloseableHttpResponse httpResponse =
                     HttpClientUtil.doPost("http://" + tcAddress + "/metadata/v1/auth/login", param, header, 4000)) {
            if (httpResponse != null) {
                if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                    response = EntityUtils.toString(httpResponse.getEntity(), StandardCharsets.UTF_8);
                    JsonNode jsonNode = OBJECT_MAPPER.readTree(response);
                    String codeStatus = jsonNode.get("code").asText();
                    if (!StringUtils.equals(codeStatus, "200")) {
                        //authorized failed,throw exception to kill process
                        throw new AuthenticationFailedException("Authentication failed! you should configure the correct username and password.");
                    }
                    accessToken = jsonNode.get("data").asText();
                    refreshToken = httpResponse.getFirstHeader(PRO_REFRESH_TOKEN).getValue();
                    isAccessTokenNearExpiration = false;
                } else {
                    //authorized failed,throw exception to kill process
                    throw new AuthenticationFailedException("Authentication failed! you should configure the correct username and password.");
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
                    refreshDoubleToken(queryHttpAddress(clusterName, key));
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
            return nodes.parallelStream().map(this::convertInetSocketAddress).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private static String handleResponse(CloseableHttpResponse httpResponse) throws IOException, RetryableException {
        StatusLine statusLine = httpResponse.getStatusLine();
        String response = null;
        JsonNode jsonNode = null;
        if (httpResponse.getEntity() != null) {
            response = EntityUtils.toString(httpResponse.getEntity(), StandardCharsets.UTF_8);
            jsonNode = OBJECT_MAPPER.readTree(response);
        }
        if (statusLine != null && statusLine.getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
            if (jsonNode != null) {
                String message = jsonNode.get("message").asText();
                if (message.equals("Check token failed")) {
                    accessToken = null;
                    refreshToken = null;
                } else if (message.equals("Access token expired")) {
                    accessToken = null;
                }
                else if (message.equals("Refresh token expired")) {
                    refreshToken = null;
                }
            }
            if ((StringUtils.isNotBlank(USERNAME) && StringUtils.isNotBlank(PASSWORD))
                    || StringUtils.isNotBlank(accessToken) || StringUtils.isNotBlank(refreshToken)) {
                throw new RetryableException("Authentication failed!");
            } else {
                throw new AuthenticationFailedException("Authentication failed! you should configure the correct username and password.");
            }
        }
        if (statusLine != null && statusLine.getStatusCode() == HttpStatus.SC_OK) {
            Header header = httpResponse.getFirstHeader(ACCESS_TOKEN_EXPIRATION_HEADER);
            if ( header != null && header.getValue().equals("true")) {
                isAccessTokenNearExpiration = true;
            }
            Header newAccessTokenHeader = httpResponse.getFirstHeader(AUTHORIZATION_HEADER);
            if (newAccessTokenHeader != null && StringUtils.isNotBlank(newAccessTokenHeader.getValue())) {
                accessToken = newAccessTokenHeader.getValue();
                isAccessTokenNearExpiration = false;
            }
        }
        return response;
    }

    private static void addAuthDataToHeader(Map<String, String> header, String tcAddress) throws RetryableException {
        if (StringUtils.isNotBlank(accessToken) && !isAccessTokenNearExpiration) {
            header.put(AUTHORIZATION_HEADER, accessToken);
        } else if (refreshToken != null) {
            header.put(PRO_REFRESH_TOKEN, refreshToken);
        } else {
            refreshDoubleToken(tcAddress);
            if (StringUtils.isNotBlank(accessToken)) {
                header.put(AUTHORIZATION_HEADER, accessToken);
            }
        }
    }

}
