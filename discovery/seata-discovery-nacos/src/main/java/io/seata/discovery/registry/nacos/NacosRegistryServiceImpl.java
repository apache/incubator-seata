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
package io.seata.discovery.registry.nacos;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.config.ConfigurationKeys;
import io.seata.discovery.registry.RegistryService;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.Event;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.client.naming.utils.CollectionUtils;

/**
 * The type Nacos registry service.
 *
 * @author jimin.jm @alibaba-inc.com
 * @date 2019 /1/31
 */
public class NacosRegistryServiceImpl implements RegistryService<EventListener> {
    private static final String DEFAULT_NAMESPACE = "public";
    private static final String DEFAULT_CLUSTER = "default";
    private static final String PRO_SERVER_ADDR_KEY = "serverAddr";
    private static final String PRO_NAMESPACE_KEY = "namespace";
    private static final String REGISTRY_TYPE = "nacos";
    private static final String REGISTRY_CLUSTER = "cluster";
    private static final Configuration FILE_CONFIG = ConfigurationFactory.CURRENT_FILE_INSTANCE;
    private static volatile NamingService naming;
    private static final ConcurrentMap<String, List<EventListener>> LISTENER_SERVICE_MAP = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, List<InetSocketAddress>> CLUSTER_ADDRESS_MAP = new ConcurrentHashMap<>();
    private static volatile NacosRegistryServiceImpl instance;

    private NacosRegistryServiceImpl() {
    }

    /**
     * Gets instance.
     *
     * @return the instance
     */
    static NacosRegistryServiceImpl getInstance() {
        if (null == instance) {
            synchronized (NacosRegistryServiceImpl.class) {
                if (null == instance) {
                    instance = new NacosRegistryServiceImpl();
                }
            }
        }
        return instance;
    }

    @Override
    public void register(InetSocketAddress address) throws Exception {
        validAddress(address);
        getNamingInstance().registerInstance(PRO_SERVER_ADDR_KEY, address.getAddress().getHostAddress(), address.getPort(), getClusterName());
    }

    @Override
    public void unregister(InetSocketAddress address) throws Exception {
        validAddress(address);
        getNamingInstance().deregisterInstance(PRO_SERVER_ADDR_KEY, address.getAddress().getHostAddress(), address.getPort(), getClusterName());
    }

    @Override
    public void subscribe(String cluster, EventListener listener) throws Exception {
        List<String> clusters = new ArrayList<>();
        clusters.add(cluster);
        LISTENER_SERVICE_MAP.putIfAbsent(cluster, new ArrayList<>());
        LISTENER_SERVICE_MAP.get(cluster).add(listener);
        getNamingInstance().subscribe(PRO_SERVER_ADDR_KEY, clusters, listener);
    }

    @Override
    public void unsubscribe(String cluster, EventListener listener) throws Exception {
        List<String> clusters = new ArrayList<>();
        clusters.add(cluster);
        List<EventListener> subscribeList = LISTENER_SERVICE_MAP.get(cluster);
        if (null != subscribeList) {
            List<EventListener> newSubscribeList = new ArrayList<>();
            for (EventListener eventListener : subscribeList) {
                if (!eventListener.equals(listener)) {
                    newSubscribeList.add(eventListener);
                }
            }
            LISTENER_SERVICE_MAP.put(cluster, newSubscribeList);
        }
        getNamingInstance().unsubscribe(PRO_SERVER_ADDR_KEY, clusters, listener);
    }

