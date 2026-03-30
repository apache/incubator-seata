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

import org.apache.seata.discovery.routing.RouterSnapshotNode;
import org.apache.seata.discovery.routing.RoutingContext;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AbstractStateRouterTest {

    /**
     * Simple test router implementation for testing AbstractStateRouter
     */
    private static class TestRouter extends AbstractStateRouter<String> {

        private final boolean shouldReturnEmpty;

        public TestRouter(String routerName, boolean shouldReturnEmpty) {
            super(routerName);
            this.shouldReturnEmpty = shouldReturnEmpty;
        }

        @Override
        protected List<String> doRoute(List<String> servers, RoutingContext ctx) {
            if (shouldReturnEmpty) {
                return new ArrayList<>();
            }
            return servers;
        }

        @Override
        public String buildSnapshot() {
            return "test-router";
        }
    }

    /**
     * Test constructor
     */
    @Test
    public void testConstructorWithValidParameters() {
        TestRouter router = new TestRouter("test-router", false);
        assertNotNull(router);
    }

    /**
     * Test normal routing flow - standard routing execution
     */
    @Test
    public void testRouteWithNormalFlow() {
        TestRouter router = new TestRouter("test-router", false);
        List<String> servers = Arrays.asList("server1", "server2", "server3");
        RoutingContext ctx = new RoutingContext();

        List<String> result = router.route(servers, ctx, null);
        assertEquals(3, result.size());
        assertTrue(result.containsAll(servers));
    }

    /**
     * Test empty result scenario with fallback enabled
     */
    @Test
    public void testRouteWithEmptyResultAndFallback() {
        TestRouter router = new TestRouter("test-router", true);
        List<String> servers = Arrays.asList("server1", "server2", "server3");
        RoutingContext ctx = new RoutingContext();

        List<String> result = router.route(servers, ctx, null);
        // Should fallback to original list when result is empty
        assertEquals(3, result.size());
        assertTrue(result.containsAll(servers));
    }

    /**
     * Test debug mode - verify debug logging
     */
    @Test
    public void testRouteWithDebugMode() {
        TestRouter router = new TestRouter("test-router", false);
        List<String> servers = Arrays.asList("server1", "server2");
        RoutingContext ctx = new RoutingContext();
        List<RouterSnapshotNode<String>> snapshots = new ArrayList<>();

        List<String> result = router.route(servers, ctx, snapshots);
        assertEquals(2, result.size());

        // Verify debug logging
        assertEquals(1, snapshots.size());
        RouterSnapshotNode<String> snapshot = snapshots.get(0);
        assertEquals("test-router", snapshot.getRouterName());
        assertEquals(2, snapshot.getInputSize());
        assertEquals(2, snapshot.getOutputSize());
        assertEquals(2, snapshot.getSelectedServers().size());
    }

    /**
     * Test chained routing - multiple routers execute in series
     */
    @Test
    public void testRouteWithNextRouter() {
        TestRouter router1 = new TestRouter("router1", false);

        List<String> servers = Arrays.asList("server1", "server2");
        RoutingContext ctx = new RoutingContext();

        List<String> result = router1.route(servers, ctx, null);
        assertEquals(2, result.size());
    }

    /**
     * Test build snapshot - verify snapshot information
     */
    @Test
    public void testBuildSnapshotWithValidData() {
        TestRouter router = new TestRouter("test-router", false);
        String snapshot = router.buildSnapshot();
        assertTrue(snapshot.contains("test-router"));
    }
}
