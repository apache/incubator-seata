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
package io.seata.discovery.registry;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.seata.common.metadata.ClusterRole;
import io.seata.common.metadata.Metadata;
import io.seata.common.metadata.MetadataResponse;
import io.seata.common.metadata.Node;
import io.seata.common.store.StoreMode;
import io.seata.common.thread.NamedThreadFactory;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.StringUtils;
import io.seata.config.ConfigChangeListener;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.seata.common.DefaultValues.DEFAULT_SEATA_GROUP;

/**
 * The type File registry service.
 *
 * @author slievrly
 */
public class SeataRegistryServiceImpl implements RegistryService<ConfigChangeListener> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SeataRegistryServiceImpl.class);
    private static volatile SeataRegistryServiceImpl instance;
    private static final RegistryService<?> FILE_REGISTRY_SERVICE = FileRegistryServiceImpl.getInstance();
    private static final String IP_PORT_SPLIT_CHAR = ":";

    private static final Map<String, List<InetSocketAddress>> INIT_ADDRESSES = new HashMap<>();

    private static final Metadata METADATA = new Metadata();

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final PoolingHttpClientConnectionManager POOLING_HTTP_CLIENT_CONNECTION_MANAGER =
        new PoolingHttpClientConnectionManager();

    private static volatile ThreadPoolExecutor FIND_LEADER_EXECUTOR;

    static {
        POOLING_HTTP_CLIENT_CONNECTION_MANAGER.setMaxTotal(10);
        POOLING_HTTP_CLIENT_CONNECTION_MANAGER.setDefaultMaxPerRoute(10);
        Runtime.getRuntime().addShutdownHook(new Thread(POOLING_HTTP_CLIENT_CONNECTION_MANAGER::close));
    }

    private SeataRegistryServiceImpl() {
    }

    /**
     * Gets instance.
     *
     * @return the instance
     */
    static SeataRegistryServiceImpl getInstance() {
        if (instance == null) {
            synchronized (SeataRegistryServiceImpl.class) {
                if (instance == null) {
                    instance = new SeataRegistryServiceImpl();
                    startQueryMetadata();
                    FIND_LEADER_EXECUTOR = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                        new LinkedBlockingQueue<>(), new NamedThreadFactory("queryMetadata", 1, true));
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
        FIND_LEADER_EXECUTOR.execute(() -> {
            long currentTime = System.currentTimeMillis();
            while (true) {
                boolean fetch = System.currentTimeMillis() - currentTime >= 30000;
                if (!fetch) {
                    fetch = watch();
                }
                // Cluster changes or reaches timeout refresh time
                if (fetch) {
                    METADATA.groups().parallelStream().forEach(group -> {
                        try {
                            acquireClusterMetaData(group);
                        } catch (Exception e) {
                            // prevents an exception from being thrown that causes the thread to break
                            LOGGER.error("failed to get the leader address,error: {}", e.getMessage());
                        }
                    });
                    currentTime = System.currentTimeMillis();
                }
            }
        });
    }

    private InetSocketAddress convertInetSocketAddress(Node node) {
        String[] address = node.getAddress().split(IP_PORT_SPLIT_CHAR);
        String ip = address[0];
        int port = Integer.parseInt(address[1]);
        return new InetSocketAddress(ip, port);
    }

    @Override
    public void close() throws Exception {
    }

    private static boolean watch() {
        Map<String, String> param = new HashMap<>();
        param.put("group", DEFAULT_SEATA_GROUP);
        String tcAddress = queryHttpAddress(DEFAULT_SEATA_GROUP);
        try (CloseableHttpResponse response =
            doGet("http://" + tcAddress + "/metadata/v1/watcher", param, null, 30000)) {
            if (response != null) {
                StatusLine statusLine = response.getStatusLine();
                return statusLine != null && statusLine.getStatusCode() == HttpStatus.SC_OK;
            }
        } catch (Exception e) {
            LOGGER.error("watch cluster fail: {}", e.getMessage());
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        }
        return false;
    }

    private static String queryHttpAddress(String group) {
        List<Node> nodeList = METADATA.getNodes(group);
        List<String> addressList;
        if (CollectionUtils.isNotEmpty(nodeList)) {
            addressList = nodeList.parallelStream().map(node -> {
                String[] address = node.getAddress().split(IP_PORT_SPLIT_CHAR);
                return address[0] + IP_PORT_SPLIT_CHAR + (Integer.parseInt(address[1]) - 1000);
            }).collect(Collectors.toList());
        } else {
            // http port = netty port - 1000
            addressList = INIT_ADDRESSES.get(group).parallelStream()
                .map(inetSocketAddress -> inetSocketAddress.getAddress().getHostAddress() + IP_PORT_SPLIT_CHAR
                    + (inetSocketAddress.getPort() - 1000))
                .collect(Collectors.toList());
        }
        int length = addressList.size();
        return addressList.get(ThreadLocalRandom.current().nextInt(length));
    }

    private static void acquireClusterMetaData(String group) {
        if (METADATA.isExpired(group)) {
            synchronized (group.intern()) {
                if (METADATA.isExpired(group)) {
                    String tcAddress = queryHttpAddress(group);
                    if (StringUtils.isNotBlank(tcAddress)) {
                        Map<String, String> param = new HashMap<>();
                        param.put("group", group);
                        String response = null;
                        try (CloseableHttpResponse httpResponse =
                            doGet("http://" + tcAddress + "/metadata/v1/cluster", param, null, 1000)) {
                            if (httpResponse != null
                                && httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                                response = EntityUtils.toString(httpResponse.getEntity(), StandardCharsets.UTF_8);
                            }
                            MetadataResponse metadataResponse = null;
                            if (StringUtils.isNotBlank(response)) {
                                try {
                                    metadataResponse = OBJECT_MAPPER.readValue(response, MetadataResponse.class);
                                } catch (JsonProcessingException e) {
                                    LOGGER.error(e.getMessage(), e);
                                    return;
                                }
                            }
                            if (metadataResponse != null) {
                                List<Node> list = new ArrayList<>();
                                for (Node node : metadataResponse.getNodes()) {
                                    if (node.getRole() == ClusterRole.LEADER) {
                                        METADATA.setLeader(node);
                                    }
                                    list.add(node);
                                }
                                METADATA.setStoreMode(StoreMode.get(metadataResponse.getMode()));
                                METADATA.setNodes(group, list);
                            }
                        } catch (IOException e) {
                            LOGGER.error(e.getMessage(), e);
                        }
                    }
                }
            }
        }
    }

    public static CloseableHttpResponse doGet(String url, Map<String, String> param, Map<String, String> header,
        int timeout) {
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
            CloseableHttpClient client =
                HttpClients.custom().setConnectionManager(POOLING_HTTP_CLIENT_CONNECTION_MANAGER)
                    .setDefaultRequestConfig(RequestConfig.custom().setConnectionRequestTimeout(timeout)
                        .setSocketTimeout(timeout).setConnectTimeout(timeout).build())
                    .build();
            return client.execute(httpGet);
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
        if (!METADATA.containsGroup(clusterName)) {
            List<InetSocketAddress> list = FILE_REGISTRY_SERVICE.lookup(key);
            if (CollectionUtils.isEmpty(list)) {
                return null;
            }
            INIT_ADDRESSES.put(clusterName, list);
            // Refresh the metadata by initializing the address
            acquireClusterMetaData(clusterName);
        }
        List<Node> nodes = METADATA.getNodes(clusterName);
        if (CollectionUtils.isNotEmpty(nodes)) {
            return nodes.parallelStream().map(this::convertInetSocketAddress).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    @Override
    public List<InetSocketAddress> aliveLookup(String transactionServiceGroup) {
        if (METADATA.isRaftMode()) {
            Node leader = METADATA.getLeader(transactionServiceGroup);
            if (leader != null) {
                String[] address = leader.getAddress().split(IP_PORT_SPLIT_CHAR);
                String ip = address[0];
                int port = Integer.parseInt(address[1]);
                return Collections.singletonList(new InetSocketAddress(ip, port));
            }
        }
        return RegistryService.super.aliveLookup(transactionServiceGroup);
    }

    @Override
    public List<InetSocketAddress> refreshAliveLookup(String transactionServiceGroup,
        List<InetSocketAddress> aliveAddress) {
        if (METADATA.isRaftMode()) {
            return Collections.emptyList();
        } else {
            return RegistryService.super.refreshAliveLookup(transactionServiceGroup, aliveAddress);
        }
    }

}
