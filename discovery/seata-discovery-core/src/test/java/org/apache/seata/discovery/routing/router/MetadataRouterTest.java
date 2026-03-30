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
package org.apache.seata.discovery.routing.router;

import org.apache.seata.common.metadata.ServiceInstance;
import org.apache.seata.discovery.routing.RoutingContext;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MetadataRouterTest {

    /**
     * Test constructor
     */
    @Test
    public void testConstructor() {
        // Constructor with router name
        MetadataRouter router = new MetadataRouter("metadata-router-1");
        assertNotNull(router);
    }

    /**
     * Test set and get expression
     */
    @Test
    public void testSetAndGetExpression() {
        MetadataRouter router = new MetadataRouter("metadata-router-1");
        router.setExpression("version >= 2.0");
        assertEquals("version >= 2.0", router.getExpression());
    }

    /**
     * Test build snapshot
     */
    @Test
    public void testBuildSnapshot() {
        MetadataRouter router = new MetadataRouter("metadata-router-1");
        router.setExpression("version >= 2.0");
        String snapshot = router.buildSnapshot();
        assertTrue(snapshot.contains("MetadataRouter"));
        assertTrue(snapshot.contains("version >= 2.0"));
    }

    /**
     * Test empty expression - should return original server list
     */
    @Test
    public void testDoRouteWithEmptyExpression() {
        MetadataRouter router = new MetadataRouter("metadata-router-1");
        router.setExpression("");

        ServiceInstance server = mock(ServiceInstance.class);
        List<ServiceInstance> servers = Collections.singletonList(server);
        RoutingContext ctx = new RoutingContext();

        List<ServiceInstance> result = router.doRoute(servers, ctx);
        assertEquals(servers, result);
    }

    /**
     * Test single expression - servers satisfying conditions pass through
     */
    @Test
    public void testDoRouteWithSingleExpression() {
        MetadataRouter router = new MetadataRouter("metadata-router-1");
        router.setExpression("version >= 2.0");

        ServiceInstance server1 = mock(ServiceInstance.class);
        ServiceInstance server2 = mock(ServiceInstance.class);
        Map<String, Object> metadata1 = new HashMap<>();
        Map<String, Object> metadata2 = new HashMap<>();
        metadata1.put("version", "2.5");
        metadata2.put("version", "1.5");
        when(server1.getMetadata()).thenReturn(metadata1);
        when(server2.getMetadata()).thenReturn(metadata2);

        List<ServiceInstance> servers = Arrays.asList(server1, server2);
        RoutingContext ctx = new RoutingContext();

        List<ServiceInstance> result = router.doRoute(servers, ctx);
        assertEquals(1, result.size());
        assertTrue(result.contains(server1));
    }

    /**
     * Test OR expression - servers satisfying any condition pass through
     */
    @Test
    public void testDoRouteWithOrExpression() {
        MetadataRouter router = new MetadataRouter("metadata-router-1");
        router.setExpression("(version >= 2.0) || (env = dev)");

        ServiceInstance server1 = mock(ServiceInstance.class);
        ServiceInstance server2 = mock(ServiceInstance.class);
        ServiceInstance server3 = mock(ServiceInstance.class);
        Map<String, Object> metadata1 = new HashMap<>();
        Map<String, Object> metadata2 = new HashMap<>();
        Map<String, Object> metadata3 = new HashMap<>();
        metadata1.put("version", "2.5");
        metadata2.put("env", "dev");
        metadata3.put("version", "1.5");
        metadata3.put("env", "prod");
        when(server1.getMetadata()).thenReturn(metadata1);
        when(server2.getMetadata()).thenReturn(metadata2);
        when(server3.getMetadata()).thenReturn(metadata3);

        List<ServiceInstance> servers = Arrays.asList(server1, server2, server3);
        RoutingContext ctx = new RoutingContext();

        List<ServiceInstance> result = router.doRoute(servers, ctx);
        assertEquals(2, result.size());
        assertTrue(result.contains(server1));
        assertTrue(result.contains(server2));
        assertFalse(result.contains(server3));
    }

    /**
     * Test null expression - should return original server list
     */
    @Test
    public void testDoRouteWithNullExpression() {
        MetadataRouter router = new MetadataRouter("metadata-router-1");
        router.setExpression(null);

        ServiceInstance server1 = mock(ServiceInstance.class);
        ServiceInstance server2 = mock(ServiceInstance.class);
        List<ServiceInstance> servers = Arrays.asList(server1, server2);
        RoutingContext ctx = new RoutingContext();

        List<ServiceInstance> result = router.doRoute(servers, ctx);
        assertEquals(servers, result);
    }

    /**
     * Test complex expression with multiple conditions
     */
    @Test
    public void testDoRouteWithComplexExpression() {
        MetadataRouter router = new MetadataRouter("metadata-router-1");
        router.setExpression("(version >= 2.0) || (region = cn-bj) || (zone = zone-a)");

        ServiceInstance server1 = mock(ServiceInstance.class);
        ServiceInstance server2 = mock(ServiceInstance.class);
        ServiceInstance server3 = mock(ServiceInstance.class);
        Map<String, Object> metadata1 = new HashMap<>();
        Map<String, Object> metadata2 = new HashMap<>();
        Map<String, Object> metadata3 = new HashMap<>();
        metadata1.put("version", "2.5");
        metadata2.put("region", "cn-bj");
        metadata3.put("zone", "zone-a");
        when(server1.getMetadata()).thenReturn(metadata1);
        when(server2.getMetadata()).thenReturn(metadata2);
        when(server3.getMetadata()).thenReturn(metadata3);

        List<ServiceInstance> servers = Arrays.asList(server1, server2, server3);
        RoutingContext ctx = new RoutingContext();

        List<ServiceInstance> result = router.doRoute(servers, ctx);
        assertEquals(3, result.size());
        assertTrue(result.contains(server1));
        assertTrue(result.contains(server2));
        assertTrue(result.contains(server3));
    }
}
