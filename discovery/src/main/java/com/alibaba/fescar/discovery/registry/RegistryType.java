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
package com.alibaba.fescar.discovery.registry;

import com.alibaba.fescar.common.exception.NotSupportYetException;

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
    Eureka;

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
        }else {
            throw new NotSupportYetException("unsupport type:" + name);
        }
    }
}
