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
package org.apache.seata.spring.boot.autoconfigure;

import org.apache.seata.spring.boot.autoconfigure.properties.SeataProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link SeataProperties} enabled/disabled state.
 */
class SeataAutoConfigurationDisabledTest {

    @Test
    void seataPropertiesShouldBeEnabledByDefault() {
        SeataProperties properties = new SeataProperties();
        assertTrue(properties.isEnabled(), "Seata should be enabled by default");
    }

    @Test
    void seataPropertiesShouldBeDisableable() {
        SeataProperties properties = new SeataProperties();
        properties.setEnabled(false);
        assertFalse(properties.isEnabled(), "Seata should be disabled when set to false");
    }

    @Test
    void seataPropertiesApplicationIdShouldBeSettable() {
        SeataProperties properties = new SeataProperties();
        properties.setApplicationId("test-app");
        assertTrue("test-app".equals(properties.getApplicationId()), "Application ID should be settable");
    }

    @Test
    void seataPropertiesTxServiceGroupShouldBeSettable() {
        SeataProperties properties = new SeataProperties();
        properties.setTxServiceGroup("test-group");
        assertTrue("test-group".equals(properties.getTxServiceGroup()), "TX service group should be settable");
    }
}
