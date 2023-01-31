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
import io.seata.config.DefaultValueManager;
import io.seata.config.source.DefaultValueConfigurationSourceProvider;

import static io.seata.config.source.ConfigSourceOrdered.DEFAULT_VALUE_PROPERTY_OBJECT_SOURCE_PROVIDER_ORDER;

/**
 * The type Property object default value configuration source provider.
 *
 * @author wang.liang
 */
@LoadLevel(name = "default-value-property-object", order = DEFAULT_VALUE_PROPERTY_OBJECT_SOURCE_PROVIDER_ORDER)
public class PropertyObjectDefaultValueConfigurationSourceProvider implements DefaultValueConfigurationSourceProvider {

    @Override
    public void provide(DefaultValueManager defaultValueManager) {
        defaultValueManager.addSourceLast(new PropertyObjectDefaultValueConfigurationSource());
    }
}
