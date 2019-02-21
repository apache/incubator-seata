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
import com.alibaba.fescar.config.ConfigType;

import com.alibaba.fescar.discovery.registry.zookeeper.ZKRegisterServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.alibaba.fescar.config.ConfigurationFactory.FILE_CONFIG_SPLIT_CHAR;
import static com.alibaba.fescar.config.ConfigurationFactory.FILE_INSTANCE;
import static com.alibaba.fescar.config.ConfigurationFactory.FILE_ROOT_REGISTRY;
import static com.alibaba.fescar.config.ConfigurationFactory.FILE_ROOT_TYPE;

/**
 * The type Registry factory.
 *
 * @Author: jimin.jm @alibaba-inc.com
 * @Project: fescar -all
 * @DateTime: 2019 /2/1 5:57 PM
 * @FileName: RegistryFactory
 * @Description:
 */
public class RegistryFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(RegistryFactory.class);

    /**
     * Gets instance.
     *
     * @return the instance
     */
    public static RegistryService getInstance() {
        ConfigType configType = null;
        try {
            configType = ConfigType.getType(
                FILE_INSTANCE.getConfig(FILE_ROOT_REGISTRY + FILE_CONFIG_SPLIT_CHAR + FILE_ROOT_TYPE));
        } catch (Exception exx) {
            LOGGER.error(exx.getMessage());
        }
        RegistryService registryService;
        switch (configType) {
            case Nacos:
                registryService = NacosRegistryServiceImpl.getInstance();
                break;
            case File:
                registryService = FileRegistryServiceImpl.getInstance();
                break;
            case ZK:
                registryService = ZKRegisterServiceImpl.getInstance();
                break;
            default:
                throw new NotSupportYetException("not support register type:" + configType);
        }
        return registryService;
    }
}
