/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.server.config;


import java.util.HashMap;
import java.util.Map;

import org.apache.seata.config.dto.ConfigurationItem;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ConfigurationProcessorTest {

    @Test
    void processConfigMap() {
        String key1 = "transport.type";
        String value1 = "TCP";
        String key2 = "UNKNOWN";
        String value2 = "UNKNOWN";
        HashMap<String, Object> configMap = new HashMap<>();
        configMap.put(key1, value1);
        configMap.put(key2, value2);

        Map<String, ConfigurationItem> itemMap = ConfigurationProcessor.processConfigMap(configMap);

        ConfigurationItem item1 = itemMap.get(key1);
        ConfigurationItem item2 = itemMap.get(key2);
        Assertions.assertEquals(2, itemMap.size());
        Assertions.assertEquals(key1, item1.getKey());
        Assertions.assertEquals(value1, item1.getValue());
        Assertions.assertNotNull(item1.getDefaultValue());
        Assertions.assertNotNull(item1.getDescription());

        Assertions.assertEquals(key2, item2.getKey());
        Assertions.assertEquals(value2, item2.getValue());
        Assertions.assertNull(item2.getDefaultValue());
        Assertions.assertNotNull(item2.getDescription());

    }
}
