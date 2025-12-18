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
package io.seata.spring.boot.autoconfigure;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link StarterConstants}.
 */
class StarterConstantsTest {

    @Test
    void testSeataPrefixConstant() {
        assertEquals("seata", StarterConstants.SEATA_PREFIX);
    }

    @Test
    void testPropertyBeanMapConstant() {
        assertNotNull(StarterConstants.PROPERTY_BEAN_MAP);
    }

    @Test
    void testServicePrefixConstant() {
        assertEquals("seata.service", StarterConstants.SERVICE_PREFIX);
    }

    @Test
    void testClientPrefixConstant() {
        assertEquals("seata.client", StarterConstants.CLIENT_PREFIX);
    }

    @Test
    void testTransportPrefixConstant() {
        assertEquals("seata.transport", StarterConstants.TRANSPORT_PREFIX);
    }

    @Test
    void testConfigPrefixConstant() {
        assertEquals("seata.config", StarterConstants.CONFIG_PREFIX);
    }

    @Test
    void testRegistryPrefixConstant() {
        assertEquals("seata.registry", StarterConstants.REGISTRY_PREFIX);
    }
}
