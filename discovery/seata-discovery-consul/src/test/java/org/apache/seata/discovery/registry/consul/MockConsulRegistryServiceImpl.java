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
package org.apache.seata.discovery.registry.consul;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.agent.model.NewService;
import com.ecwid.consul.v1.health.HealthServicesRequest;
import com.ecwid.consul.v1.health.model.HealthService;
import org.apache.seata.common.metadata.ServiceInstance;
import org.apache.seata.common.thread.NamedThreadFactory;
import org.apache.seata.common.util.NetUtil;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.config.ConfigurationKeys;
import org.apache.seata.config.exception.ConfigNotFoundException;
import org.apache.seata.discovery.registry.RegistryHeartBeats;
import org.apache.seata.discovery.registry.RegistryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Mock implementation of ConsulRegistryServiceImpl for testing purposes.
 * Uses TTL checks instead of TCP checks to avoid connection issues in test environment.
 */
public class MockConsulRegistryServiceImpl implements RegistryService<ConsulListener> {

    private static volatile MockConsulRegistryServiceImpl instance;
    private static volatile ConsulClient client;

    private static final Logger LOGGER = LoggerFactory.getLogger(MockConsulRegistryServiceImpl.class);
    private static final Configuration FILE_CONFIG = ConfigurationFactory.CURRENT_FILE_INSTANCE;
    private static final String FILE_ROOT_REGISTRY = "registry";
    private static final String FILE_CONFIG_SPLIT_CHAR = ".";
    private static final String REGISTRY_TYPE = "consul";
    private static final String SERVER_ADDR_KEY = "serverAddr";
    private static final String REGISTRY_CLUSTER = "cluster";
    private static final String DEFAULT_CLUSTER_NAME = "default";
    private static final String SERVICE_TAG = "services";
    private static final String ACL_TOKEN = "aclToken";
    private static final String FILE_CONFIG_KEY_PREFIX =
            FILE_ROOT_REGISTRY + FILE_CONFIG_SPLIT_CHAR + REGISTRY_TYPE + FILE_CONFIG_SPLIT_CHAR;

    private ConcurrentMap<String, List<ServiceInstance>> clusterAddressMap;
    private ConcurrentMap<String, Set<ConsulListener>> listenerMap;
    private ExecutorService notifierExecutor;
    private ConcurrentMap<String, ConsulNotifier> notifiers;

    private static final int THREAD_POOL_NUM = 1;
    private static final int MAP_INITIAL_CAPACITY = 8;

    private String transactionServiceGroup;

    /**
     * default deregister critical server after
     */
    private static final String DEFAULT_DEREGISTER_TIME = "20s";
    /**
     * default watch timeout in second
     */
    private static final int DEFAULT_WATCH_TIMEOUT = 60;

    private MockConsulRegistryServiceImpl() {
        // initial the capacity with 8
        clusterAddressMap = new ConcurrentHashMap<>(MAP_INITIAL_CAPACITY);
        listenerMap = new ConcurrentHashMap<>(MAP_INITIAL_CAPACITY);
        notifiers = new ConcurrentHashMap<>(MAP_INITIAL_CAPACITY);
        notifierExecutor = new ThreadPoolExecutor(
                THREAD_POOL_NUM,
                THREAD_POOL_NUM,
                Integer.MAX_VALUE,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(),
                new NamedThreadFactory("services-consul-notifier", THREAD_POOL_NUM));
    }

    /**
     * get instance of MockConsulRegistryServiceImpl
     *
     * @return instance
     */
    static MockConsulRegistryServiceImpl getInstance() {
        if (instance == null) {
            synchronized (MockConsulRegistryServiceImpl.class) {
                if (instance == null) {
                    instance = new MockConsulRegistryServiceImpl();
                }
            }
        }
        return instance;
    }

    @Override
    public void register(ServiceInstance instance) throws Exception {
        InetSocketAddress address = instance.getAddress();
        // Skip address validation for testing
        // NetUtil.validAddress(address);

        doRegister(instance);
        // Immediately send TTL check to make service healthy
        doTtlCheck(instance);
        // Add heartbeat for re-registration and TTL check
        RegistryHeartBeats.addHeartBeat(REGISTRY_TYPE, instance, this::doRegister);
        // Add TTL check to keep service healthy
        RegistryHeartBeats.addHeartBeat(REGISTRY_TYPE, instance, 15000, this::doTtlCheck);
    }

