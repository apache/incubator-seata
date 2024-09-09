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

/**
 * The enum Registry type.
 *
 */
public enum RegistryType {
    /**
     * File registry type.
     */
    File,
    /**
     * Raft registry type.
     */
    Raft,
    /**
     * ZK registry type.
     */
    ZK,
    /**
     * Redis registry type.
     */
    Redis,
    /**
     * Nacos registry type.
     */
    Nacos,
    /**
     * Eureka registry type.
     */
    Eureka,
    /**
     * Consul registry type
     */
    Consul,
    /**
     * Etcd3 registry type
     */
    Etcd3,
    /**
     * Sofa registry type
     */
    Sofa,
    /**
     * Custom registry type
     */
    Custom,
    /**
     * Seata namingServer registry type
     */
    Seata;

    /**
     * Gets type.
     *
     * @param name the name
     * @return the type
     */
    public static RegistryType getType(String name) {
        for (RegistryType registryType : RegistryType.values()) {
            if (registryType.name().equalsIgnoreCase(name)) {
                return registryType;
            }
        }
        throw new IllegalArgumentException("not support registry type: " + name);
    }
}
