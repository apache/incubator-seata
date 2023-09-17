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
package io.seata.discovery.registry.namingserver;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.seata.common.metadata.Instance;
import io.seata.common.metadata.MetaResponse;
import io.seata.common.thread.NamedThreadFactory;
import io.seata.common.util.NetUtil;
import io.seata.common.util.StringUtils;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.core.http.HttpServlet;
import io.seata.discovery.registry.RegistryService;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


public class NamingserverRegistryServiceImpl implements RegistryService<NamingListener> {
    private static final Logger LOGGER = LoggerFactory.getLogger(NamingserverRegistryServiceImpl.class);

    public static volatile NamingserverRegistryServiceImpl instance;
    private static final String NAMESPACE_KEY = "namespace";
    private static final String VGROUP_KEY = "vGroup";
    private static final String CLIENT_TERM_KEY = "clientTerm";
    private static final String DEFAULT_NAMESPACE = "public";
    private static final String NAMING_SERVICE_URL_KEY = "server-addr";
    private static final String NAMING_SERVER_CLIENT_URL = "serverAddr";
    private static final String FILE_ROOT_REGISTRY = "registry";
    private static final String FILE_CONFIG_SPLIT_CHAR = ".";
    private static final String REGISTRY_TYPE = "namingserver";
    private static final String HTTP_PREFIX = "http://";
    private static final String TIME_OUT_KEY = "timeout";
    private static final int HEARTBEAT_PERIOD = 30 * 1000;
    private static final int PULL_PERIOD = 30 * 1000;
    private static final int LONG_POLL_TIME_OUT_PERIOD = 1000;
    private static final int THREAD_POOL_NUM = 1;
    private volatile long term = 0;
    private ScheduledFuture<?> heartBeatScheduledFuture;
    private volatile boolean isSubscribed = false;
    private static final Configuration FILE_CONFIG = ConfigurationFactory.CURRENT_FILE_INSTANCE;
    private static final ConcurrentMap<String/* vgroup */, List<InetSocketAddress>> VGROUP_ADDRESS_MAP = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String/* vgroup */, List<NamingListener>> LISTENER_SERVICE_MAP = new ConcurrentHashMap<>();
    protected final ScheduledExecutorService heartBeatExecutorService = new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("heartBeatScheduledExcuter", THREAD_POOL_NUM, true));

    private final ExecutorService notifierExecutor = new ThreadPoolExecutor(THREAD_POOL_NUM, THREAD_POOL_NUM, Integer.MAX_VALUE, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), new NamedThreadFactory("serviceNamingNotifier", THREAD_POOL_NUM));


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
        instance.setIp(address.getAddress().getHostAddress());
        instance.setPort(address.getPort());

        instance.setTimeStamp(System.currentTimeMillis());
        doRegister(instance, getNamingAddrs());

        heartBeatScheduledFuture = this.heartBeatExecutorService.scheduleAtFixedRate(() -> {
            try {
                instance.setTimeStamp(System.currentTimeMillis());
                doRegister(instance, getNamingAddrs());
            } catch (Exception e) {
                LOGGER.error("Naming server register Exception", e);
            }
        }, HEARTBEAT_PERIOD, HEARTBEAT_PERIOD, TimeUnit.MILLISECONDS);

    }

    public void doRegister(Instance instance, List<String> urlList) {
        for (String urlSuffix : urlList) {
            String url = HTTP_PREFIX + urlSuffix + "/naming/v1/register?";
            String namespace = instance.getNamespace();
            String clusterName = instance.getClusterName();
            String unit = instance.getUnit();
            String jsonBody = instance.toJsonString();
            String params = "namespace=" + namespace + "&clusterName=" + clusterName + "&unit=" + unit;
            url += params;

            HttpResponse response = HttpServlet.doPost(url, jsonBody);
            try {
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode == 200) {
                    LOGGER.info("instance has been registered successfully:{}", statusCode);
                } else {
                    LOGGER.warn("instance has been registered unsuccessfully:{}", statusCode);
                }
            } catch (Exception e) {
                LOGGER.error("instance has been registered failed in namingserver {}", url);
            }
        }
    }

    @Override
    public void unregister(InetSocketAddress address) {
        NetUtil.validAddress(address);
        Instance instance = Instance.getInstance();
        instance.setIp(address.getAddress().getHostAddress());
        instance.setPort(address.getPort());

        for (String urlSuffix : getNamingAddrs()) {
            String url = HTTP_PREFIX + urlSuffix + "/naming/v1/unregister?";
            String unit = instance.getUnit();
            String jsonBody = instance.toJsonString();
            String params = "unit=" + unit;
            url += params;
            HttpResponse response = HttpServlet.doPost(url, jsonBody);
            try {
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode == 200) {
                    LOGGER.info("instance has been unregistered successfully:{}", statusCode);
                } else {
                    LOGGER.warn("instance has been unregistered unsuccessfully:{}", statusCode);
                }
            } catch (Exception e) {
                LOGGER.error("instance has been unregistered failed in namingserver {}", url);
            }
        }
        // stop heartbeat

        if (heartBeatScheduledFuture != null && !heartBeatScheduledFuture.isCancelled()) {
            heartBeatScheduledFuture.cancel(false);
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
                            throw new RuntimeException(e);
                        }
                    }
                    currentTime = System.currentTimeMillis();
                }
            }
        });
        Runtime.getRuntime().addShutdownHook(new Thread(notifierExecutor::shutdown));
    }

    public boolean watch(String vGroup) {
        Map<String, String> param = new HashMap<>();
        param.put(VGROUP_KEY, vGroup);
        param.put(CLIENT_TERM_KEY, String.valueOf(term));
        param.put(TIME_OUT_KEY, String.valueOf(LONG_POLL_TIME_OUT_PERIOD));
        String watchAddr = HTTP_PREFIX + getNamingAddr() + "/naming/v1/watch";
        HttpResponse response = HttpServlet.doGet(watchAddr, param);
        try {
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
        return VGROUP_ADDRESS_MAP.get(key);
    }


    public List<InetSocketAddress> refreshGroup(String vGroup) throws IOException {
        Map<String, String> paraMap = new HashMap<>();
        paraMap.put(VGROUP_KEY, vGroup);
        paraMap.put(NAMESPACE_KEY, getNamespace());
        String url = HTTP_PREFIX + getNamingAddr() + "/naming/v1/discovery";
        try (CloseableHttpResponse response = HttpServlet.doGet(url, paraMap)) {
            if (response == null) {
                throw new NamingRegistryException("cannot lookup server list in vgroup: " + vGroup);
            }
            String jsonResponse = EntityUtils.toString(response.getEntity(), "UTF-8");
            response.close();
            // jsonResponse -> MetaResponse
            ObjectMapper objectMapper = new ObjectMapper();
            MetaResponse metaResponse = objectMapper.readValue(jsonResponse, new TypeReference<MetaResponse>() {
            });
            // MetaResponse -> endpoint list
            List<InetSocketAddress> newAddressList = metaResponse.getClusterList().stream().flatMap(cluster -> cluster.getUnitData().stream()).flatMap(unit -> unit.getNamingInstanceList().stream()).map(namingInstance -> new InetSocketAddress(namingInstance.getIp(), namingInstance.getPort())).collect(Collectors.toList());
            term = metaResponse.getTerm();

            VGROUP_ADDRESS_MAP.put(vGroup, newAddressList);


        } catch (IOException e) {
            e.printStackTrace();
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

    @Override
    public List<InetSocketAddress> aliveLookup(String transactionServiceGroup) {
        return RegistryService.super.aliveLookup(transactionServiceGroup);
    }

    @Override
    public List<InetSocketAddress> refreshAliveLookup(String transactionServiceGroup, List<InetSocketAddress> aliveAddress) {
        return RegistryService.super.refreshAliveLookup(transactionServiceGroup, aliveAddress);
    }


    public String getNamespace() {
        String namespaceKey = String.join(FILE_CONFIG_SPLIT_CHAR, FILE_ROOT_REGISTRY, REGISTRY_TYPE, NAMESPACE_KEY);
        String namespace = FILE_CONFIG.getConfig(namespaceKey);
        if (StringUtils.isBlank(namespace)) {
            namespace = DEFAULT_NAMESPACE;
        }
        return namespace;
    }

    /**
     * get a namingserver url config in client
     *
     * @return url
     */
    public String getNamingAddr() {
        String namingAddrKey = String.join(FILE_CONFIG_SPLIT_CHAR, FILE_ROOT_REGISTRY, REGISTRY_TYPE, NAMING_SERVER_CLIENT_URL);
        return FILE_CONFIG.getConfig(namingAddrKey);
    }

    /**
     * get all namingserver urlList config in server
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