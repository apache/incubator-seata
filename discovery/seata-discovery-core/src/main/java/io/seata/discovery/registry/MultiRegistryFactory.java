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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.seata.common.ConfigurationKeys;
import io.seata.common.Constants;
import io.seata.common.exception.NotSupportYetException;
import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.common.util.StringUtils;
import io.seata.config.ConfigurationFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type multiple Registry factory.
 *
 * @author liuqiufeng
 */
public class MultiRegistryFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiRegistryFactory.class);

    /**
     * Gets instances.
     *
     * @return the instance list
     */
    public static List<RegistryService> getInstances() {
        return MultiRegistryFactoryHolder.INSTANCES;
    }

    private static List<RegistryService> buildRegistryServices() {
        List<RegistryService> registryServices = new ArrayList<>();
        String registryTypeNamesStr =
            ConfigurationFactory.CURRENT_FILE_INSTANCE.getConfig(ConfigurationKeys.FILE_ROOT_REGISTRY
                + ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR + ConfigurationKeys.FILE_ROOT_TYPE);
        if (StringUtils.isBlank(registryTypeNamesStr)) {
            registryTypeNamesStr = RegistryType.File.name();
        }
        String[] registryTypeNames = registryTypeNamesStr.split(Constants.REGISTRY_TYPE_SPLIT_CHAR);
        if (registryTypeNames.length > 1) {
            LOGGER.info("use multi registry center type: {}", registryTypeNamesStr);
        }
        for (String registryTypeName : registryTypeNames) {
            RegistryType registryType;
            try {
                registryType = RegistryType.getType(registryTypeName);
            } catch (Exception exx) {
                throw new NotSupportYetException("not support registry type: " + registryTypeName);
            }
            RegistryService registryService = EnhancedServiceLoader
                .load(RegistryProvider.class, Objects.requireNonNull(registryType).name()).provide();
            registryServices.add(registryService);
        }
        return registryServices;
    }

    private static class MultiRegistryFactoryHolder {
        private static final List<RegistryService> INSTANCES = buildRegistryServices();
    }
}
