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
package io.seata.config.defaultconfig;

import java.util.List;
import java.util.Map;

import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.config.CacheableConfiguration;
import io.seata.config.ConfigCache;

/**
 * The type Seata default config manager.
 *
 * @author wang.liang
 */
public class SeataDefaultConfigManager extends CacheableConfiguration
        implements DefaultConfigManager {

    public static final String DEFAULT_NAME = "seata-default-config-manager";


    public SeataDefaultConfigManager(String name, Map<String, ConfigCache> configCacheMap) {
        super(name, configCacheMap);
    }

    public SeataDefaultConfigManager(String name) {
        super(name);
    }

    public SeataDefaultConfigManager() {
        this(DEFAULT_NAME);
    }


    /**
     * Override for load the DefaultConfigSource list in this method,
     * not the ConfigSource list in the method {@link super#loadSources()}.
     */
    @Override
    protected void loadSources() {
        // Avoid print logs repeatedly.
        super.disablePrintGetSuccessLog();

        // Load the DefaultConfigSourceProvider, and provide some DefaultConfigSource.
        List<DefaultConfigSourceProvider> providers = EnhancedServiceLoader.loadAll(DefaultConfigSourceProvider.class);
        for (DefaultConfigSourceProvider provider : providers) {
            provider.provide(this);
        }
    }
}