    @Override
    public List<InetSocketAddress> lookup(String key) throws Exception {
        Configuration config = ConfigurationFactory.getInstance();
        String clusterName = config.getConfig(PREFIX_SERVICE_ROOT + CONFIG_SPLIT_CHAR + PREFIX_SERVICE_MAPPING + key);
        if (null == clusterName) {
            return null;
        }
        if (!LISTENER_SERVICE_MAP.containsKey(clusterName)) {
            List<String> clusters = new ArrayList<>();
            clusters.add(clusterName);
            List<Instance> firstAllInstances = getNamingInstance().getAllInstances(PRO_SERVER_ADDR_KEY, clusters);
            if (null != firstAllInstances) {
                List<InetSocketAddress> newAddressList = new ArrayList<>();
                for (Instance instance : firstAllInstances) {
                    if (instance.isEnabled() && instance.isHealthy()) {
                        newAddressList.add(new InetSocketAddress(instance.getIp(), instance.getPort()));
                    }
                }
                CLUSTER_ADDRESS_MAP.put(clusterName, newAddressList);
            }
            subscribe(clusterName, new EventListener() {
                @Override
                public void onEvent(Event event) {
                    List<Instance> instances = ((NamingEvent) event).getInstances();
                    if (null == instances && null != CLUSTER_ADDRESS_MAP.get(clusterName)) {
                        CLUSTER_ADDRESS_MAP.remove(clusterName);
                    } else if (!CollectionUtils.isEmpty(instances)) {
                        List<InetSocketAddress> newAddressList = new ArrayList<>();
                        for (Instance instance : instances) {
                            if (instance.isEnabled() && instance.isHealthy()) {
                                newAddressList.add(new InetSocketAddress(instance.getIp(), instance.getPort()));
                            }
                        }
                        CLUSTER_ADDRESS_MAP.put(clusterName, newAddressList);
                    }
                }
            });
        }
        return CLUSTER_ADDRESS_MAP.get(clusterName);
    }

    @Override
    public void close() throws Exception {

    }

    private void validAddress(InetSocketAddress address) {
        if (null == address.getHostName() || 0 == address.getPort()) {
            throw new IllegalArgumentException("invalid address:" + address);
        }
    }

    /**
     * Gets naming instance.
     *
     * @return the naming instance
     * @throws Exception the exception
     */
    public static NamingService getNamingInstance() throws Exception {
        if (null == naming) {
            synchronized (NacosRegistryServiceImpl.class) {
                if (null == naming) {
                    naming = NamingFactory.createNamingService(getNamingProperties());
                }
            }
        }
        return naming;
    }

    private static Properties getNamingProperties() {
        Properties properties = new Properties();
        if (null != System.getProperty(PRO_SERVER_ADDR_KEY)) {
            properties.setProperty(PRO_SERVER_ADDR_KEY, System.getProperty(PRO_SERVER_ADDR_KEY));
        } else {
            String address = FILE_CONFIG.getConfig(getNacosAddrFileKey());
            if (null != address) {
                properties.setProperty(PRO_SERVER_ADDR_KEY, address);
            }
        }
        if (null != System.getProperty(PRO_NAMESPACE_KEY)) {
            properties.setProperty(PRO_NAMESPACE_KEY, System.getProperty(PRO_NAMESPACE_KEY));
        } else {
            String namespace = FILE_CONFIG.getConfig(getNacosNameSpaceFileKey());
            if (null == namespace) {
                namespace = DEFAULT_NAMESPACE;
            }
            properties.setProperty(PRO_NAMESPACE_KEY, namespace);
        }
        return properties;
    }

    private static String getClusterName() {
        String cluster = FILE_CONFIG.getConfig(getNacosClusterFileKey());
        if (null == cluster) {
            cluster = DEFAULT_CLUSTER;
        }
        return cluster;
    }

    private static String getNacosAddrFileKey() {
        return ConfigurationKeys.FILE_ROOT_REGISTRY + ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR + REGISTRY_TYPE
            + ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR
            + PRO_SERVER_ADDR_KEY;
    }

    private static String getNacosNameSpaceFileKey() {
        return ConfigurationKeys.FILE_ROOT_REGISTRY + ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR + REGISTRY_TYPE
            + ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR
            + PRO_NAMESPACE_KEY;
    }

    private static String getNacosClusterFileKey() {
        return ConfigurationKeys.FILE_ROOT_REGISTRY + ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR + REGISTRY_TYPE
            + ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR
            + REGISTRY_CLUSTER;
    }
}
