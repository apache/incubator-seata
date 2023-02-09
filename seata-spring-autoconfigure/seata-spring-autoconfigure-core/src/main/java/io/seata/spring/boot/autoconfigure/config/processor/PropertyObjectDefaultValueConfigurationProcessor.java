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
package io.seata.spring.boot.autoconfigure.config.processor;

import io.seata.common.loader.LoadLevel;
import io.seata.config.Configuration;
import io.seata.config.processor.ConfigurationProcessor;
import io.seata.spring.boot.autoconfigure.config.source.PropertyObjectDefaultValueConfigSource;

import static io.seata.config.processor.ConfigProcessorOrdered.PROPERTY_OBJECT_DEFAULT_VALUE_PROCESSOR_ORDER;

/**
 * The type Property object default config source provider.
 *
 * @author wang.liang
 */
@LoadLevel(name = "property-object-default-value-processor", order = PROPERTY_OBJECT_DEFAULT_VALUE_PROCESSOR_ORDER)
public class PropertyObjectDefaultValueConfigurationProcessor implements ConfigurationProcessor {

    @Override
    public void process(Configuration configuration) {
        configuration.addSource(new PropertyObjectDefaultValueConfigSource());
    }
}
