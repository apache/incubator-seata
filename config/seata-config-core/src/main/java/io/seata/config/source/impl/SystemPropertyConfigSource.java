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
package io.seata.config.source.impl;

import java.util.Properties;

import io.seata.common.util.ObjectUtils;
import io.seata.common.util.StringUtils;
import io.seata.config.source.ConfigSource;

/**
 * The type SystemPropertyConfigSource.
 *
 * @author wang.liang
 */
public class SystemPropertyConfigSource implements ConfigSource {

    private final Properties properties = System.getProperties();


    @Override
    public String getName() {
        return "system-property";
    }

    @Override
    public String getLatestConfig(String dataId, long timeoutMills) {
        String config1 = properties.getProperty(dataId);
        if (!ObjectUtils.isNullOrBlank(config1)) {
            return config1;
        }

        String propertyDataId = StringUtils.hump2Line(dataId);
        if (!propertyDataId.equals(dataId)) {
            String config2 = properties.getProperty(propertyDataId);
            if (!ObjectUtils.isNullOrBlank(config2)) {
                return config2;
            }
        }

        // May be null or blank.
        return config1;
    }
}
