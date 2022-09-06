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
package io.seata.discovery.registry.polaris;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import io.seata.common.ConfigurationKeys;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.NetUtil;
import io.seata.common.util.StringUtils;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.discovery.registry.RegistryHeartBeats;
import io.seata.discovery.registry.RegistryService;
import io.seata.discovery.registry.polaris.client.PolarisInstance;
import io.seata.discovery.registry.polaris.client.PolarisNamingClient;
import io.seata.discovery.registry.polaris.client.PolarisNamingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link PolarisRegistryServiceImpl} Definition .
 *
 * @author <a href="mailto:iskp.me@gmail.com">Palmer Xu</a> 2022-08-23
 */
public class PolarisRegistryServiceImpl implements RegistryService<PolarisListener> {

    /**
     * Logger Instance.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PolarisRegistryServiceImpl.class);

    /**
     * Polaris Naming Client Instance of {@link PolarisNamingClient} .
     */
    private static volatile PolarisNamingClient client;

    /**
     * Polaris Registry Service Implements Instance of {@link PolarisRegistryServiceImpl}.
     */
    private static volatile PolarisRegistryServiceImpl instance;

    /**
     * Seata System Configs.
     */
    private static final Configuration FILE_CONFIG = ConfigurationFactory.CURRENT_FILE_INSTANCE;

    /**
     * Polaris Service Listener Map.
     */
    private static final ConcurrentMap<String, List<PolarisListener>> LISTENER_SERVICE_MAP = new ConcurrentHashMap<>();

    /**
     * Polaris Service Cluster Address Map.
     */
    private static final ConcurrentMap<String, List<InetSocketAddress>> CLUSTER_ADDRESS_MAP = new ConcurrentHashMap<>();

    /**
     * Sync Lock Object.
     */
    private static final Object LOCK_OBJ = new Object();

    // ~~ Polaris Naming Config Properties Key ~~

    /**
     * Polaris Discovery Typo.
     */
    private static final String REGISTRY_TYPE = "polaris";

    /**
     * Default Application Name.
     */
    private static final String DEFAULT_APPLICATION = "seata-server";

    /**
     * Polaris Default Namespace.
     */
    private static final String DEFAULT_NAMESPACE = "default";

    /**
     * Default Cluster Name.
     */
    public static final String DEFAULT_CLUSTER = "default";

    /**
     * Polaris Namespace Config Key.
     */
    private static final String POLARIS_NAMESPACE_KEY = "namespace";

    /**
     * Polaris Application Config Key.
     */
    private static final String POLARIS_APPLICATION_KEY = "application";

    /**
     * Polaris Server-Addr Config Key.
     */
    private static final String POLARIS_SERVER_KEY = "serverAddr";

    /**
     * Polaris Token Config Key.
     */
    private static final String POLARIS_SERVER_ACCESS_TOKEN = "token";

    /**
     * Polaris Connect-Timeout Config Key.
     */
    private static final String POLARIS_SERVER_CONNECT_TIME = "connectTimeout";

    /**
     * Polaris Read-Timeout Config Key.
     */
    private static final String POLARIS_SERVER_READ_TIME = "readTimeout";

    /**
     * Polaris Service Instance Refresh Time.
     */
    private static final String POLARIS_SERVER_REFRESH_TIME = "refreshTime";

    /**
     * Polaris Service Instance Refresh Period.
     */
    private static final long POLARIS_SERVICE_REFRESH_PERIOD = 2000L;

    /**
     * Gets instance.
     *
     * @return the instance
     */
    static PolarisRegistryServiceImpl getInstance() {
        if (instance == null) {
            synchronized (PolarisRegistryServiceImpl.class) {
                if (instance == null) {
                    instance = new PolarisRegistryServiceImpl();
                }
            }
        }
        return instance;
    }

