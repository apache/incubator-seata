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

package com.alibaba.fescar.config.registry;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.alibaba.fescar.config.Configuration;
import com.alibaba.fescar.config.ConfigurationFactory;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.EventListener;

/**
 * The type Nacos registry service.
 *
 * @Author: jimin.jm @alibaba-inc.com
 * @Project: fescar -all
 * @DateTime: 2019 /1/31 5:22 PM
 * @FileName: NacosRegistryServiceImpl
 * @Description:
 */
public class NacosRegistryServiceImpl implements RegistryService<EventListener> {

    private static final String DEFAULT_NAMESPACE = "public";
    private static final String DEFAULT_CLUSTER = "default";
    private static final String PRO_SERVER_ADDR_KEY = "serverAddr";
    private static final String PRO_NAMESPACE_KEY = "namespace";
    private static final String FILE_ROOT_REGISTRY = "registry";
    private static final String FILE_CONFIG_SPLIT_CHAR = ".";
    private static final String REGISTRY_TYPE = "nacos";
    private static final String REGISTRY_CLUSTER = "cluster";
    private static final Configuration FILE_CONFIG = ConfigurationFactory.getInstance();
    private static volatile NamingService naming;

    @Override
    public void register(InetSocketAddress address) throws Exception {
        validAddress(address);
        getNamingInstance().registerInstance(PRO_SERVER_ADDR_KEY, address.getHostName(), address.getPort(),
            getClusterName());
    }

    @Override
    public void unregister(InetSocketAddress address) throws Exception {
        validAddress(address);
        getNamingInstance().deregisterInstance(PRO_SERVER_ADDR_KEY, address.getHostName(), address.getPort(),
            getClusterName());
    }

    @Override
    public void subscribe(String cluster, EventListener listener) throws Exception {
        List<String> clusters = new ArrayList<>();
        clusters.add(cluster);
        getNamingInstance().subscribe(PRO_SERVER_ADDR_KEY, clusters, listener);
    }

    @Override
    public void unsubscribe(String cluster, EventListener listener) throws Exception {
        List<String> clusters = new ArrayList<>();
        clusters.add(cluster);
        getNamingInstance().unsubscribe(PRO_SERVER_ADDR_KEY, clusters, listener);
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
        return FILE_ROOT_REGISTRY + FILE_CONFIG_SPLIT_CHAR + REGISTRY_TYPE + FILE_CONFIG_SPLIT_CHAR
            + PRO_SERVER_ADDR_KEY;
    }

    private static String getNacosNameSpaceFileKey() {
        return FILE_ROOT_REGISTRY + FILE_CONFIG_SPLIT_CHAR + REGISTRY_TYPE + FILE_CONFIG_SPLIT_CHAR
            + PRO_NAMESPACE_KEY;
    }

    private static String getNacosClusterFileKey() {
        return FILE_ROOT_REGISTRY + FILE_CONFIG_SPLIT_CHAR + REGISTRY_TYPE + FILE_CONFIG_SPLIT_CHAR
            + REGISTRY_CLUSTER;
    }
}