    private void doRegister(ServiceInstance instance) {
        NewService service = createService(instance);
        getConsulClient().agentServiceRegister(service, getAclToken());
    }

    private void doTtlCheck(ServiceInstance instance) throws Exception {
        // Send TTL check to keep service healthy
        String checkId = "service:" + createServiceId(instance.getAddress());
        getConsulClient().agentCheckPass(checkId, getAclToken());
    }

    @Override
    public void unregister(ServiceInstance instance) {
        InetSocketAddress address = instance.getAddress();
        // Skip address validation for testing
        // NetUtil.validAddress(address);
        getConsulClient().agentServiceDeregister(createServiceId(address), getAclToken());
    }

    @Override
    public void subscribe(String cluster, ConsulListener listener) {
        // 1.add listener to subscribe list
        listenerMap.computeIfAbsent(cluster, key -> new HashSet<>()).add(listener);
        // 2.get healthy services
        Response<List<HealthService>> response = getHealthyServices(cluster, -1, DEFAULT_WATCH_TIMEOUT);
        // 3.get current consul index.
        Long index = response.getConsulIndex();
        ConsulNotifier notifier = notifiers.computeIfAbsent(cluster, key -> new ConsulNotifier(cluster, index));
        // 4.run notifier
        notifierExecutor.submit(notifier);
    }

    @Override
    public void unsubscribe(String cluster, ConsulListener listener) {
        // 1.remove notifier for the cluster
        ConsulNotifier notifier = notifiers.remove(cluster);
        // 2.stop the notifier
        notifier.stop();
    }

    @Override
    public List<ServiceInstance> lookup(String key) {
        transactionServiceGroup = key;
        final String cluster = getServiceGroup(key);
        if (cluster == null) {
            String missingDataId = PREFIX_SERVICE_ROOT + CONFIG_SPLIT_CHAR + PREFIX_SERVICE_MAPPING + key;
            throw new ConfigNotFoundException("%s configuration item is required", missingDataId);
        }
        return lookupByCluster(cluster);
    }

    private List<ServiceInstance> lookupByCluster(String cluster) {
        if (!listenerMap.containsKey(cluster)) {
            // 1.refresh cluster
            refreshCluster(cluster);
            // 2. subscribe
            subscribe(cluster, services -> refreshCluster(cluster, services));
        }
        return clusterAddressMap.get(cluster);
    }

    /**
     * get consul client
     *
     * @return client
     */
    private ConsulClient getConsulClient() {
        if (client == null) {
            synchronized (MockConsulRegistryServiceImpl.class) {
                if (client == null) {
                    String serverAddr = FILE_CONFIG.getConfig(FILE_CONFIG_KEY_PREFIX + SERVER_ADDR_KEY);
                    InetSocketAddress inetSocketAddress = NetUtil.toInetSocketAddress(serverAddr);
                    client = new ConsulClient(inetSocketAddress.getHostName(), inetSocketAddress.getPort());
                }
            }
        }
        return client;
    }

    /**
     * get cluster name , this function is only on the server use
     *
     * @return
     */
    private String getClusterName() {
        String clusterConfigName =
                String.join(FILE_CONFIG_SPLIT_CHAR, FILE_ROOT_REGISTRY, REGISTRY_TYPE, REGISTRY_CLUSTER);
        return FILE_CONFIG.getConfig(clusterConfigName, DEFAULT_CLUSTER_NAME);
    }

    /**
     * create serviceId
     *
     * @param address
     * @return serviceId
     */
    private String createServiceId(InetSocketAddress address) {
        return getClusterName() + "-" + NetUtil.toStringAddress(address);
    }

    /**
     * get consul acl-token
     *
     * @return acl-token
     */
    private static String getAclToken() {
        String fileConfigKey = String.join(
                ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR,
                ConfigurationKeys.FILE_ROOT_REGISTRY,
                REGISTRY_TYPE,
                ACL_TOKEN);
        String aclToken = StringUtils.isNotBlank(System.getProperty(ACL_TOKEN))
                ? System.getProperty(ACL_TOKEN)
                : FILE_CONFIG.getConfig(fileConfigKey);
        return StringUtils.isNotBlank(aclToken) ? aclToken : null;
    }

