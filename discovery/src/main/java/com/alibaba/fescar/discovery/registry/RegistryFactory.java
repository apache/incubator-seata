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
import com.alibaba.fescar.config.ConfigurationFactory;
import com.alibaba.fescar.config.ConfigurationKeys;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Registry factory.
 *
 * @author jimin.jm @alibaba-inc.com
 * @date 2019 /2/1
 */
public class RegistryFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(RegistryFactory.class);

    /**
     * Gets instance.
     *
     * @return the instance
     */
    public static RegistryService getInstance() {
        RegistryType registryType = null;
        try {
            registryType = RegistryType.getType(
                ConfigurationFactory.FILE_INSTANCE.getConfig(
                    ConfigurationKeys.FILE_ROOT_REGISTRY + ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR
                        + ConfigurationKeys.FILE_ROOT_TYPE));
        } catch (Exception exx) {
            LOGGER.error(exx.getMessage());
        }
        RegistryService registryService;
        switch (registryType) {
            case Nacos:
                registryService = NacosRegistryServiceImpl.getInstance();
                break;
            case Redis:
                registryService = RedisRegistryServiceImpl.getInstance();
                break;
            case Eureka:
                registryService = EurekaRegistryServiceImpl.getInstance();
                break;
            case File:
                registryService = FileRegistryServiceImpl.getInstance();
                break;
            default:
                throw new NotSupportYetException("not support register type:" + registryType);

        }
        return registryService;
    }
}
