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

package com.alibaba.fescar.config;

import com.alibaba.fescar.common.exception.NotSupportYetException;

/**
 * The enum Config type.
 *
 * @author: jimin.jm @alibaba-inc.com
 * @date: 2019 /2/1
 */
public enum ConfigType {
    /**
     * File config type.
     */
    File,
    /**
     * zookeeper config type.
     */
    ZK,
    /**
     * Nacos config type.
     */
    Nacos;

    /**
     * Gets type.
     *
     * @param name the name
     * @return the type
     */
    public static ConfigType getType(String name) {
        if (File.name().equalsIgnoreCase(name)) {
            return File;
        } else if (Nacos.name().equalsIgnoreCase(name)) {
            return Nacos;
        } else if (ZK.name().equalsIgnoreCase(name)) {
            return ZK;
        } else {
            throw new NotSupportYetException("unsupport type:" + name);
        }
    }
}
