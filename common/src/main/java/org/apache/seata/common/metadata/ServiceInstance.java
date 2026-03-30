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
package org.apache.seata.common.metadata;

import org.apache.seata.common.util.NetUtil;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Entity for packaging inetSocketAddress and metadata for loadBalance
 */
public class ServiceInstance {
    private InetSocketAddress address;
    private Map<String, Object> metadata;

    public ServiceInstance(InetSocketAddress address, Map<String, Object> metadata) {
        this.address = address;
        this.metadata = (metadata == null) ? new HashMap<>() : metadata;
    }

    public ServiceInstance(Instance instance) {
        this(
                new InetSocketAddress(
                        instance.getTransaction().getHost(),
                        instance.getTransaction().getPort()),
                instance.getMetadata());
    }

    public ServiceInstance(InetSocketAddress address) {
        this(address, null);
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    public void setAddress(InetSocketAddress address) {
        this.address = address;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = (metadata == null) ? new HashMap<>() : metadata;
    }

    /**
     * Converts a list of InetSocketAddress to a list of ServiceInstance.
     *
     * @param addresses list of InetSocketAddress
     * @return list of ServiceInstance
     */
    public static List<ServiceInstance> convertToServiceInstanceList(List<InetSocketAddress> addresses) {
        List<ServiceInstance> serviceInstances = new ArrayList<>();
        if (addresses != null && !addresses.isEmpty()) {
            for (InetSocketAddress address : addresses) {
                NetUtil.validAddress(address);
                serviceInstances.add(new ServiceInstance(address, null));
            }
        }
        return serviceInstances;
    }

    /**
     * Converts a set of InetSocketAddress to a set of ServiceInstance in RedisRegistryServiceImpl.
     *
     * @param addresses set of InetSocketAddress
     * @return set of ServiceInstance
     */
    public static Set<ServiceInstance> convertToServiceInstanceSet(Set<InetSocketAddress> addresses) {
        Set<ServiceInstance> serviceInstances = new HashSet<>();
        if (addresses != null && !addresses.isEmpty()) {
            for (InetSocketAddress address : addresses) {
                NetUtil.validAddress(address);
                serviceInstances.add(new ServiceInstance(address, null));
            }
        }
        return serviceInstances;
    }

    /**
     * Creates a ServiceInstance from an InetSocketAddress and a Map<String, String> of metadata.
     *
     * @param address the InetSocketAddress
     * @param stringMap the map of string metadata
     * @return a new ServiceInstance
     */
    public static ServiceInstance fromStringMap(InetSocketAddress address, Map<String, String> stringMap) {
        Map<String, Object> metadata = new HashMap<>();
        if (stringMap != null) {
            metadata.putAll(stringMap);
        }
        return new ServiceInstance(address, metadata);
    }

    /**
     * Convert metadata to Map<String, String>. Non-String values will use toString().
     *
     * @return Map<String, String>
     */
    public static Map<String, String> getStringMap(Map<String, Object> metadata) {
        Map<String, String> stringMap = new HashMap<>();
        if (metadata != null) {
            for (Map.Entry<String, Object> entry : metadata.entrySet()) {
                stringMap.put(
                        entry.getKey(),
                        entry.getValue() == null ? null : entry.getValue().toString());
            }
        }

        return stringMap;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ServiceInstance that = (ServiceInstance) o;
        return Objects.equals(address, that.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address);
    }

    @Override
    public String toString() {
        return "ServiceInstance{" + "address=" + address + ", metadata=" + metadata + '}';
    }
}
