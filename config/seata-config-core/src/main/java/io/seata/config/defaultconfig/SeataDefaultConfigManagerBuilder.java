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

import io.seata.common.loader.EnhancedServiceLoader;

/**
 * the type Seata default config manager builder.
 *
 * @author wang.liang
 */
public class SeataDefaultConfigManagerBuilder implements DefaultConfigManagerBuilder {

    @Override
    public DefaultConfigManager build() {
        SeataDefaultConfigManager manager = new SeataDefaultConfigManager();

        // load defaultConfigSource
        List<DefaultConfigSourceProvider> providers = EnhancedServiceLoader.loadAll(DefaultConfigSourceProvider.class);
        for (DefaultConfigSourceProvider provider : providers) {
            provider.provide(manager);
        }

        // Avoid print logs repeatedly
        manager.disablePrintGetSuccessLog();

        return manager;
    }

}