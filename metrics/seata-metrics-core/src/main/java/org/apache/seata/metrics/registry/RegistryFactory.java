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
package org.apache.seata.metrics.registry;

import java.util.Objects;

import org.apache.seata.common.exception.NotSupportYetException;
import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.core.constants.ConfigurationKeys;

import static org.apache.seata.common.DefaultValues.DEFAULT_METRICS_REGISTRY_TYPE;

/**
 * Registry Factory for load configured metrics registry
 *
 */
public class RegistryFactory {
    public static Registry getInstance() {
        RegistryType registryType;
        String registryTypeName = ConfigurationFactory.getInstance().getConfig(
            ConfigurationKeys.METRICS_PREFIX + ConfigurationKeys.METRICS_REGISTRY_TYPE, DEFAULT_METRICS_REGISTRY_TYPE);
        if (!StringUtils.isNullOrEmpty(registryTypeName)) {
            try {
                registryType = RegistryType.getType(registryTypeName);
            } catch (Exception exx) {
                throw new NotSupportYetException("not support metrics registry type: " + registryTypeName);
            }
            return EnhancedServiceLoader.load(Registry.class, Objects.requireNonNull(registryType).getName());
        }
        return null;
    }
}
