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
package io.seata.config.custom;

import java.util.stream.Stream;

import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.common.loader.LoadLevel;
import io.seata.common.util.StringUtils;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.config.ConfigurationKeys;
import io.seata.config.ConfigurationProvider;
import io.seata.config.source.ConfigSourceType;

/**
 * @author ggndnn
 */
@LoadLevel(name = "Custom")
public class CustomConfigurationProvider implements ConfigurationProvider {
    @Override
    public Configuration provide() {
        String pathDataId = ConfigurationKeys.FILE_ROOT_CONFIG + ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR
                + ConfigSourceType.Custom.name().toLowerCase() + ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR
                + "name";
        String name = ConfigurationFactory.getInstance().getString(pathDataId);
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("name value of custom config type must not be blank");
        }
        if (Stream.of(ConfigSourceType.values())
                .anyMatch(ct -> ct.name().equalsIgnoreCase(name))) {
            throw new IllegalArgumentException(String.format("custom config type name %s is not allowed", name));
        }
        return EnhancedServiceLoader.load(ConfigurationProvider.class, name).provide();
    }
}
