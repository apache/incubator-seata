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
import org.apache.http.entity.ContentType;
import org.apache.http.protocol.HTTP;
import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.common.exception.AuthenticationFailedException;
import org.apache.seata.common.exception.NotSupportYetException;
import org.apache.seata.common.exception.ParseEndpointException;
import org.apache.seata.common.exception.RetryableException;
import org.apache.seata.common.metadata.ClusterWatchEvent;
import org.apache.seata.common.metadata.Metadata;
import org.apache.seata.common.metadata.MetadataResponse;
import org.apache.seata.common.metadata.Node;
import org.apache.seata.common.thread.NamedThreadFactory;
import org.apache.seata.common.util.CollectionUtils;
import org.apache.seata.common.util.HttpClientUtil;
import org.apache.seata.common.util.NetUtil;
import org.apache.seata.common.util.SeataHttpWatch;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.config.ConfigChangeListener;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.core.protocol.Version;
import org.apache.seata.discovery.registry.RegistryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    private static final long DEFAULT_METADATA_MAX_AGE_MS = 30000L;

    private static final long WATCH_TIMEOUT_MS = 30000L;

    private static final long RETRY_DELAY_MS = 1000L;

    private static final int HTTP2_WATCH_READ_TIMEOUT_SECONDS = 30;

    private static final String MIN_HTTP2_VERSION = "2.7.0";

    private static volatile WatchProtocol CURRENT_WATCH_PROTOCOL = WatchProtocol.HTTP1;

    private static volatile SeataHttpWatch<ClusterWatchEvent> HTTP2_WATCH;

    private static volatile String HTTP2_WATCH_GROUP;

    /**
     * Service node health check
     */
    private static final Map<String, List<InetSocketAddress>> ALIVE_NODES = new ConcurrentHashMap<>();

    private static final String PREFERRED_NETWORKS;

    /**
     * Protocol used by watch mechanism.
     */
    private enum WatchProtocol {

        /** HTTP/1.x protocol */
        HTTP1,

        /** HTTP/2 protocol */
        HTTP2
    }

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

    @SuppressWarnings("AliDeprecation")
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
                        long metadataMaxAgeMs = CONFIG.getLong(getMetadataMaxAgeMs(), DEFAULT_METADATA_MAX_AGE_MS);
                        long currentTime = System.currentTimeMillis();
                        while (!CLOSED.get()) {
                            try {
                                boolean fetch = System.currentTimeMillis() - currentTime > metadataMaxAgeMs;
                                String clusterName = CURRENT_TRANSACTION_CLUSTER_NAME;
                                if (!fetch) {
                                    fetch = watch();
                                }
                                if (fetch) {
                                    for (String group : METADATA.groups(clusterName)) {
                                        try {
                                            acquireClusterMetaData(clusterName, group);
                                        } catch (Exception e) {
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
                                    Thread.sleep(RETRY_DELAY_MS);
                                } catch (InterruptedException ignored) {
                                }
                            }
                        }
                        closeHttp2Watch();
                    });
                    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                        CLOSED.compareAndSet(false, true);
                        closeHttp2Watch();
                        if (REFRESH_METADATA_EXECUTOR != null) {
                            REFRESH_METADATA_EXECUTOR.shutdown();
                        }
                    }));
                }
            }
        }
    }

    private static boolean watch() throws RetryableException {
        String clusterName = CURRENT_TRANSACTION_CLUSTER_NAME;
        if (StringUtils.isBlank(clusterName)) {
            return false;
        }

        WatchProtocol targetProtocol = resolveWatchProtocol(clusterName);
        switchWatchProtocolIfNecessary(targetProtocol);

        if (targetProtocol == WatchProtocol.HTTP2) {
            return watchHttp2(clusterName);
        }
        return watchHttp1(clusterName);
    }

    private static void switchWatchProtocolIfNecessary(WatchProtocol targetProtocol) {
        if (CURRENT_WATCH_PROTOCOL == targetProtocol) {
            return;
        }

        LOGGER.info("Switching raft watch protocol from {} to {}", CURRENT_WATCH_PROTOCOL, targetProtocol);
        if (targetProtocol == WatchProtocol.HTTP1) {
            closeHttp2Watch();
        }
        CURRENT_WATCH_PROTOCOL = targetProtocol;
    }

    private static WatchProtocol resolveWatchProtocol(String clusterName) {
        if (StringUtils.isBlank(clusterName)) {
            return WatchProtocol.HTTP1;
        }

        Set<String> groups = METADATA.groups(clusterName);
        if (CollectionUtils.isEmpty(groups)) {
            return WatchProtocol.HTTP1;
        }

        boolean hasNode = false;
        for (String group : groups) {
            List<Node> nodes = METADATA.getNodes(clusterName, group);
            if (CollectionUtils.isEmpty(nodes)) {
                continue;
            }
            hasNode = true;
            if (!isClusterHttp2Enabled(clusterName, group)) {
                return WatchProtocol.HTTP1;
            }
        }

        return hasNode ? WatchProtocol.HTTP2 : WatchProtocol.HTTP1;
    }

    private static boolean watchHttp1(String clusterName) throws RetryableException {
        Map<String, String> header = new HashMap<>();
        header.put(HTTP.CONTENT_TYPE, ContentType.APPLICATION_FORM_URLENCODED.getMimeType());
        Map<String, String> param = new HashMap<>();
        Map<String, Long> groupTerms = METADATA.getClusterTerm(clusterName);
        groupTerms.forEach((k, v) -> param.put(k, String.valueOf(v)));
        for (String group : groupTerms.keySet()) {
            String tcAddress = queryHttpAddress(clusterName, group);
            if (StringUtils.isBlank(tcAddress)) {
                return false;
            }
            if (isTokenExpired()) {
                refreshToken(tcAddress);
            }
            if (StringUtils.isNotBlank(jwtToken)) {
                header.put(AUTHORIZATION_HEADER, jwtToken);
            }
            try (Response response = HttpClientUtil.doPost(
                    "http://" + tcAddress + "/metadata/v1/watch", param, header, (int) WATCH_TIMEOUT_MS)) {
                if (response != null) {
                    int statusCode = response.code();
                    if (statusCode == HttpStatus.SC_UNAUTHORIZED) {
                        if (StringUtils.isNotBlank(USERNAME) && StringUtils.isNotBlank(PASSWORD)) {
                            throw new RetryableException("Authentication failed!");
                        } else {
                            throw new AuthenticationFailedException(
                                    "Authentication failed! you should configure the correct username and password.");
                        }
                    }
                    return statusCode == HttpStatus.SC_OK;
                }
            } catch (IOException e) {
                LOGGER.error("watch cluster node: {}, fail: {}", tcAddress, e.getMessage());
                throw new RetryableException(e.getMessage(), e);
            }
            break;
        }
        return false;
    }

    private static boolean watchHttp2(String clusterName) throws RetryableException {
        Map<String, Long> groupTerms = METADATA.getClusterTerm(clusterName);
        if (CollectionUtils.isEmpty(groupTerms)) {
            return false;
        }

        String group = selectWatchGroup(groupTerms);
        if (StringUtils.isBlank(group)) {
            return false;
        }
        String tcAddress = queryHttpAddress(clusterName, group);
        if (StringUtils.isBlank(tcAddress)) {
            return false;
        }

        Map<String, String> header = new HashMap<>();
        header.put(HTTP.CONTENT_TYPE, ContentType.APPLICATION_FORM_URLENCODED.getMimeType());

        Map<String, String> param = new HashMap<>();
        groupTerms.forEach((k, v) -> param.put(k, String.valueOf(v)));

        if (isTokenExpired()) {
            refreshToken(tcAddress);
        }
        if (StringUtils.isNotBlank(jwtToken)) {
            header.put(AUTHORIZATION_HEADER, jwtToken);
        }

        ensureHttp2Watch(group, tcAddress, param, header);
        SeataHttpWatch<ClusterWatchEvent> watch = HTTP2_WATCH;
        if (watch == null) {
            return false;
        }

        try {
            SeataHttpWatch.Response<ClusterWatchEvent> response = watch.next();
            return shouldRefreshMetadata(clusterName, group, response);
        } catch (RuntimeException e) {
            if (CLOSED.get()) {
                closeHttp2Watch();
                return false;
            }
            closeHttp2Watch();
            throw new RetryableException("HTTP2 watch failed", e);
        }
    }

    private static String selectWatchGroup(Map<String, Long> groupTerms) {
        if (CollectionUtils.isEmpty(groupTerms)) {
            return null;
        }

        if (StringUtils.isNotBlank(HTTP2_WATCH_GROUP) && groupTerms.containsKey(HTTP2_WATCH_GROUP)) {
            return HTTP2_WATCH_GROUP;
        }

        List<String> groups = new ArrayList<>(groupTerms.keySet());
        Collections.sort(groups);
        return groups.get(0);
    }

    private static synchronized void ensureHttp2Watch(
            String group, String tcAddress, Map<String, String> param, Map<String, String> header)
            throws RetryableException {

        if (HTTP2_WATCH != null && StringUtils.equals(group, HTTP2_WATCH_GROUP)) {
            return;
        }

        closeHttp2Watch();

        try {
            HTTP2_WATCH = HttpClientUtil.watchPost(
                    "http://" + tcAddress + "/metadata/v1/watch",
                    param,
                    header,
                    ClusterWatchEvent.class,
                    HTTP2_WATCH_READ_TIMEOUT_SECONDS);
            HTTP2_WATCH_GROUP = group;
        } catch (IOException e) {
            closeHttp2Watch();
            throw new RetryableException(e.getMessage(), e);
        } catch (RuntimeException e) {
            closeHttp2Watch();
            if (e.getMessage() != null && e.getMessage().contains("401")) {
                tokenTimeStamp = -1;
            }
            throw new RetryableException("Failed to create HTTP2 watch", e);
        }
    }

    private static boolean shouldRefreshMetadata(
            String clusterName, String defaultGroup, SeataHttpWatch.Response<ClusterWatchEvent> response) {

        if (response == null
                || response.type != SeataHttpWatch.Response.Type.UPDATE
                || response.object == null
                || response.object.getMetadata() == null
                || CollectionUtils.isEmpty(response.object.getMetadata().getNodes())) {
            return false;
        }

        ClusterWatchEvent event = response.object;
        MetadataResponse incomingMetadata = event.getMetadata();

        String eventGroup = StringUtils.isNotBlank(event.getGroup()) ? event.getGroup() : defaultGroup;
        long localTerm = METADATA.getClusterTerm(clusterName).getOrDefault(eventGroup, -1L);
        if (incomingMetadata.getTerm() < localTerm) {
            return false;
        }
        boolean termAdvanced = incomingMetadata.getTerm() > localTerm;

        boolean changed = termAdvanced || hasMetadataChanged(clusterName, eventGroup, incomingMetadata);

        if (changed) {
            METADATA.refreshMetadata(clusterName, incomingMetadata);
        }

        return changed;
    }

    private static boolean hasMetadataChanged(String clusterName, String group, MetadataResponse incomingMetadata) {
        if (incomingMetadata == null) {
            return false;
        }

        List<Node> incomingNodes = incomingMetadata.getNodes();
        List<Node> localNodes = METADATA.getNodes(clusterName, group);

        if (CollectionUtils.isEmpty(localNodes) != CollectionUtils.isEmpty(incomingNodes)) {
            return true;
        }

        if (CollectionUtils.isEmpty(localNodes)) {
            return false;
        }

        if (incomingMetadata.getTerm() > METADATA.getClusterTerm(clusterName).getOrDefault(group, -1L)) {
            return true;
        }

        if (localNodes.size() != incomingNodes.size()) {
            return true;
        }

        return !buildNodeSignatures(localNodes).equals(buildNodeSignatures(incomingNodes));
    }

    private static Set<String> buildNodeSignatures(List<Node> nodes) {
        Set<String> signatures = new HashSet<>();
        for (Node node : nodes) {
            signatures.add(buildNodeSignature(node));
        }
        return signatures;
    }

    private static String buildNodeSignature(Node node) {
        if (node == null) {
            return "";
        }

        String control = node.getControl() == null
                ? ""
                : node.getControl().getHost()
                        + IP_PORT_SPLIT_CHAR
                        + node.getControl().getPort();
        String transaction = node.getTransaction() == null
                ? ""
                : node.getTransaction().getHost()
                        + IP_PORT_SPLIT_CHAR
                        + node.getTransaction().getPort();

        return control + "|" + transaction + "|" + node.getRole() + "|" + node.getVersion() + "|" + node.getGroup();
    }

    private static synchronized void closeHttp2Watch() {
        SeataHttpWatch<ClusterWatchEvent> watch = HTTP2_WATCH;
        HTTP2_WATCH = null;
        HTTP2_WATCH_GROUP = null;
        if (watch != null) {
            try {
                watch.close();
            } catch (IOException e) {
                LOGGER.warn("Failed to close HTTP2 watch stream", e);
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
                addressList = nodeList.stream()
                        .map(RaftRegistryServiceImpl::selectControlEndpointStr)
                        .collect(Collectors.toList());
            } else {
                stream = inetSocketAddresses.stream();
            }
        } else {
            List<InetSocketAddress> initAddresses = INIT_ADDRESSES.get(clusterName);
            if (CollectionUtils.isEmpty(initAddresses)) {
                return null;
            }
            stream = initAddresses.stream();
        }
        if (addressList != null) {
            return addressList.get(ThreadLocalRandom.current().nextInt(addressList.size()));
        } else {
            Map<String, Node> map = new HashMap<>();
            if (CollectionUtils.isNotEmpty(nodeList)) {
                for (Node node : nodeList) {
                    InetSocketAddress inetSocketAddress = selectTransactionEndpoint(node);
                    map.put(inetSocketAddress.getHostString() + IP_PORT_SPLIT_CHAR + inetSocketAddress.getPort(), node);
                }
            }
            addressList = stream.map(inetSocketAddress -> {
                        String host = NetUtil.toStringHost(inetSocketAddress);
                        Node node = map.get(host + IP_PORT_SPLIT_CHAR + inetSocketAddress.getPort());
                        InetSocketAddress controlEndpoint = null;
                        if (node != null) {
                            controlEndpoint = selectControlEndpoint(node);
                        }
                        return host
                                + IP_PORT_SPLIT_CHAR
                                + (controlEndpoint != null ? controlEndpoint.getPort() : inetSocketAddress.getPort());
                    })
                    .collect(Collectors.toList());
            return addressList.isEmpty()
                    ? null
                    : addressList.get(ThreadLocalRandom.current().nextInt(addressList.size()));
        }
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
        closeHttp2Watch();
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
            param.put("group", group);
            String response = null;
            try (Response httpResponse =
                    HttpClientUtil.doGet("http://" + tcAddress + "/metadata/v1/cluster", param, header, 1000)) {
                if (httpResponse != null) {
                    int statusCode = httpResponse.code();
                    if (statusCode == HttpStatus.SC_OK) {
                        if (httpResponse.body() != null) {
                            response = httpResponse.body().string();
                        } else {
                            throw new RetryableException("Response body is null");
                        }
                    } else if (statusCode == HttpStatus.SC_UNAUTHORIZED) {
                        if (StringUtils.isNotBlank(USERNAME) && StringUtils.isNotBlank(PASSWORD)) {
                            refreshToken(tcAddress);
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
                if (StringUtils.isNotBlank(response)) {
                    try {
                        MetadataResponse metadataResponse = OBJECT_MAPPER.readValue(response, MetadataResponse.class);
                        if (CollectionUtils.isEmpty(metadataResponse.getNodes())) {
                            LOGGER.warn(
                                    "empty metadata nodes from cluster endpoint, clusterName={}, group={}, response={}",
                                    clusterName,
                                    group,
                                    response);
                            return;
                        }
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

    private static void refreshToken(String tcAddress) throws RetryableException {
        if (StringUtils.isBlank(USERNAME) || StringUtils.isBlank(PASSWORD)) {
            return;
        }
        Map<String, String> param = new HashMap<>();
        param.put(PRO_USERNAME_KEY, USERNAME);
        param.put(PRO_PASSWORD_KEY, PASSWORD);
        Map<String, String> header = new HashMap<>();
        header.put(HTTP.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
        String response = null;
        try (Response httpResponse =
                HttpClientUtil.doPost("http://" + tcAddress + "/api/v1/auth/login", param, header, 1000)) {
            if (httpResponse != null) {
                if (httpResponse.code() == HttpStatus.SC_OK) {
                    if (httpResponse.body() != null) {
                        response = httpResponse.body().string();
                        JsonNode jsonNode = OBJECT_MAPPER.readTree(response);
                        String codeStatus = jsonNode.get("code").asText();
                        if (!StringUtils.equals(codeStatus, "200")) {
                            throw new AuthenticationFailedException(
                                    "Authentication failed! you should configure the correct username and password.");
                        }
                        jwtToken = jsonNode.get("data").asText();
                        tokenTimeStamp = System.currentTimeMillis();
                    } else {
                        throw new AuthenticationFailedException("Authentication failed! Response body is null.");
                    }
                } else {
                    throw new AuthenticationFailedException(
                            "Authentication failed! you should configure the correct username and password.");
                }
            }
        } catch (IOException e) {
            throw new RetryableException(e.getMessage(), e);
        }
    }

    private static boolean supportsHttp2(Node node) {
        if (node == null) {
            return false;
        }
        String version = node.getVersion();
        if (StringUtils.isBlank(version)) {
            return false;
        }
        try {
            return Version.isAboveOrEqualVersion(version, MIN_HTTP2_VERSION);
        } catch (Exception e) {
            LOGGER.warn("Invalid version: {}, fallback to HTTP/1.1", version);
            return false;
        }
    }

    private static boolean isClusterHttp2Enabled(String clusterName, String group) {
        List<Node> nodes = METADATA.getNodes(clusterName, group);
        if (CollectionUtils.isEmpty(nodes)) {
            return false;
        }
        return nodes.stream().allMatch(RaftRegistryServiceImpl::supportsHttp2);
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
                try {
                    refreshToken(queryHttpAddress(clusterName, key));
                } catch (Exception e) {
                    throw new RuntimeException("Init fetch token failed!", e);
                }
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
