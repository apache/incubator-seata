/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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

package com.alibaba.fescar.discovery.registry.sofa;

import com.alibaba.fescar.config.Configuration;
import com.alibaba.fescar.config.ConfigurationFactory;
import com.alibaba.fescar.discovery.registry.RegistryService;
import com.alipay.sofa.registry.client.api.RegistryClient;
import com.alipay.sofa.registry.client.api.RegistryClientConfig;
import com.alipay.sofa.registry.client.api.SubscriberDataObserver;
import com.alipay.sofa.registry.client.api.model.RegistryType;
import com.alipay.sofa.registry.client.api.model.UserData;
import com.alipay.sofa.registry.client.api.registration.PublisherRegistration;
import com.alipay.sofa.registry.client.api.registration.SubscriberRegistration;
import com.alipay.sofa.registry.client.provider.DefaultRegistryClient;
import com.alipay.sofa.registry.client.provider.DefaultRegistryClientConfigBuilder;
import com.alipay.sofa.registry.core.model.ScopeEnum;
import org.apache.commons.lang.StringUtils;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.alibaba.fescar.config.ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR;
import static com.alibaba.fescar.config.ConfigurationKeys.FILE_ROOT_REGISTRY;

/**
 * The type Nacos registry service.
 *
 * @author jimin.jm @alibaba-inc.com
 * @date 2019 /1/31
 */
public class SofaRegistryServiceImpl implements RegistryService<SubscriberDataObserver> {

    private static final String PRO_SERVER_ADDR_KEY = "serverAddr";
    private static final String PRO_REGION_KEY = "region";
    private static final String PRO_DATACENTER_KEY = "datacenter";
    private static final String PRO_GROUP_KEY = "group";
    private static final String PRO_APPLICATION_KEY = "application";
    private static final String PRO_CLUSTER_KEY = "cluster";

    private static final String REGISTRY_TYPE = "sofa";
    private static final Configuration FILE_CONFIG = ConfigurationFactory.FILE_INSTANCE;
    private static final ConcurrentMap<String, List<SubscriberDataObserver>> LISTENER_SERVICE_MAP = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, List<InetSocketAddress>> CLUSTER_ADDRESS_MAP = new ConcurrentHashMap<>();
    private static volatile SofaRegistryServiceImpl instance;

    public static final String DEFAULT_LOCAL_DATACENTER = "DefaultDataCenter";
    public static final String DEFAULT_LOCAL_REGION = "DEFAULT_ZONE";
    public static final String DEFAULT_GROUP = "SEATA";
    private static final String DEFAULT_APPLICATION = "default";
    private static final String DEFAULT_CLUSTER = "default";


    private static Properties props;


    private static volatile RegistryClient registryClient;


    private SofaRegistryServiceImpl() {
    }

    /**
     * Gets instance.
     *
     * @return the instance
     */
    static SofaRegistryServiceImpl getInstance() {
        if (null == instance) {
            synchronized (SofaRegistryServiceImpl.class) {
                if (null == instance) {
                    instance = new SofaRegistryServiceImpl();
                }
            }
        }
        return instance;
    }

    @Override
    public void register(InetSocketAddress address) throws Exception {
        validAddress(address);

        PublisherRegistration publisherRegistration;
        publisherRegistration = new PublisherRegistration(PRO_SERVER_ADDR_KEY);
        publisherRegistration.setGroup(getSofaGroupFileKey());

        String serviceData = address.getAddress().getHostAddress() + ":" + address.getPort();

        getNamingInstance().register(publisherRegistration, serviceData);
    }

    @Override
    public void unregister(InetSocketAddress address) throws Exception {
        validAddress(address);
        getNamingInstance().unregister(PRO_SERVER_ADDR_KEY, getSofaGroupFileKey(), RegistryType.PUBLISHER);
    }

    private RegistryClient getNamingInstance() {

        props = getNamingProperties();
        String address = props.getProperty(PRO_SERVER_ADDR_KEY);
        final String portStr = StringUtils.substringAfter(address, ":");

        RegistryClientConfig config = DefaultRegistryClientConfigBuilder.start()
                .setAppName(getApplicationName()).setDataCenter(DEFAULT_LOCAL_DATACENTER).setZone(DEFAULT_LOCAL_REGION)
                .setRegistryEndpoint(StringUtils.substringBefore(address, ":"))
                .setRegistryEndpointPort(Integer.parseInt(portStr)).build();

        registryClient = new DefaultRegistryClient(config);
        ((DefaultRegistryClient) registryClient).init();

        return registryClient;
    }

    @Override
    public void subscribe(String cluster, SubscriberDataObserver listener) throws Exception {
        // 生成订阅对象，并添加额外属性
        SubscriberRegistration subscriberRegistration = new SubscriberRegistration(cluster, listener);
        subscriberRegistration.setScopeEnum(ScopeEnum.global);
        subscriberRegistration.setGroup(props.getProperty(PRO_GROUP_KEY));
        getNamingInstance().register(subscriberRegistration);
    }

    @Override
    public void unsubscribe(String cluster, SubscriberDataObserver listener) throws Exception {
        getNamingInstance().unregister(cluster, props.getProperty(PRO_GROUP_KEY), RegistryType.SUBSCRIBER);
    }

