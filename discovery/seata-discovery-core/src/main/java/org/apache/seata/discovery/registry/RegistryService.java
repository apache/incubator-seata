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
package org.apache.seata.discovery.registry;

import org.apache.seata.common.metadata.ServiceInstance;
import org.apache.seata.common.util.CollectionUtils;
import org.apache.seata.config.ConfigurationFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service registry interface for managing service registration and discovery.
 *
 * @param <T> the listener type parameter
 */
public interface RegistryService<T> {

    /**
     * The constant PREFIX_SERVICE_MAPPING.
     */
    String PREFIX_SERVICE_MAPPING = "vgroupMapping.";

    /**
     * The constant PREFIX_SERVICE_ROOT.
     */
    String PREFIX_SERVICE_ROOT = "service";

    /**
     * The constant CONFIG_SPLIT_CHAR.
     */
    String CONFIG_SPLIT_CHAR = ".";

    /**
     * Set of service group names.
     */
    Set<String> SERVICE_GROUP_NAME = ConcurrentHashMap.newKeySet();

    /**
     * Current instance cache map for service node health check.
     */
    Map<String, Map<String, List<ServiceInstance>>> CURRENT_INSTANCE_MAP = new ConcurrentHashMap<>();

    /**
     * Register a serviceInstance.
     *
     * @param address the serviceInstance to register
     * @throws Exception the exception
     */
    void register(ServiceInstance address) throws Exception;

    /**
     * Unregister a serviceInstance.
     *
     * @param address the service instance to unregister
     * @throws Exception the exception
     */
    void unregister(ServiceInstance address) throws Exception;

    /**
     * Subscribe to cluster changes.
     *
     * @param cluster  the cluster
     * @param listener the listener
     * @throws Exception the exception
     */
    void subscribe(String cluster, T listener) throws Exception;

    /**
     * Unsubscribe from cluster changes.
     *
     * @param cluster  the cluster
     * @param listener the listener
     * @throws Exception the exception
     */
    void unsubscribe(String cluster, T listener) throws Exception;

    /**
     * Look up serviceInstances by key.
     *
     * @param key the key
     * @return list of serviceInstances
     * @throws Exception the exception
     */
    List<ServiceInstance> lookup(String key) throws Exception;

    /**
     * Close the registry service.
     * @throws Exception the exception
     */
    void close() throws Exception;

    /**
     * Get current service group name from configuration.
     *
     * @param key service group
     * @return the service group name
     */
    default String getServiceGroup(String key) {
        key = PREFIX_SERVICE_ROOT + CONFIG_SPLIT_CHAR + PREFIX_SERVICE_MAPPING + key;
        if (!SERVICE_GROUP_NAME.contains(key)) {
            SERVICE_GROUP_NAME.add(key);
        }
        return ConfigurationFactory.getInstance().getConfig(key);
    }

    /**
     * Look up alive serviceInstances for a transaction service group.
     *
     * @param transactionServiceGroup the transaction service group
     * @return list of alive serviceInstances
     */
    default List<ServiceInstance> aliveLookup(String transactionServiceGroup) {
        Map<String, List<ServiceInstance>> clusterInstanceMap =
                CURRENT_INSTANCE_MAP.computeIfAbsent(transactionServiceGroup, key -> new ConcurrentHashMap<>());

        String clusterName = getServiceGroup(transactionServiceGroup);
        List<ServiceInstance> serviceInstances = clusterInstanceMap.get(clusterName);
        if (CollectionUtils.isNotEmpty(serviceInstances)) {
            return serviceInstances;
        }

        // fall back to addresses of any cluster
        return clusterInstanceMap.values().stream()
                .filter(CollectionUtils::isNotEmpty)
                .findAny()
                .orElse(Collections.emptyList());
    }

    /**
     * Refresh alive serviceInstances for a transaction service group.
     *
     * @param transactionServiceGroup the transaction service group
     * @param aliveInstances the alive instances to update
     * @return the previous list of instances
     */
    default List<ServiceInstance> refreshAliveLookup(
            String transactionServiceGroup, List<ServiceInstance> aliveInstances) {
        Map<String, List<ServiceInstance>> clusterInstanceMap =
                CURRENT_INSTANCE_MAP.computeIfAbsent(transactionServiceGroup, key -> new ConcurrentHashMap<>());

        String clusterName = getServiceGroup(transactionServiceGroup);

        return clusterInstanceMap.put(clusterName, aliveInstances);
    }

    /**
     * Remove offline instances if necessary by intersecting old and new instances.
     *
     * @param transactionGroupService the transaction group service
     * @param clusterName the cluster name
     * @param onlineInstances the online instances
     */
    default void removeOfflineAddressesIfNecessary(
            String transactionGroupService, String clusterName, Collection<ServiceInstance> onlineInstances) {

        Map<String, List<ServiceInstance>> clusterInstanceMap =
                CURRENT_INSTANCE_MAP.computeIfAbsent(transactionGroupService, key -> new ConcurrentHashMap<>());

        List<ServiceInstance> currentInstances = clusterInstanceMap.getOrDefault(clusterName, Collections.emptyList());

        List<ServiceInstance> serviceInstances =
                currentInstances.stream().filter(onlineInstances::contains).collect(Collectors.toList());

        // prevent empty update
        if (CollectionUtils.isNotEmpty(serviceInstances)) {
            clusterInstanceMap.put(clusterName, serviceInstances);
        }
    }
}
