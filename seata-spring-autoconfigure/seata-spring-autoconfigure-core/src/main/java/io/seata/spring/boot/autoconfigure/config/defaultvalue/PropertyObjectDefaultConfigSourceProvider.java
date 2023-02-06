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
package io.seata.spring.boot.autoconfigure.config.defaultvalue;

import io.seata.common.loader.LoadLevel;
import io.seata.config.defaultconfig.DefaultConfigManager;
import io.seata.config.defaultconfig.DefaultConfigSourceProvider;

import static io.seata.config.source.ConfigSourceOrdered.PROPERTY_OBJECT_DEFAULT_CONFIG_SOURCE_ORDER;

/**
 * The type Property object default config source provider.
 *
 * @author wang.liang
 */
@LoadLevel(name = "property-object-default-config", order = PROPERTY_OBJECT_DEFAULT_CONFIG_SOURCE_ORDER)
public class PropertyObjectDefaultConfigSourceProvider implements DefaultConfigSourceProvider {

    @Override
    public void provide(DefaultConfigManager defaultConfigManager) {
        defaultConfigManager.addSource(new PropertyObjectDefaultConfigSource());
    }
}
