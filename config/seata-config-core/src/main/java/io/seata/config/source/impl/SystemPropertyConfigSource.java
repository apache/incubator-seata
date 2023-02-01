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

    private static final Properties PROPERTIES = System.getProperties();


    @Override
    public String getTypeName() {
        return "system-property";
    }

    @Override
    public Object getLatestConfig(String dataId, long timeoutMills) {
        Object config = PROPERTIES.get(dataId);
        if (ObjectUtils.isNullOrBlank(config)) {
            return config;
        }

        String propertyDataId = StringUtils.hump2Line(dataId);
        if (!propertyDataId.equals(dataId)) {
            config = PROPERTIES.get(propertyDataId);
            if (ObjectUtils.isNullOrBlank(config)) {
                return config;
            }
        }

        return null;
    }
}
