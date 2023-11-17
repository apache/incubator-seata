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
package io.seata.discovery.registry.raft;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.seata.common.ConfigurationKeys;
import io.seata.common.exception.AuthenticationFailedException;
import io.seata.common.metadata.Metadata;
import io.seata.common.metadata.MetadataResponse;
import io.seata.common.metadata.Node;
import io.seata.common.thread.NamedThreadFactory;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.StringUtils;
import io.seata.config.ConfigChangeListener;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.discovery.registry.RegistryService;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type File registry service.
 *
 * @author funkye
 */
public class RaftRegistryServiceImpl implements RegistryService<ConfigChangeListener> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RaftRegistryServiceImpl.class);

    private static final String REGISTRY_TYPE = "raft";

    private static final String PRO_SERVER_ADDR_KEY = "serverAddr";

    private static final String PRO_USERNAME_KEY = "username";

    private static final String PRO_PASSWORD_KEY = "password";

    private static final String AUTHORIZATION_HEADER = "Authorization";

    private static long TOKEN_EXPIRE_TIME_IN_MILLISECONDS;

    public static String jwtToken;

    private static long tokenTimeStamp = -1;

    private static volatile RaftRegistryServiceImpl instance;

    private static final Configuration CONFIG = ConfigurationFactory.getInstance();

    private static final String IP_PORT_SPLIT_CHAR = ":";

    private static final Map<String, List<InetSocketAddress>> INIT_ADDRESSES = new HashMap<>();

    private static final Metadata METADATA = new Metadata();

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final PoolingHttpClientConnectionManager POOLING_HTTP_CLIENT_CONNECTION_MANAGER =
        new PoolingHttpClientConnectionManager();

    private static final Map<Integer/*timeout*/, CloseableHttpClient> HTTP_CLIENT_MAP = new ConcurrentHashMap<>();

    private static volatile String CURRENT_TRANSACTION_SERVICE_GROUP;

    private static volatile String CURRENT_TRANSACTION_CLUSTER_NAME;

    private static volatile ThreadPoolExecutor REFRESH_METADATA_EXECUTOR;

    private static final AtomicBoolean CLOSED = new AtomicBoolean(false);

    /**
     * Service node health check
     */
    private static final Map<String, List<InetSocketAddress>> ALIVE_NODES = new ConcurrentHashMap<>();

    static {
        POOLING_HTTP_CLIENT_CONNECTION_MANAGER.setMaxTotal(10);
        POOLING_HTTP_CLIENT_CONNECTION_MANAGER.setDefaultMaxPerRoute(10);
    }

    private RaftRegistryServiceImpl() {
        TOKEN_EXPIRE_TIME_IN_MILLISECONDS = CONFIG.getLong(getTokenExpireTimeInMillisecondsKey(), 29 * 60 * 1000L);
        refreshToken();
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
                            // Forced refresh of metadata information after set age
                            boolean fetch = System.currentTimeMillis() - currentTime > metadataMaxAgeMs;
                            String clusterName = CURRENT_TRANSACTION_CLUSTER_NAME;
                            if (!fetch) {
                                fetch = watch();
                            }
                            // Cluster changes or reaches timeout refresh time
                            if (fetch) {
                                AtomicBoolean success = new AtomicBoolean(true);
                                METADATA.groups(clusterName).parallelStream().forEach(group -> {
                                    try {
                                        acquireClusterMetaData(clusterName, group);
                                    } catch (Exception e) {
                                        success.set(false);
                                        // prevents an exception from being thrown that causes the thread to break
                                        LOGGER.error("failed to get the leader address,error: {}", e.getMessage());
                                    }
                                });
                                if (success.get()) {
                                    currentTime = System.currentTimeMillis();
                                    if (LOGGER.isDebugEnabled()) {
                                        LOGGER.debug("refresh seata cluster metadata time: {}", currentTime);
                                    }
                                }
                            }
                        }
                    });
                    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                        CLOSED.compareAndSet(false, true);
                        REFRESH_METADATA_EXECUTOR.shutdown();
                        HTTP_CLIENT_MAP.values().parallelStream().forEach(client -> {
                            try {
                                client.close();
                            } catch (IOException e) {
                                LOGGER.error(e.getMessage(), e);
                            }
                        });
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
                    map.put(node.getTransaction().getHost() + IP_PORT_SPLIT_CHAR + node.getTransaction().getPort(),
                        node);
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

    private static String getTokenExpireTimeInMillisecondsKey() {
        return String.join(ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR, ConfigurationKeys.FILE_ROOT_REGISTRY,
            REGISTRY_TYPE, ConfigurationKeys.TOKEN_VALID_TIME_MS_KEY);
    }

    private static boolean isTokenExpired() {
        if (tokenTimeStamp == -1) {
            return true;
        }
        long tokenExpiredTime = tokenTimeStamp + TOKEN_EXPIRE_TIME_IN_MILLISECONDS;
        return System.currentTimeMillis() >= tokenExpiredTime;
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

    private static boolean watch() {
        Map<String, String> header = new HashMap<>();
        Map<String, String> param = new HashMap<>();
        String clusterName = CURRENT_TRANSACTION_CLUSTER_NAME;
        Map<String, Long> groupTerms = METADATA.getClusterTerm(clusterName);
        groupTerms.forEach((k, v) -> param.put(k, String.valueOf(v)));
        for (String group : groupTerms.keySet()) {
            String tcAddress = queryHttpAddress(clusterName, group);
            if (isTokenExpired()) {
                refreshToken();
            }
            if (!Objects.isNull(jwtToken)) {
                header.put(AUTHORIZATION_HEADER, jwtToken);
            }
            try (CloseableHttpResponse response =
                     doPost("http://" + tcAddress + "/metadata/v1/watch", param, header, 30000, "application/x-www-form-urlencoded")) {
                if (response != null) {
                    StatusLine statusLine = response.getStatusLine();
                    if (statusLine != null && statusLine.getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
                        throw new AuthenticationFailedException("Authentication failed! you should configure the correct username and password.");
                    }
                    return statusLine != null && statusLine.getStatusCode() == HttpStatus.SC_OK;
                }
            } catch (AuthenticationFailedException e) {
                throw e;
            } catch (Exception e) {
                LOGGER.error("watch cluster fail: {}", e.getMessage());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {
                }
                continue;
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
        acquireClusterMetaData(clusterName, "");
    }

    private static void acquireClusterMetaData(String clusterName, String group) {
        String tcAddress = queryHttpAddress(clusterName, group);
        Map<String, String> header = new HashMap<>();
        if (isTokenExpired()) {
            refreshToken();
        }
        if (!Objects.isNull(jwtToken)) {
            header.put(AUTHORIZATION_HEADER, jwtToken);
        }
        if (StringUtils.isNotBlank(tcAddress)) {
            Map<String, String> param = new HashMap<>();
            param.put("group", group);
            String response = null;
            try (CloseableHttpResponse httpResponse =
                     doGet("http://" + tcAddress + "/metadata/v1/cluster", param, header, 1000)) {
                if (httpResponse != null) {
                    if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                        response = EntityUtils.toString(httpResponse.getEntity(), StandardCharsets.UTF_8);
                    } else if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
                        throw new AuthenticationFailedException("Authentication failed! you should configure the correct username and password.");
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
                LOGGER.error(e.getMessage(), e);
            }
        }
    }

    private static void refreshToken() {
        // if username and password is not in config , return
        String username = CONFIG.getConfig(getRaftUserNameKey());
        String password = CONFIG.getConfig(getRaftPassWordKey());
        if (Objects.isNull(username) || Objects.isNull(password)) {
            return;
        }
        String raftClusterAddress = CONFIG.getConfig(getRaftAddrFileKey());
        // get token and set it in cache
        if (StringUtils.isNotBlank(raftClusterAddress)) {
            String[] tcAddressList = raftClusterAddress.split(",");
            String tcAddress = tcAddressList[0];
            Map<String, String> param = new HashMap<>();
            param.put(PRO_USERNAME_KEY, username);
            param.put(PRO_PASSWORD_KEY, password);
            String response = null;
            try (CloseableHttpResponse httpResponse =
                     doPost("http://" + tcAddress + "/api/v1/auth/login", param, null, 1000, "application/json")) {
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
                        tokenTimeStamp = System.currentTimeMillis();
                    } else {
                        //authorized failed,throw exception to kill process
                        throw new AuthenticationFailedException("Authentication failed! you should configure the correct username and password.");
                    }
                }
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }

    }

    public static CloseableHttpResponse doGet(String url, Map<String, String> param, Map<String, String> header,
                                              int timeout) {
        CloseableHttpClient client;
        try {
            URIBuilder builder = new URIBuilder(url);
            if (param != null) {
                for (String key : param.keySet()) {
                    builder.addParameter(key, param.get(key));
                }
            }
            URI uri = builder.build();
            HttpGet httpGet = new HttpGet(uri);
            if (header != null) {
                header.forEach(httpGet::addHeader);
            }
            client = HTTP_CLIENT_MAP.computeIfAbsent(timeout,
                k -> HttpClients.custom().setConnectionManager(POOLING_HTTP_CLIENT_CONNECTION_MANAGER)
                    .setDefaultRequestConfig(RequestConfig.custom().setConnectionRequestTimeout(timeout)
                        .setSocketTimeout(timeout).setConnectTimeout(timeout).build())
                    .build());
            return client.execute(httpGet);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return null;
    }

    public static CloseableHttpResponse doPost(String url, Map<String, String> params, Map<String, String> header,
                                               int timeout, String contentType) {
        CloseableHttpClient client;
        try {
            URIBuilder builder = new URIBuilder(url);
            URI uri = builder.build();
            HttpPost httpPost = new HttpPost(uri);
            if (header != null) {
                header.forEach(httpPost::addHeader);
            }
            if (contentType != null) {
                httpPost.setHeader("Content-Type", contentType);
            }

            if ("application/x-www-form-urlencoded".equals(contentType)) {
                List<NameValuePair> nameValuePairs = new ArrayList<>();
                params.forEach((k, v) -> {
                    nameValuePairs.add(new BasicNameValuePair(k, v));
                });
                String requestBody = URLEncodedUtils.format(nameValuePairs, StandardCharsets.UTF_8);

                StringEntity stringEntity = new StringEntity(requestBody, ContentType.APPLICATION_FORM_URLENCODED);
                httpPost.setEntity(stringEntity);
            } else if ("application/json".equals(contentType)) {
                String requestBody = OBJECT_MAPPER.writeValueAsString(params);
                StringEntity stringEntity = new StringEntity(requestBody, ContentType.APPLICATION_JSON);
                httpPost.setEntity(stringEntity);
            }

            client = HTTP_CLIENT_MAP.computeIfAbsent(timeout,
                k -> HttpClients.custom().setConnectionManager(POOLING_HTTP_CLIENT_CONNECTION_MANAGER)
                    .setDefaultRequestConfig(RequestConfig.custom().setConnectionRequestTimeout(timeout)
                        .setSocketTimeout(timeout).setConnectTimeout(timeout).build())
                    .build());
            return client.execute(httpPost);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return null;
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

}