    /**
     * create a new service
     *
     * @param instance
     * @return newService
     */
    private NewService createService(ServiceInstance instance) {
        InetSocketAddress address = instance.getAddress();

        NewService newService = new NewService();
        newService.setId(createServiceId(address));
        newService.setName(getClusterName());
        newService.setTags(Collections.singletonList(SERVICE_TAG));
        newService.setPort(address.getPort());
        newService.setAddress(NetUtil.toIpAddress(address));
        newService.setCheck(createCheck(address));
        newService.setMeta(ServiceInstance.getStringMap(instance.getMetadata()));
        return newService;
    }

    /**
     * create service check based on TTL (for testing purposes)
     * This allows the service to be considered healthy without actually running on the port
     *
     * @param address
     * @return
     */
    private NewService.Check createCheck(InetSocketAddress address) {
        NewService.Check check = new NewService.Check();
        // Use TTL check instead of TCP check for testing
        check.setTtl("30s");
        check.setDeregisterCriticalServiceAfter(DEFAULT_DEREGISTER_TIME);
        return check;
    }

    /**
     * get healthy services
     *
     * @param service
     * @return
     */
    private Response<List<HealthService>> getHealthyServices(String service, long index, long watchTimeout) {
        return getConsulClient()
                .getHealthServices(
                        service,
                        HealthServicesRequest.newBuilder()
                                .setTag(SERVICE_TAG)
                                .setQueryParams(new QueryParams(watchTimeout, index))
                                .setPassing(true)
                                .setToken(getAclToken())
                                .build());
    }

    /**
     * refresh cluster
     *
     * @param cluster
     */
    private void refreshCluster(String cluster) {
        if (StringUtils.isBlank(cluster)) {
            return;
        }
        Response<List<HealthService>> response = getHealthyServices(cluster, -1, -1);
        if (response == null) {
            return;
        }
        refreshCluster(cluster, response.getValue());
    }

    /**
     * refresh cluster
     *
     * @param cluster
     * @param services
     */
    private void refreshCluster(String cluster, List<HealthService> services) {
        if (cluster == null || services == null) {
            return;
        }

        List<ServiceInstance> instances = services.stream()
                .map(HealthService::getService)
                .map(service -> {
                    InetSocketAddress address = new InetSocketAddress(service.getAddress(), service.getPort());
                    Map<String, Object> metadata = new HashMap<>();
                    if (service.getMeta() != null) {
                        metadata.putAll(service.getMeta());
                    }
                    return new ServiceInstance(address, metadata);
                })
                .collect(Collectors.toList());

        clusterAddressMap.put(cluster, instances);

        removeOfflineAddressesIfNecessary(transactionServiceGroup, cluster, instances);
    }

    /**
     * consul notifier
     */
    private class ConsulNotifier implements Runnable {

        private String cluster;
        private long consulIndex;
        private boolean running;
        private boolean hasError = false;

        ConsulNotifier(String cluster, long consulIndex) {
            this.cluster = cluster;
            this.consulIndex = consulIndex;
            this.running = true;
        }

        @Override
        public void run() {
            while (this.running) {
                try {
                    processService();
                } catch (Exception exception) {
                    hasError = true;
                    LOGGER.error("consul refresh services error:{}", exception.getMessage());
                }
            }
        }

        private void processService() {
            Response<List<HealthService>> response = getHealthyServices(cluster, consulIndex, DEFAULT_WATCH_TIMEOUT);
            Long currentIndex = response.getConsulIndex();

            if ((currentIndex != null && currentIndex > consulIndex) || hasError) {
                hasError = false;
                List<HealthService> services = response.getValue();
                consulIndex = currentIndex; /*lgtm[java/dereferenced-value-may-be-null]*/
                for (ConsulListener listener : listenerMap.get(cluster)) {
                    listener.onEvent(services);
                }
            }
        }

        void stop() {
            this.running = false;
        }
    }

    @Override
    public void close() throws Exception {
        notifiers.values().forEach(ConsulNotifier::stop);
        notifiers.clear();

        // Shut down the ThreadPoolExecutor
        if (notifierExecutor != null && !notifierExecutor.isShutdown()) {
            notifierExecutor.shutdown();
            try {
                if (!notifierExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    notifierExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                notifierExecutor.shutdownNow();
            } finally {
                notifierExecutor = null;
            }
        }

        RegistryHeartBeats.close(REGISTRY_TYPE);
    }
}
