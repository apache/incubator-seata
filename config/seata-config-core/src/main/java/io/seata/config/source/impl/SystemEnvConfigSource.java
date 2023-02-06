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

import javax.annotation.Nonnull;

import io.seata.common.util.StringUtils;
import io.seata.config.source.ConfigSource;

import static io.seata.config.processor.ConfigProcessorOrdered.SYSTEM_ENV_PROCESSOR_ORDER;

/**
 * The type SystemEnvConfigSource.
 *
 * @author wang.liang
 */
public class SystemEnvConfigSource implements ConfigSource {

    private final Map<String, String> env = System.getenv();


    @Override
    public String getLatestConfig(String dataId, long timeoutMills) {
        String config1 = env.get(dataId);
        if (!StringUtils.isNotBlank(config1)) {
            return config1;
        }

        if (dataId.contains(".")) {
            String envDataId = dataId.toUpperCase().replace(".", "_");
            String config2 = env.get(envDataId);
            if (!StringUtils.isNotBlank(config2)) {
                return config2;
            }
        }

        // May be null or blank.
        return config1;
    }

    @Nonnull
    @Override
    public String getName() {
        return "system-env";
    }

    @Override
    public int getOrder() {
        return SYSTEM_ENV_PROCESSOR_ORDER;
    }
}
