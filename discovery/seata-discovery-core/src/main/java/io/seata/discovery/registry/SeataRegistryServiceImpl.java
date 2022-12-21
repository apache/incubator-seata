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


import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.seata.common.metadata.ClusterRole;
import io.seata.common.metadata.Metadata;
import io.seata.common.metadata.MetadataResponse;
import io.seata.common.metadata.Node;
import io.seata.common.thread.NamedThreadFactory;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.StringUtils;
import io.seata.config.ConfigChangeListener;
import org.apache.http.HttpStatus;
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

/**
 * The type File registry service.
 *
 * @author slievrly
 */
public class SeataRegistryServiceImpl implements RegistryService<ConfigChangeListener> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SeataRegistryServiceImpl.class);
    private static volatile SeataRegistryServiceImpl instance;
    private static final RegistryService<?> registryService = FileRegistryServiceImpl.getInstance();
    private static final String IP_PORT_SPLIT_CHAR = ":";

    private static final Map<String, List<InetSocketAddress>> INIT_ADDRESSES = new HashMap<>();

    private static final Metadata METADATA = new Metadata();

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final PoolingHttpClientConnectionManager POOLING_HTTP_CLIENT_CONNECTION_MANAGER =
        new PoolingHttpClientConnectionManager();
    private static final RequestConfig REQUEST_CONFIG =
        RequestConfig.custom().setConnectionRequestTimeout(10000).setSocketTimeout(10000).setConnectTimeout(10000).build();

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

    @Override
    public List<InetSocketAddress> lookup(String key) throws Exception {
        String clusterName = getServiceGroup(key);
        if (clusterName == null) {
            return null;
        }
        if(!METADATA.containsGroup(clusterName)){
            List<InetSocketAddress> list = registryService.lookup(key);
            if(CollectionUtils.isEmpty(list)){
                return null;
            }
            INIT_ADDRESSES.put(clusterName,list);
            // Refresh the metadata by initializing the address
            acquireClusterMetaData(clusterName);
        }
        Node leader = METADATA.getLeader(clusterName);
        // leader is not empty, it must be raft mode
        if (leader != null) {
            String[] address = leader.getAddress().split(IP_PORT_SPLIT_CHAR);
            String ip = address[0];
            int port = Integer.parseInt(address[1]);
            return Collections.singletonList(new InetSocketAddress(ip, port));
        } else {
            List<Node> nodes = METADATA.getNodes(clusterName);
            if (CollectionUtils.isNotEmpty(nodes)) {
                return nodes.parallelStream().map(this::convertInetSocketAddress).collect(Collectors.toList());
            }
        }
        return Collections.emptyList();
    }

    private InetSocketAddress convertInetSocketAddress(Node node){
        String[] address = node.getAddress().split(IP_PORT_SPLIT_CHAR);
        String ip = address[0];
        int port = Integer.parseInt(address[1]);
        return new InetSocketAddress(ip, port);
    }

    @Override
    public void close() throws Exception {

    }


    protected static void startQueryMetadata() {
        ScheduledThreadPoolExecutor findLeaderExecutor =
            new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("queryMetadata", 1, true));
        // The leader election takes 5 second
        findLeaderExecutor.scheduleAtFixedRate(() -> METADATA.groups().parallelStream().forEach(group -> {
            try {
                acquireClusterMetaData(group);
            } catch (Exception e) {
                // prevents an exception from being thrown that causes the thread to break
                LOGGER.error("failed to get the leader address,error:{}", e.getMessage());
            }
        }), 5000, 5000, TimeUnit.MILLISECONDS);
    }

    private static void acquireClusterMetaData(String group) {
        if (METADATA.isExpired(group)) {
            synchronized (group.intern()) {
                if (METADATA.isExpired(group)) {
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
                            .map(inetSocketAddress -> inetSocketAddress.getAddress().getHostAddress()
                                + IP_PORT_SPLIT_CHAR + (inetSocketAddress.getPort() - 1000))
                            .collect(Collectors.toList());
                    }
                    int length = addressList.size();
                    String tcAddress = addressList.get(ThreadLocalRandom.current().nextInt(length));
                    if (StringUtils.isNotBlank(tcAddress)) {
                        Map<String, String> param = new HashMap<>();
                        param.put("group", group);
                        String response = doGet("http://" + tcAddress + "/metadata/v1/cluster", param, null);
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
                            METADATA.setNodes(group, list);
                        }
                    }
                }
            }
        }
    }

    public static String doGet(String url, Map<String, String> param, Map<String, String> header) {
        String resultString = "";
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
                    .setDefaultRequestConfig(REQUEST_CONFIG).build();
            try (CloseableHttpResponse response = client.execute(httpGet)) {
                if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                    resultString = EntityUtils.toString(response.getEntity(), "UTF-8");
                }
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return resultString;
    }

}
