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

import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import javax.annotation.Nonnull;

import io.seata.common.util.StringUtils;
import io.seata.config.source.AbstractScheduledUpdateConfigSource;

/**
 * The type SystemPropertyConfigSource.
 *
 * @author wang.liang
 */
public class SystemPropertyConfigSource extends AbstractScheduledUpdateConfigSource {

    public static final String DEFAULT_NAME = "system-property";


    public SystemPropertyConfigSource(@Nonnull String name, boolean allowAutoUpdate, long executorServicePeriod) {
        super(name, allowAutoUpdate, executorServicePeriod);
    }

    public SystemPropertyConfigSource(@Nonnull String name, boolean allowAutoUpdate) {
        super(name, allowAutoUpdate);
    }

    public SystemPropertyConfigSource(@Nonnull String name, long executorServicePeriod) {
        super(name, executorServicePeriod);
    }

    public SystemPropertyConfigSource(@Nonnull String name, ScheduledThreadPoolExecutor executorService, long executorServicePeriod) {
        super(name, executorService, executorServicePeriod);
    }

    public SystemPropertyConfigSource(@Nonnull String name, ScheduledThreadPoolExecutor executorService) {
        super(name, executorService);
    }


    /**
     * @see super#init()
     */
    @Override
    protected void initLatestConfigCacheMap(Map<String, String> latestConfigCacheMap) {
        super.initLatestConfigCacheMap(latestConfigCacheMap);

        System.getProperties().stringPropertyNames().forEach(key -> {
            latestConfigCacheMap.put(key, System.getProperty(key));
        });
    }

    /**
     * @see super#doCheckWhetherConfigChanged(String)
     */
    @Override
    public String getLatestConfig(String dataId, long timeoutMills) {
        String config1 = System.getProperty(dataId);
        if (StringUtils.isNotBlank(config1)) {
            return config1;
        }

        String propertyDataId = StringUtils.hump2Line(dataId);
        if (!propertyDataId.equals(dataId)) {
            String config2 = System.getProperty(propertyDataId);
            if (StringUtils.isNotBlank(config2)) {
                return config2;
            }
        }

        // May be null or blank.
        return config1;
    }
}