    @Override
    public List<InetSocketAddress> lookup(String key) throws Exception {
        Configuration config = ConfigurationFactory.getInstance();
        String clusterName = config.getConfig(PREFIX_SERVICE_ROOT + CONFIG_SPLIT_CHAR + PREFIX_SERVICE_MAPPING + key);
        if (null == clusterName) {
            return null;
        }
        if (!LISTENER_SERVICE_MAP.containsKey(clusterName)) {
            subscribe(clusterName, new SubscriberDataObserver() {
                @Override
                public void handleData(String dataId, UserData data) {
                    Map<String, List<String>> instances = data.getZoneData();
                    if (null == instances && null != CLUSTER_ADDRESS_MAP.get(clusterName)) {
                        CLUSTER_ADDRESS_MAP.remove(clusterName);
                    } else {
                        List<InetSocketAddress> tranformData = flatData(instances);
                        List<InetSocketAddress> newAddressList = new ArrayList<>();
                        newAddressList.addAll(tranformData);
                        CLUSTER_ADDRESS_MAP.put(clusterName, newAddressList);
                    }
                }
            });
        }
        return CLUSTER_ADDRESS_MAP.get(clusterName);
    }


    private List<InetSocketAddress> flatData(Map<String, List<String>> instances) {
        List<InetSocketAddress> result = new ArrayList<InetSocketAddress>();

        for (Map.Entry<String, List<String>> entry : instances.entrySet()) {
            for (String str : entry.getValue()) {
                String ip = StringUtils.substringBefore(str, ":");
                String port = StringUtils.substringAfter(str, ":");
                InetSocketAddress inetSocketAddress = new InetSocketAddress(ip, Integer.parseInt(port));
                result.add(inetSocketAddress);
            }
        }
        return result;
    }

    @Override
    public void close() throws Exception {
    }

    private void validAddress(InetSocketAddress address) {
        if (null == address.getHostName() || 0 == address.getPort()) {
            throw new IllegalArgumentException("invalid address:" + address);
        }
    }

    private static Properties getNamingProperties() {
        Properties properties = new Properties();
        if (null != System.getProperty(PRO_SERVER_ADDR_KEY)) {
            properties.setProperty(PRO_SERVER_ADDR_KEY, System.getProperty(PRO_SERVER_ADDR_KEY));
        } else {
            String address = FILE_CONFIG.getConfig(getSofaAddrFileKey());
            if (null != address) {
                properties.setProperty(PRO_SERVER_ADDR_KEY, address);
            }
        }
        if (null != System.getProperty(PRO_REGION_KEY)) {
            properties.setProperty(PRO_REGION_KEY, System.getProperty(PRO_REGION_KEY));
        } else {
            String region = FILE_CONFIG.getConfig(getSofaRegionFileKey());
            if (null == region) {
                region = DEFAULT_LOCAL_REGION;
            }
            properties.setProperty(PRO_REGION_KEY, region);
        }

        if (null != System.getProperty(PRO_DATACENTER_KEY)) {
            properties.setProperty(PRO_DATACENTER_KEY, System.getProperty(PRO_DATACENTER_KEY));
        } else {
            String datacenter = FILE_CONFIG.getConfig(getSofaDataCenterFileKey());
            if (null == datacenter) {
                datacenter = DEFAULT_LOCAL_DATACENTER;
            }
            properties.setProperty(PRO_DATACENTER_KEY, datacenter);
        }

        if (null != System.getProperty(PRO_GROUP_KEY)) {
            properties.setProperty(PRO_GROUP_KEY, System.getProperty(PRO_GROUP_KEY));
        } else {
            String group = FILE_CONFIG.getConfig(getSofaGroupFileKey());
            if (null == group) {
                group = DEFAULT_GROUP;
            }
            properties.setProperty(PRO_GROUP_KEY, group);
        }

        if (null != System.getProperty(PRO_CLUSTER_KEY)) {
            properties.setProperty(PRO_CLUSTER_KEY, System.getProperty(PRO_CLUSTER_KEY));
        } else {
            String group = FILE_CONFIG.getConfig(getSofaClusterFileKey());
            if (null == group) {
                group = DEFAULT_CLUSTER;
            }
            properties.setProperty(PRO_CLUSTER_KEY, group);
        }

        return properties;
    }

    private static String getSofaClusterFileKey() {
        return FILE_ROOT_REGISTRY + FILE_CONFIG_SPLIT_CHAR + REGISTRY_TYPE
                + FILE_CONFIG_SPLIT_CHAR
                + PRO_CLUSTER_KEY;
    }

    private static String getSofaAddrFileKey() {
        return FILE_ROOT_REGISTRY + FILE_CONFIG_SPLIT_CHAR + REGISTRY_TYPE
                + FILE_CONFIG_SPLIT_CHAR
                + PRO_SERVER_ADDR_KEY;
    }

    private static String getSofaRegionFileKey() {
        return FILE_ROOT_REGISTRY + FILE_CONFIG_SPLIT_CHAR + REGISTRY_TYPE
                + FILE_CONFIG_SPLIT_CHAR
                + PRO_REGION_KEY;
    }

    private static String getSofaDataCenterFileKey() {
        return FILE_ROOT_REGISTRY + FILE_CONFIG_SPLIT_CHAR + REGISTRY_TYPE
                + FILE_CONFIG_SPLIT_CHAR
                + PRO_DATACENTER_KEY;
    }


    private static String getSofaGroupFileKey() {
        return FILE_ROOT_REGISTRY + FILE_CONFIG_SPLIT_CHAR + REGISTRY_TYPE
                + FILE_CONFIG_SPLIT_CHAR
                + PRO_GROUP_KEY;
    }


    private String getApplicationFileKey() {
        return FILE_ROOT_REGISTRY + FILE_CONFIG_SPLIT_CHAR + REGISTRY_TYPE + FILE_CONFIG_SPLIT_CHAR
                + PRO_APPLICATION_KEY;
    }

    private String getApplicationName() {
        String application = FILE_CONFIG.getConfig(getApplicationFileKey());
        if (null == application) {
            application = DEFAULT_APPLICATION;
        }
        return application;
    }
}
