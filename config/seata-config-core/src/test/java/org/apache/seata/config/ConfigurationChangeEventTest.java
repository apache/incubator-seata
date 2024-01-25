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
package org.apache.seata.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class ConfigurationChangeEventTest {

    @Test
    void getDataId() {
        ConfigurationChangeEvent event = new ConfigurationChangeEvent();
        event.setDataId("dataId");
        Assertions.assertEquals("dataId", event.getDataId());
    }

    @Test
    void getOldValue() {
        ConfigurationChangeEvent event = new ConfigurationChangeEvent();
        event.setOldValue("oldValue");
        Assertions.assertEquals("oldValue", event.getOldValue());
    }

    @Test
    void getNewValue() {
        ConfigurationChangeEvent event = new ConfigurationChangeEvent();
        event.setNewValue("newValue");
        Assertions.assertEquals("newValue", event.getNewValue());
    }

    @Test
    void getChangeType() {
        ConfigurationChangeEvent event = new ConfigurationChangeEvent();
        event.setChangeType(ConfigurationChangeType.ADD);
        Assertions.assertEquals(ConfigurationChangeType.ADD, event.getChangeType());
    }

    @Test
    void getNamespace() {
        ConfigurationChangeEvent event = new ConfigurationChangeEvent();
        event.setNamespace("namespace");
        Assertions.assertEquals("namespace", event.getNamespace());
    }
}
