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
package org.apache.seata.discovery.registry.namingserver;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.util.List;
import java.util.HashMap;
import java.util.Objects;
import java.util.Map;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.ContentType;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.apache.seata.common.metadata.Node;
import org.apache.seata.common.metadata.namingserver.Instance;
import org.apache.seata.common.metadata.namingserver.MetaResponse;
import org.apache.seata.common.thread.NamedThreadFactory;
import org.apache.seata.common.util.CollectionUtils;
import org.apache.seata.common.util.HttpClientUtil;
import org.apache.seata.common.util.NetUtil;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.discovery.registry.RegistryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class NamingserverRegistryServiceImpl implements RegistryService<NamingListener> {
    private static final Logger LOGGER = LoggerFactory.getLogger(NamingserverRegistryServiceImpl.class);

    public static volatile NamingserverRegistryServiceImpl instance;
    private static final String NAMESPACE_KEY = "namespace";
    private static final String VGROUP_KEY = "vGroup";
    private static final String CLIENT_TERM_KEY = "clientTerm";
    private static final String DEFAULT_NAMESPACE = "public";
    private static final String NAMING_SERVICE_URL_KEY = "server-addr";
    private static final String FILE_ROOT_REGISTRY = "registry";
    private static final String FILE_CONFIG_SPLIT_CHAR = ".";
    private static final String REGISTRY_TYPE = "seata";
    private static final String HTTP_PREFIX = "http://";
    private static final String TIME_OUT_KEY = "timeout";

    private static final String HEART_BEAT_KEY = "heartbeat-period";
    private static int healthcheckPeriod = 5 * 1000;
    private static final int PULL_PERIOD = 30 * 1000;
    private static final int LONG_POLL_TIME_OUT_PERIOD = 28 * 1000;
    private static final int THREAD_POOL_NUM = 1;
    private static final int HEALTH_CHECK_THRESHOLD = 1; // namingserver is considered unhealthy if failing in healthy check more than 1 times
    private volatile long term = 0;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private volatile boolean isSubscribed = false;
    private static final Configuration FILE_CONFIG = ConfigurationFactory.CURRENT_FILE_INSTANCE;
    private String namingServerAddressCache;
    private static ConcurrentMap<String /* namingserver address */, AtomicInteger /* Number of Health Check Continues Failures */> AVAILABLE_NAMINGSERVER_MAP = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String/* vgroup */, List<InetSocketAddress>> VGROUP_ADDRESS_MAP = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String/* vgroup */, List<NamingListener>> LISTENER_SERVICE_MAP = new ConcurrentHashMap<>();
    protected final ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("seata-namingser-scheduled", THREAD_POOL_NUM, true));
    private final ExecutorService notifierExecutor = new ThreadPoolExecutor(THREAD_POOL_NUM, THREAD_POOL_NUM, Integer.MAX_VALUE, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), new NamedThreadFactory("serviceNamingNotifier", THREAD_POOL_NUM));


    private NamingserverRegistryServiceImpl() {
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        String heartBeatKey = String.join(FILE_CONFIG_SPLIT_CHAR, FILE_ROOT_REGISTRY, REGISTRY_TYPE, HEART_BEAT_KEY);
        healthcheckPeriod = FILE_CONFIG.getInt(heartBeatKey, healthcheckPeriod);
        List<String> urlList = getNamingAddrs();
        checkAvailableNamingAddr(urlList);
        this.executorService.scheduleAtFixedRate(() -> checkAvailableNamingAddr(urlList), healthcheckPeriod,
                healthcheckPeriod, TimeUnit.MILLISECONDS);
    }

    private void checkAvailableNamingAddr(List<String> urlList) {
        for (String url : urlList) {
            AtomicInteger unHealthCount = AVAILABLE_NAMINGSERVER_MAP.computeIfAbsent(url, value -> new AtomicInteger(0));
            // do health check
            boolean isHealthy = doHealthCheck(url);
            int unHealthCountBefore = unHealthCount.get();
            if (!isHealthy) {
                unHealthCount.incrementAndGet();
            } else {
                unHealthCount.set(0);
                AVAILABLE_NAMINGSERVER_MAP.put(url, unHealthCount);
            }
            // record message that naming server node going online or going offline
            int unHealthCountAfter = unHealthCount.get();
            if (!Objects.equals(unHealthCountAfter, 0) && unHealthCountAfter == HEALTH_CHECK_THRESHOLD) {
                LOGGER.error("naming server node go offline {}", url);
            }
            if (!Objects.equals(unHealthCountAfter, unHealthCountBefore) && unHealthCountAfter == 0) {
                LOGGER.info("naming server node go online {}", url);
            }
        }
    }

    /**
     * Gets instance.
     *
     * @return the instance
     */
    static NamingserverRegistryServiceImpl getInstance() {

        if (instance == null) {
            synchronized (NamingserverRegistryServiceImpl.class) {
                if (instance == null) {
                    instance = new NamingserverRegistryServiceImpl();
                }
            }
        }
        return instance;
    }


    @Override
    public void register(InetSocketAddress address) throws Exception {
        NetUtil.validAddress(address);
        Instance instance = Instance.getInstance();
        instance.setTransaction(new Node.Endpoint(address.getAddress().getHostAddress(), address.getPort(), "netty"));
        instance.setTimestamp(System.currentTimeMillis());
        doRegister(instance, getNamingAddrs());
    }

    public void doRegister(Instance instance, List<String> urlList) {
        for (String urlSuffix : urlList) {
            // continue if name server node is unhealthy
            if (AVAILABLE_NAMINGSERVER_MAP.computeIfAbsent(urlSuffix, value -> new AtomicInteger(0)).get() >= HEALTH_CHECK_THRESHOLD) {
                continue;
            }
            String url = HTTP_PREFIX + urlSuffix + "/naming/v1/register?";
            String namespace = instance.getNamespace();
            String clusterName = instance.getClusterName();
            String unit = instance.getUnit();
            String jsonBody = instance.toJsonString(OBJECT_MAPPER);
            String params = "namespace=" + namespace + "&clusterName=" + clusterName + "&unit=" + unit;
            url += params;
            Map<String, String> header = new HashMap<>();
            header.put(HTTP.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());

            try (CloseableHttpResponse response = HttpClientUtil.doPost(url, jsonBody, header, 3000)) {
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode == 200) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("instance has been registered successfully:{}", statusCode);
                    }
                } else {
                    LOGGER.warn("instance has been registered unsuccessfully:{}", statusCode);
                }
            } catch (Exception e) {
                LOGGER.error("instance has been registered failed in namingserver {}", url);
            }
        }
    }

    public boolean doHealthCheck(String url) {
        url = HTTP_PREFIX + url + "/naming/v1/health";
        Map<String, String> header = new HashMap<>();
        header.put(HTTP.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
        try (CloseableHttpResponse response = HttpClientUtil.doGet(url, null, header, 3000)) {
            int statusCode = response.getStatusLine().getStatusCode();
            return statusCode == 200;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void unregister(InetSocketAddress address) {
        NetUtil.validAddress(address);
        Instance instance = Instance.getInstance();
        instance.setTransaction(new Node.Endpoint(address.getAddress().getHostAddress(), address.getPort(), "netty"));
        for (String urlSuffix : getNamingAddrs()) {
            String url = HTTP_PREFIX + urlSuffix + "/naming/v1/unregister?";
            String unit = instance.getUnit();
            String jsonBody = instance.toJsonString(OBJECT_MAPPER);
            String params = "unit=" + unit;
            params = params + "&clusterName=" + instance.getClusterName();
            params = params + "&namespace=" + instance.getNamespace();
            url += params;
            Map<String, String> header = new HashMap<>();
            header.put(HTTP.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
            try (CloseableHttpResponse response = HttpClientUtil.doPost(url, jsonBody, header, 3000)) {
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode == 200) {
                    LOGGER.info("instance has been unregistered successfully:{}", statusCode);
                } else {
                    LOGGER.warn("instance has been unregistered unsuccessfully:{}", statusCode);
                }
            } catch (Exception e) {
                LOGGER.error("instance has been unregistered failed in namingserver {}", url, e);
            }
        }
    }

    @Override
    public void subscribe(String cluster, NamingListener listener) throws Exception {

    }

    public void subscribe(NamingListener listener, String vGroup) throws Exception {
        LISTENER_SERVICE_MAP.computeIfAbsent(vGroup, key -> new ArrayList<>()).add(listener);
        isSubscribed = true;
        notifierExecutor.execute(() -> {
            long currentTime = System.currentTimeMillis();
            while (isSubscribed) {
                try {
                    // pull
                    boolean needFetch = System.currentTimeMillis() - currentTime > PULL_PERIOD;
                    if (!needFetch) {
                        // push
                        needFetch = watch(vGroup);
                    }
                    if (needFetch) {
                        for (NamingListener namingListener : LISTENER_SERVICE_MAP.get(vGroup)) {
                            try {
                                namingListener.onEvent(vGroup);
                            } catch (Exception e) {
                                LOGGER.warn("vGroup {} onEvent wrong {}", vGroup, e);
                                try {
                                    TimeUnit.SECONDS.sleep(1);
                                } catch (InterruptedException ignored) {
                                }
                            }
                        }
                        namingServerAddressCache = null;
                        currentTime = System.currentTimeMillis();
                    }
                } catch (Exception ex) {
                    LOGGER.error("watch failed! ", ex);
                    try {
                        Thread.sleep(1000);
                    } catch (Exception ignore) {
                    }
                }
            }
        });
        Runtime.getRuntime().addShutdownHook(new Thread(notifierExecutor::shutdown));
    }

    public boolean watch(String vGroup) {
        String namingAddr = getNamingAddr();
        String clientAddr = NetUtil.getLocalHost();
        StringBuilder watchAddrBuilder = new StringBuilder(HTTP_PREFIX)
                .append(namingAddr)
                .append("/naming/v1/watch?")
                .append(VGROUP_KEY).append("=").append(vGroup)
                .append("&").append(CLIENT_TERM_KEY).append("=").append(term)
                .append("&").append(TIME_OUT_KEY).append("=").append(LONG_POLL_TIME_OUT_PERIOD)
                .append("&clientAddr=").append(clientAddr);
        String watchAddr = watchAddrBuilder.toString();

        try (CloseableHttpResponse response = HttpClientUtil.doPost(watchAddr, (String) null, null, 30000)) {
            if (response != null) {
                StatusLine statusLine = response.getStatusLine();
                return statusLine != null && statusLine.getStatusCode() == HttpStatus.SC_OK;
            }
        } catch (Exception e) {
            LOGGER.error("watch failed: {}", e.getMessage());
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException ignored) {
            }
        }
        return false;
    }

    @Override
    public void unsubscribe(String cluster, NamingListener listener) throws Exception {

    }

    public void unsubscribe(NamingListener listener, String vGroup) throws Exception {
        // remove watchers
        List<NamingListener> listeners = LISTENER_SERVICE_MAP.get(vGroup);
        if (listeners != null) {
            listeners.remove(listener);
            if (listeners.isEmpty()) {
                LISTENER_SERVICE_MAP.remove(vGroup);
            }
        }

        // close subscribe thread
        isSubscribed = false;

    }

    public void unsubscribe(String vGroup) throws Exception {
        LISTENER_SERVICE_MAP.remove(vGroup);
        isSubscribed = false;
    }

    /**
     * @param key vGroup name
     * @return List<InetSocketAddress> available instance list
     * @throws Exception
     */


    @Override
    public List<InetSocketAddress> lookup(String key) throws Exception {
        if (!isSubscribed) {
            // get available instanceList by vGroup
            refreshGroup(key);
            // subscribe the vGroup
            subscribe(vGroup -> {
                try {
                    refreshGroup(vGroup);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }, key);
        }

        return VGROUP_ADDRESS_MAP.get(key);
    }


    public List<InetSocketAddress> refreshGroup(String vGroup) throws IOException {
        Map<String, String> paraMap = new HashMap<>();
        String namingAddr = getNamingAddr();
        paraMap.put(VGROUP_KEY, vGroup);
        paraMap.put(NAMESPACE_KEY, getNamespace());
        String url = HTTP_PREFIX + namingAddr + "/naming/v1/discovery";
        Map<String, String> header = new HashMap<>();
        header.put(HTTP.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
        try (CloseableHttpResponse response = HttpClientUtil.doGet(url, paraMap, header, 3000)) {
            if (response == null) {
                throw new NamingRegistryException("cannot lookup server list in vgroup: " + vGroup);
            }
            String jsonResponse = EntityUtils.toString(response.getEntity(), "UTF-8");
            // jsonResponse -> MetaResponse
            MetaResponse metaResponse = OBJECT_MAPPER.readValue(jsonResponse, new TypeReference<MetaResponse>() {
            });
            // MetaResponse -> endpoint list
            List<InetSocketAddress> newAddressList = metaResponse.getClusterList().stream()
                    .flatMap(cluster -> cluster.getUnitData().stream())
                    .flatMap(unit -> unit.getNamingInstanceList().stream())
                    .map(namingInstance -> new InetSocketAddress(namingInstance.getTransaction().getHost(), namingInstance.getTransaction().getPort())).collect(Collectors.toList());
            if (metaResponse.getTerm() > 0) {
                term = metaResponse.getTerm();
            }
            VGROUP_ADDRESS_MAP.put(vGroup, newAddressList);
            removeOfflineAddressesIfNecessary(vGroup,vGroup,newAddressList);
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
            throw new RemoteException();
        }

        return VGROUP_ADDRESS_MAP.get(vGroup);
    }

    @Override
    public void close() throws Exception {

    }

    @Override
    public String getServiceGroup(String key) {
        return RegistryService.super.getServiceGroup(key);
    }


    public String getNamespace() {
        String namespaceKey = String.join(FILE_CONFIG_SPLIT_CHAR, FILE_ROOT_REGISTRY, REGISTRY_TYPE, NAMESPACE_KEY);
        String namespace = FILE_CONFIG.getConfig(namespaceKey);
        if (StringUtils.isBlank(namespace)) {
            namespace = DEFAULT_NAMESPACE;
        }
        return namespace;
    }

    @Override
    public List<InetSocketAddress> aliveLookup(String transactionServiceGroup) {
        Map<String, List<InetSocketAddress>> clusterAddressMap = CURRENT_ADDRESS_MAP.computeIfAbsent(transactionServiceGroup,
            k -> new ConcurrentHashMap<>());

        List<InetSocketAddress> inetSocketAddresses = clusterAddressMap.get(transactionServiceGroup);
        if (CollectionUtils.isNotEmpty(inetSocketAddresses)) {
            return inetSocketAddresses;
        }

        // fall back to addresses of any cluster
        return clusterAddressMap.values().stream().filter(CollectionUtils::isNotEmpty)
                .findAny().orElse(Collections.emptyList());
    }

    @Override
    public List<InetSocketAddress> refreshAliveLookup(String transactionServiceGroup,
                                                      List<InetSocketAddress> aliveAddress) {
        Map<String, List<InetSocketAddress>> clusterAddressMap = CURRENT_ADDRESS_MAP.computeIfAbsent(transactionServiceGroup,
            key -> new ConcurrentHashMap<>());


        return clusterAddressMap.put(transactionServiceGroup, aliveAddress);
    }


    /**
     * get one namingserver url
     *
     * @return url
     */
    public String getNamingAddr() {
        if (namingServerAddressCache != null) {
            return namingServerAddressCache;
        }
        Map<String, AtomicInteger> availableNamingserverMap = new HashMap<>(AVAILABLE_NAMINGSERVER_MAP);
        List<String> availableNamingserverList = new ArrayList<>();
        for (Map.Entry<String, AtomicInteger> entry : availableNamingserverMap.entrySet()) {
            String namingServerAddress = entry.getKey();
            Integer numberOfFailures = entry.getValue().get();

            if (numberOfFailures < HEALTH_CHECK_THRESHOLD) {
                availableNamingserverList.add(namingServerAddress);
            }
        }
        if (availableNamingserverList.isEmpty()) {
            throw new NamingRegistryException("no available namingserver address!");
        } else {
            namingServerAddressCache = availableNamingserverList.get(ThreadLocalRandom.current().nextInt(availableNamingserverList.size()));
            return namingServerAddressCache;
        }

    }

    /**
     * get all namingserver urlList
     *
     * @return url List
     */
    public List<String> getNamingAddrs() {
        String namingAddrsKey = String.join(FILE_CONFIG_SPLIT_CHAR, FILE_ROOT_REGISTRY, REGISTRY_TYPE, NAMING_SERVICE_URL_KEY);

        String urlListStr = FILE_CONFIG.getConfig(namingAddrsKey);
        if (urlListStr.isEmpty()) {
            throw new NamingRegistryException("Naming server url can not be null!");
        }
        return Arrays.stream(urlListStr.split(",")).collect(Collectors.toList());
    }


}