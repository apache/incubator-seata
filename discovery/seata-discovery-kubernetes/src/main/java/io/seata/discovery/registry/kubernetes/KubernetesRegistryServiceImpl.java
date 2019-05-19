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
package io.seata.discovery.registry.kubernetes;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.seata.common.util.NetUtil;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.discovery.registry.RegistryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author @hero_zhanghao
 * @date 2019/5/6 17:55
 **/
public class KubernetesRegistryServiceImpl  implements RegistryService<KubernetesEventListener> {

    private static final Logger LOGGER = LoggerFactory.getLogger(KubernetesRegistryServiceImpl.class);

    private static final String FILE_CONFIG_SPLIT_CHAR = ".";
    private static final String DEFAULT_SERVICE_NAME = "default";
    private static final String REGISTRY_TYPE = "kubernetes";
    private static final String CLUSTER = "serviceName";
    private static final String FILE_ROOT_REGISTRY = "registry";

    private static volatile boolean subscribeListener = false;
    private static volatile KubernetesRegistryServiceImpl instance;
    private static final int MAP_INITIAL_CAPACITY = 8;
    private static  ConcurrentMap<String, Set<InetSocketAddress>> clusterAddressMap;

    private static volatile KubernetesClient kubernetesClient;

    private KubernetesRegistryServiceImpl() {
    }


    static KubernetesRegistryServiceImpl getInstance(){
        if (null == instance) {
            synchronized (KubernetesRegistryServiceImpl.class) {
                if (null == instance) {
                    clusterAddressMap = new ConcurrentHashMap<>(MAP_INITIAL_CAPACITY);
                    kubernetesClient = new DefaultKubernetesClient();
                    instance = new KubernetesRegistryServiceImpl();
                }
            }
        }
        return instance;
    }


    @Override
    public void register(InetSocketAddress address) throws Exception {
        // No registration required
    }

    @Override
    public void unregister(InetSocketAddress address) throws Exception {

    }

    @Override
    public void subscribe(String cluster, KubernetesEventListener listener) throws Exception {
        subscribeListener = true;
        refreshCluster();
    }

    @Override
    public void unsubscribe(String cluster, KubernetesEventListener listener) throws Exception {
        subscribeListener = false;
    }

    @Override
    public List<InetSocketAddress> lookup(String key) throws Exception {
        Configuration config = ConfigurationFactory.getInstance();
        String clusterName = config.getConfig(PREFIX_SERVICE_ROOT + CONFIG_SPLIT_CHAR + PREFIX_SERVICE_MAPPING + key);
        if (null == clusterName) {
            return null;
        }
        if (subscribeListener){
           return new ArrayList(clusterAddressMap.get(clusterName));
        }else{
            Endpoints endpoints = kubernetesClient.endpoints().withName(clusterName).get();
            List<EndpointSubset> subsets = getSubsetsFromEndpoints(endpoints);
            List<InetSocketAddress> socketAddresses = new ArrayList<>();
            if (!subsets.isEmpty()) {
                subsets.forEach(subset -> {
                    List<EndpointAddress> addressList = subset.getAddresses();
                    EndpointPort endpointPort = findEndpointPort(subset);
                    addressList.forEach(address -> {
                        final String ip = address.getIp();
                        final Integer port = endpointPort.getPort();
                        socketAddresses.add(new InetSocketAddress(ip,port));
                    });
                });
            }
            return  socketAddresses;
        }
    }


    private List<EndpointSubset> getSubsetsFromEndpoints(Endpoints endpoints) {
        if (endpoints == null) {
            return new ArrayList<>();
        }
        if (endpoints.getSubsets() == null) {
            return new ArrayList<>();
        }

        return endpoints.getSubsets();
    }


    private void refreshCluster(){
        final EndpointsList endpointsList = kubernetesClient.endpoints().list();
        final List<Endpoints> endpointsItems = endpointsList.getItems();
        if (endpointsItems == null || endpointsItems.isEmpty()) {
            clusterAddressMap.clear();
            return;
        }
        endpointsItems.forEach(endpoints -> {
            final List<EndpointSubset> subsets = endpoints.getSubsets();
            final String serviceName = endpoints.getMetadata().getName();
            if (subsets != null && !subsets.isEmpty()){
                Set<InetSocketAddress> addressSet = new HashSet<>();
                subsets.forEach(endpointSubset -> {
                    final List<EndpointAddress> addresses = endpointSubset.getAddresses();
                    final EndpointPort endpointPort = findEndpointPort(endpointSubset);
                    addresses.forEach(e -> {
                        final String ip = e.getIp();
                        final Integer port = endpointPort.getPort();
                        addressSet.add(new InetSocketAddress(ip,port));
                        LOGGER.info("serviceName = {} , ip = {} , port = {} ",serviceName,ip,port);
                    });
                });
                clusterAddressMap.putIfAbsent(serviceName,addressSet);
            }
        });
    }


    private EndpointPort findEndpointPort(EndpointSubset s) {
        List<EndpointPort> ports = s.getPorts();
        EndpointPort endpointPort;
        if (ports.size() == 1) {
            endpointPort = ports.get(0);
        }else{
            endpointPort = ports.stream().findAny().orElseThrow(IllegalStateException::new);
        }
        return endpointPort;
    }

    @Override
    public void close() throws Exception {
        kubernetesClient = null;
    }


    private KubernetesClient getKubernetesClient(){
        if (null == kubernetesClient) {
            synchronized (KubernetesRegistryServiceImpl.class) {
                if (null == kubernetesClient) {
                    kubernetesClient = new DefaultKubernetesClient();
                }
            }
        }
        return kubernetesClient;
    }
}
