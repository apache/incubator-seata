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
package org.apache.seata.spring.boot.autoconfigure.properties.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RoutingPropertiesTest {

    @Test
    public void testDefaultValues() {
        RoutingProperties properties = new RoutingProperties();

        assertFalse(properties.isEnabled());
        assertFalse(properties.isDebug());
        assertEquals("", properties.getRouters());
        assertNotNull(properties.getMetadataRouter());
        assertTrue(properties.getMetadataRouter().isEnabled());
        assertNotNull(properties.getNumberedMetadataRouters());
        assertTrue(properties.getNumberedMetadataRouters().isEmpty());
    }

    @Test
    public void testRoutersConfiguration() {
        RoutingProperties properties = new RoutingProperties();

        String routers = "metadata-router,metadata-router-1,spi-custom";
        properties.setRouters(routers);

        assertEquals(routers, properties.getRouters());
    }

    @Test
    public void testDefaultMetadataRouterConfiguration() {
        RoutingProperties properties = new RoutingProperties();

        // Test default metadata router
        MetadataRouterConfig defaultRouter = properties.getMetadataRouter();
        assertNotNull(defaultRouter);
        assertTrue(defaultRouter.isEnabled());
        assertEquals("", defaultRouter.getExpression());

        // Test setting default metadata router
        defaultRouter.setEnabled(false);
        defaultRouter.setExpression("env == prod");

        assertFalse(properties.getMetadataRouter().isEnabled());
        assertEquals("env == prod", properties.getMetadataRouter().getExpression());
    }

    @Test
    public void testNumberedMetadataRoutersConfiguration() {
        RoutingProperties properties = new RoutingProperties();

        MetadataRouterConfig config = new MetadataRouterConfig();
        config.setEnabled(true);
        config.setExpression("version >= 2.0");

        properties.setNumberedMetadataRouter("metadata-router-1", config);

        assertTrue(properties.getNumberedMetadataRouter("metadata-router-1").isEnabled());
        assertEquals(
                "version >= 2.0",
                properties.getNumberedMetadataRouter("metadata-router-1").getExpression());
        assertNull(properties.getNumberedMetadataRouter("metadata-router-999"));
    }

    @Test
    public void testNumberedMetadataRoutersMapOperations() {
        RoutingProperties properties = new RoutingProperties();

        assertTrue(properties.getNumberedMetadataRouters().isEmpty());
        assertFalse(properties.hasNumberedMetadataRouter("metadata-router-1"));

        MetadataRouterConfig config = new MetadataRouterConfig();
        properties.setNumberedMetadataRouter("metadata-router-1", config);

        assertTrue(properties.hasNumberedMetadataRouter("metadata-router-1"));
        assertEquals(1, properties.getNumberedMetadataRouters().size());
    }

    @Test
    public void testRoutingConfiguration() {
        RoutingProperties properties = new RoutingProperties();

        properties.setEnabled(true);
        properties.setDebug(true);

        assertTrue(properties.isEnabled());
        assertTrue(properties.isDebug());
    }
}
