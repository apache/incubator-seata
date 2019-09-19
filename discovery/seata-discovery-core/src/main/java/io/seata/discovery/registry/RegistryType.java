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
package io.seata.discovery.registry;

import io.seata.common.exception.NotSupportYetException;

/**
 * The enum Registry type.
 *
 * @author jimin.jm @alibaba-inc.com
 * @date 2019 /02/26
 */
public enum RegistryType {
    /**
     * File registry type.
     */
    File,
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
     * Sofa registry type
     */
    Custom;

    /**
     * Gets type.
     *
     * @param name the name
     * @return the type
     */
    public static RegistryType getType(String name) {
        if (File.name().equalsIgnoreCase(name)) {
            return File;
        } else if (Nacos.name().equalsIgnoreCase(name)) {
            return Nacos;
        } else if (Redis.name().equalsIgnoreCase(name)) {
            return Redis;
        } else if (Eureka.name().equalsIgnoreCase(name)) {
            return Eureka;
        } else if (ZK.name().equalsIgnoreCase(name)) {
            return ZK;
        } else if (Consul.name().equalsIgnoreCase(name)) {
            return Consul;
        } else if (Etcd3.name().equalsIgnoreCase(name)) {
            return Etcd3;
        } else if (Sofa.name().equalsIgnoreCase(name)) {
            return Sofa;
        } else if (Custom.name().equalsIgnoreCase(name)) {
            return Custom;
        } else {
            throw new NotSupportYetException("unsupported type:" + name);
        }
    }
}
