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
package org.apache.seata.discovery.routing;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RouterSnapshotNodeTest {

    /**
     * Test constructor - create snapshot node
     */
    @Test
    public void testConstructor() {
        String routerName = "test-router";
        int inputSize = 5;
        int outputSize = 3;
        List<String> selectedServers = Arrays.asList("server1", "server2", "server3");
        long executionTime = 15L;

        RouterSnapshotNode<String> node =
                new RouterSnapshotNode<>(routerName, inputSize, outputSize, selectedServers, executionTime);

        assertNotNull(node);
    }

    /**
     * Test getRouterName
     */
    @Test
    public void testGetRouterName() {
        String routerName = "test-router";
        RouterSnapshotNode<String> node =
                new RouterSnapshotNode<>(routerName, 5, 3, Collections.singletonList("server1"), 10L);

        assertEquals(routerName, node.getRouterName());
    }

    /**
     * Test getInputSize
     */
    @Test
    public void testGetInputSize() {
        int inputSize = 10;
        RouterSnapshotNode<String> node =
                new RouterSnapshotNode<>("router", inputSize, 5, Collections.singletonList("server1"), 10L);

        assertEquals(inputSize, node.getInputSize());
    }

    /**
     * Test getOutputSize
     */
    @Test
    public void testGetOutputSize() {
        int outputSize = 7;
        RouterSnapshotNode<String> node =
                new RouterSnapshotNode<>("router", 10, outputSize, Collections.singletonList("server1"), 10L);

        assertEquals(outputSize, node.getOutputSize());
    }

    /**
     * Test getSelectedServers
     */
    @Test
    public void testGetSelectedServers() {
        List<String> selectedServers = Arrays.asList("server1", "server2", "server3");
        RouterSnapshotNode<String> node = new RouterSnapshotNode<>("router", 5, 3, selectedServers, 10L);

        assertEquals(selectedServers, node.getSelectedServers());
        assertEquals(3, node.getSelectedServers().size());
    }

    /**
     * Test getExecutionTimeMs
     */
    @Test
    public void testGetExecutionTimeMs() {
        long executionTime = 25L;
        RouterSnapshotNode<String> node =
                new RouterSnapshotNode<>("router", 5, 3, Collections.singletonList("server1"), executionTime);

        assertEquals(executionTime, node.getExecutionTimeMs());
    }

    /**
     * Test toString - verify formatted output
     */
    @Test
    public void testToString() {
        String routerName = "test-router";
        int inputSize = 5;
        int outputSize = 3;
        List<String> selectedServers = Arrays.asList("server1", "server2");
        long executionTime = 15L;

        RouterSnapshotNode<String> node =
                new RouterSnapshotNode<>(routerName, inputSize, outputSize, selectedServers, executionTime);

        String result = node.toString();
        assertTrue(result.contains(routerName));
        assertTrue(result.contains(String.valueOf(inputSize)));
        assertTrue(result.contains(String.valueOf(outputSize)));

        assertTrue(result.contains(String.valueOf(executionTime)));
        assertTrue(result.contains("ms")); // time unit

        assertTrue(result.contains("server1"));
        assertTrue(result.contains("server2"));
    }
}
