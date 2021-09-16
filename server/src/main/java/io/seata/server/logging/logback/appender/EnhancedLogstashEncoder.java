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
package io.seata.server.logging.logback.appender;

import java.util.ArrayList;

import net.logstash.logback.composite.JsonProvider;
import net.logstash.logback.composite.JsonProviders;
import net.logstash.logback.encoder.LogstashEncoder;

/**
 * The type Enhanced logstash encoder
 *
 * @author wang.liang
 * @since 1.5.0
 */
public class EnhancedLogstashEncoder extends LogstashEncoder {

    /**
     * set exclude provider
     *
     * @param excludedProviderClassName the excluded provider class name
     */
    public void setExcludeProvider(String excludedProviderClassName) {
        JsonProviders<?> providers = getFormatter().getProviders();
        for (JsonProvider<?> provider : new ArrayList<>(providers.getProviders())) {
            if (provider.getClass().getName().equals(excludedProviderClassName)) {
                providers.removeProvider((JsonProvider) provider);
            }
        }
    }
}