    /**
     * Default private constructor for {@link PolarisRegistryServiceImpl}.
     */
    private PolarisRegistryServiceImpl() {
        if (client == null) {
            try {
                // Read polaris center config properties
                PolarisNamingProperties properties = getNamingProperties();
                // build context
                client = PolarisNamingClient.getClient(properties);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Register.
     *
     * @param address the address
     * @throws Exception the exception
     */
    @Override
    public void register(InetSocketAddress address) throws Exception {
        NetUtil.validAddress(address);
        doRegister(address);
        RegistryHeartBeats.addHeartBeat(REGISTRY_TYPE, address, POLARIS_SERVICE_REFRESH_PERIOD, this::doRegister);
    }

    private void doRegister(InetSocketAddress address) {
        client.registerInstance(getNamespaceName(), getApplicationServiceName(), address.getAddress().getHostAddress(), address.getPort(), DEFAULT_CLUSTER);
    }

    /**
     * Unregister.
     *
     * @param address the address
     * @throws Exception the exception
     */
    @Override
    public void unregister(InetSocketAddress address) throws Exception {
        NetUtil.validAddress(address);
        client.deregisterInstance(getNamespaceName(), getApplicationServiceName(), address.getAddress().getHostAddress(), address.getPort(), DEFAULT_CLUSTER);
    }

    /**
     * Subscribe.
     *
     * @param cluster  the cluster
     * @param listener the listener
     * @throws Exception the exception
     */
    @Override
    public void subscribe(String cluster, PolarisListener listener) throws Exception {
        LISTENER_SERVICE_MAP.computeIfAbsent(cluster, key -> new ArrayList<>()).add(listener);
        client.subscribe(getNamespaceName(), getApplicationServiceName(), DEFAULT_CLUSTER, listener);
    }

    /**
     * Unsubscribe.
     *
     * @param cluster  the cluster
     * @param listener the listener
     * @throws Exception the exception
     */
    @Override
    public void unsubscribe(String cluster, PolarisListener listener) throws Exception {
        List<PolarisListener> subscribeList = LISTENER_SERVICE_MAP.get(cluster);
        if (subscribeList != null) {
            List<PolarisListener> newSubscribeList = subscribeList.stream()
                .filter(eventListener -> !eventListener.equals(listener))
                .collect(Collectors.toList());
            LISTENER_SERVICE_MAP.put(cluster, newSubscribeList);
        }
        client.unsubscribe(getNamespaceName(), getApplicationServiceName(), DEFAULT_CLUSTER, listener);
    }

    /**
     * Lookup list.
     *
     * @param key the key
     * @return the list
     * @throws Exception the exception
     */
    @Override
    public List<InetSocketAddress> lookup(String key) throws Exception {

        String clusterName = getServiceGroup(key);
        if (clusterName == null) {
            return null;
        }

        // isolation with polaris service name
        final String serviceName = getApplicationServiceName(clusterName);

        if (!LISTENER_SERVICE_MAP.containsKey(serviceName)) {
            synchronized (LOCK_OBJ) {
                if (!LISTENER_SERVICE_MAP.containsKey(serviceName)) {
                    List<PolarisInstance> allInstances = client.getAllInstances(getNamespaceName(), serviceName, DEFAULT_CLUSTER);
                    // map
                    List<InetSocketAddress> newAddressList = allInstances.stream()
                        .filter(PolarisInstance::isHealthy)
                        .map(eachInstance -> new InetSocketAddress(eachInstance.getHost(), eachInstance.getPort()))
                        .collect(Collectors.toList());
                    CLUSTER_ADDRESS_MAP.put(serviceName, newAddressList);

                    // Subscribe
                    subscribe(serviceName, event -> {
                        List<PolarisInstance> instances = event.getInstances();
                        if (CollectionUtils.isEmpty(instances) && null != CLUSTER_ADDRESS_MAP.get(serviceName)) {
                            LOGGER.info("receive empty server list, service-name :{}", serviceName);
                        } else {
                            List<InetSocketAddress> newAddressList2 = instances.stream()
                                .filter(PolarisInstance::isHealthy)
                                .map(eachInstance -> new InetSocketAddress(eachInstance.getHost(), eachInstance.getPort()))
                                .collect(Collectors.toList());
                            CLUSTER_ADDRESS_MAP.put(serviceName, newAddressList2);
                        }
                    });
                }
            }
        }
        return CLUSTER_ADDRESS_MAP.get(serviceName);
    }

    /**
     * Close.
     *
     * @throws Exception maybe thrown exception.
     */
    @Override
    public void close() throws Exception {

    }

    // ~~ inner static methods

    /**
     * Build polaris naming properties from seata startup env.
     *
     * @return instance of {@link PolarisNamingProperties}
     */
    private static PolarisNamingProperties getNamingProperties() {
        PolarisNamingProperties properties = new PolarisNamingProperties();

        if (System.getProperty(POLARIS_SERVER_KEY) != null) {
            properties.address(System.getProperty(POLARIS_SERVER_KEY));
        } else {
            String address = FILE_CONFIG.getConfig(getPolarisAddrFileKey());
            if (StringUtils.isBlank(address)) {
                throw new RuntimeException("Discovery server address is blank. Please check your config .");
            }
            properties.address(address);
        }

        if (System.getProperty(POLARIS_SERVER_ACCESS_TOKEN) != null) {
            properties.token(System.getProperty(POLARIS_SERVER_ACCESS_TOKEN));
        } else {
            String token = FILE_CONFIG.getConfig(getPolarisAccessTokenKey());
            if (StringUtils.isBlank(token)) {
                throw new RuntimeException("Discovery server token is blank. Please check your config .");
            }
            properties.token(token);
        }

        if (System.getProperty(POLARIS_SERVER_CONNECT_TIME) != null) {
            properties.connectTimeout(Integer.parseInt(System.getProperty(POLARIS_SERVER_CONNECT_TIME)));
        } else {
            String connectTimeout = FILE_CONFIG.getConfig(getPolarisConnectTimeoutKey());
            if (StringUtils.isNotBlank(connectTimeout)) {
                properties.connectTimeout(Integer.parseInt(connectTimeout));
            }
        }

        if (System.getProperty(POLARIS_SERVER_READ_TIME) != null) {
            properties.readTimeout(Integer.parseInt(System.getProperty(POLARIS_SERVER_READ_TIME)));
        } else {
            String readTimeout = FILE_CONFIG.getConfig(getPolarisReadTimeoutKey());
            if (StringUtils.isNotBlank(readTimeout)) {
                properties.readTimeout(Integer.parseInt(readTimeout));
            }
        }

        // Set Default Value
        properties.refreshTime((int) POLARIS_SERVICE_REFRESH_PERIOD);
        if (System.getProperty(POLARIS_SERVER_REFRESH_TIME) != null) {
            properties.refreshTime(Integer.parseInt(System.getProperty(POLARIS_SERVER_REFRESH_TIME)));
        } else {
            String refreshTime = FILE_CONFIG.getConfig(getPolarisRefreshTimeKey());
            if (StringUtils.isNotBlank(refreshTime)) {
                properties.refreshTime(Integer.parseInt(refreshTime));
            }
        }

        return properties;
    }

    private static String getNamespaceName() {
        return FILE_CONFIG.getConfig(getPolarisNameSpaceFileKey(), DEFAULT_NAMESPACE);
    }

    private static String getApplicationServiceName() {
        return FILE_CONFIG.getConfig(getPolarisApplicationKey(), DEFAULT_APPLICATION);
    }

    private static String getApplicationServiceName(String key) {
        return String.join("#", FILE_CONFIG.getConfig(getPolarisApplicationKey(), DEFAULT_APPLICATION), key);
    }

    public static String getPolarisNameSpaceFileKey() {
        return String.join(ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR, ConfigurationKeys.FILE_ROOT_REGISTRY, REGISTRY_TYPE, POLARIS_NAMESPACE_KEY);
    }

    public static String getPolarisAddrFileKey() {
        return String.join(ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR, ConfigurationKeys.FILE_ROOT_REGISTRY, REGISTRY_TYPE, POLARIS_SERVER_KEY);
    }

    public static String getPolarisAccessTokenKey() {
        return String.join(ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR, ConfigurationKeys.FILE_ROOT_REGISTRY, REGISTRY_TYPE, POLARIS_SERVER_ACCESS_TOKEN);
    }

    public static String getPolarisApplicationKey() {
        return String.join(ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR, ConfigurationKeys.FILE_ROOT_REGISTRY, REGISTRY_TYPE, POLARIS_APPLICATION_KEY);
    }

    public static String getPolarisConnectTimeoutKey() {
        return String.join(ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR, ConfigurationKeys.FILE_ROOT_REGISTRY, REGISTRY_TYPE, POLARIS_SERVER_CONNECT_TIME);
    }

    public static String getPolarisReadTimeoutKey() {
        return String.join(ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR, ConfigurationKeys.FILE_ROOT_REGISTRY, REGISTRY_TYPE, POLARIS_SERVER_READ_TIME);
    }

    public static String getPolarisRefreshTimeKey() {
        return String.join(ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR, ConfigurationKeys.FILE_ROOT_REGISTRY, REGISTRY_TYPE, POLARIS_SERVER_REFRESH_TIME);
    }
}
