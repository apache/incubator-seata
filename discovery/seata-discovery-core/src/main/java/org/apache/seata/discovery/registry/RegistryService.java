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

import org.apache.seata.common.util.CollectionUtils;
import org.apache.seata.config.ConfigurationFactory;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * The interface Registry service.
 *
 * @param <T> the type parameter
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

    Set<String> SERVICE_GROUP_NAME = new HashSet<>();

    /**
     * Service node health check
     */
    Map<String, Map<String, List<InetSocketAddress>>> CURRENT_ADDRESS_MAP = new ConcurrentHashMap<>();
    /**
     * Register.
     *
     * @param address the address
     * @throws Exception the exception
     */
    void register(InetSocketAddress address) throws Exception;

    /**
     * Unregister.
     *
     * @param address the address
     * @throws Exception the exception
     */
    void unregister(InetSocketAddress address) throws Exception;

    /**
     * Subscribe.
     *
     * @param cluster  the cluster
     * @param listener the listener
     * @throws Exception the exception
     */
    void subscribe(String cluster, T listener) throws Exception;

    /**
     * Unsubscribe.
     *
     * @param cluster  the cluster
     * @param listener the listener
     * @throws Exception the exception
     */
    void unsubscribe(String cluster, T listener) throws Exception;

    /**
     * Lookup list.
     *
     * @param key the key
     * @return the list
     * @throws Exception the exception
     */
    List<InetSocketAddress> lookup(String key) throws Exception;

    /**
     * Close.
     * @throws Exception the exception
     */
    void close() throws Exception;

    /**
     * Get current service group name
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

    default List<InetSocketAddress> aliveLookup(String transactionServiceGroup) {
        Map<String, List<InetSocketAddress>> clusterAddressMap = CURRENT_ADDRESS_MAP.computeIfAbsent(transactionServiceGroup,
            k -> new ConcurrentHashMap<>());

        String clusterName = getServiceGroup(transactionServiceGroup);
        List<InetSocketAddress> inetSocketAddresses = clusterAddressMap.get(clusterName);
        if (CollectionUtils.isNotEmpty(inetSocketAddresses)) {
            return inetSocketAddresses;
        }

        // fall back to addresses of any cluster
        return clusterAddressMap.values().stream().filter(CollectionUtils::isNotEmpty)
                .findAny().orElse(Collections.emptyList());
    }

    default List<InetSocketAddress> refreshAliveLookup(String transactionServiceGroup,
        List<InetSocketAddress> aliveAddress) {

        Map<String, List<InetSocketAddress>> clusterAddressMap = CURRENT_ADDRESS_MAP.computeIfAbsent(transactionServiceGroup,
            key -> new ConcurrentHashMap<>());

        String clusterName = getServiceGroup(transactionServiceGroup);

        return clusterAddressMap.put(clusterName, aliveAddress);
    }


    /**
     *
     * remove offline addresses if necessary.
     *
     * Intersection of the old and new addresses
     *
     * @param clusterName
     * @param newAddressed
     */
    default void removeOfflineAddressesIfNecessary(String transactionGroupService, String clusterName, Collection<InetSocketAddress> newAddressed) {

        Map<String, List<InetSocketAddress>> clusterAddressMap = CURRENT_ADDRESS_MAP.computeIfAbsent(transactionGroupService,
            key -> new ConcurrentHashMap<>());

        List<InetSocketAddress> currentAddresses = clusterAddressMap.getOrDefault(clusterName, Collections.emptyList());

        List<InetSocketAddress> inetSocketAddresses = currentAddresses
                .stream().filter(newAddressed::contains).collect(
                        Collectors.toList());

        // prevent empty update
        if (CollectionUtils.isNotEmpty(inetSocketAddresses)) {
            clusterAddressMap.put(clusterName, inetSocketAddresses);
        }
    }

}
