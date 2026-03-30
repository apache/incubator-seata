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
package org.apache.seata.discovery.routing.expression;

import org.apache.seata.common.metadata.ServiceInstance;
import org.apache.seata.discovery.routing.RoutingContext;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ConfigurableConditionMatcherTest {

    /**
     * Test constructor - verify condition parsing and exception handling
     */
    @Test
    public void testConstructorWithValidAndInvalidConditions() {
        ConfigurableConditionMatcher matcher = new ConfigurableConditionMatcher("version >= 2.3");
        assertTrue(matcher.toString().contains("key='version'"));
        assertTrue(matcher.toString().contains("operator='>='"));
        assertTrue(matcher.toString().contains("value='2.3'"));

        // Test invalid condition
        assertThrows(IllegalArgumentException.class, () -> {
            new ConfigurableConditionMatcher("invalid");
        });
    }

    /**
     * Test string comparison - equals and not equals operations
     */
    @Test
    public void testStringComparisonWithEqualsAndNotEquals() {
        ServiceInstance server = mock(ServiceInstance.class);
        RoutingContext ctx = mock(RoutingContext.class);
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("env", "prod");
        when(server.getMetadata()).thenReturn(metadata);

        // Test equals with =
        ConfigurableConditionMatcher matcher = new ConfigurableConditionMatcher("env = prod");
        assertTrue(matcher.match(server, ctx));

        // Test equals with ==
        matcher = new ConfigurableConditionMatcher("env == prod");
        assertTrue(matcher.match(server, ctx));

        // Test not equals
        matcher = new ConfigurableConditionMatcher("env != dev");
        assertTrue(matcher.match(server, ctx));

        // Test no match with =
        matcher = new ConfigurableConditionMatcher("env = dev");
        assertFalse(matcher.match(server, ctx));

        // Test no match with ==
        matcher = new ConfigurableConditionMatcher("env == dev");
        assertFalse(matcher.match(server, ctx));
    }

    /**
     * Test numeric comparison - various comparison operators
     */
    @Test
    public void testNumericComparisonWithVariousOperators() {
        ServiceInstance server = mock(ServiceInstance.class);
        RoutingContext ctx = mock(RoutingContext.class);
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("version", "2.3");
        when(server.getMetadata()).thenReturn(metadata);

        // Test greater than or equal
        ConfigurableConditionMatcher matcher = new ConfigurableConditionMatcher("version >= 2.0");
        assertTrue(matcher.match(server, ctx));

        // Test >
        matcher = new ConfigurableConditionMatcher("version > 2.0");
        assertTrue(matcher.match(server, ctx));

        // Test <
        matcher = new ConfigurableConditionMatcher("version < 3.0");
        assertTrue(matcher.match(server, ctx));

        // Test <=
        matcher = new ConfigurableConditionMatcher("version <= 2.3");
        assertTrue(matcher.match(server, ctx));

        // Test no match
        matcher = new ConfigurableConditionMatcher("version > 3.0");
        assertFalse(matcher.match(server, ctx));
    }

    /**
     * Test numeric precision - handle different numeric formats
     */
    @Test
    public void testNumericPrecisionWithDifferentFormats() {
        ServiceInstance server = mock(ServiceInstance.class);
        RoutingContext ctx = mock(RoutingContext.class);
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("version", "1.0");
        when(server.getMetadata()).thenReturn(metadata);

        // Test precision issue: 1 = 1.0 with =
        ConfigurableConditionMatcher matcher = new ConfigurableConditionMatcher("version = 1.0");
        assertTrue(matcher.match(server, ctx));

        matcher = new ConfigurableConditionMatcher("version = 1");
        assertTrue(matcher.match(server, ctx));

        // Test precision issue: 1 == 1.0 with ==
        matcher = new ConfigurableConditionMatcher("version == 1.0");
        assertTrue(matcher.match(server, ctx));

        matcher = new ConfigurableConditionMatcher("version == 1");
        assertTrue(matcher.match(server, ctx));
    }

    /**
     * Test mixed comparison - numeric and string comparison
     */
    @Test
    public void testMixedComparisonWithNumericAndString() {
        ServiceInstance server = mock(ServiceInstance.class);
        RoutingContext ctx = mock(RoutingContext.class);
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("version", "2.3");
        metadata.put("env", "prod");
        when(server.getMetadata()).thenReturn(metadata);

        // Numeric comparison
        ConfigurableConditionMatcher matcher = new ConfigurableConditionMatcher("version >= 2.0");
        assertTrue(matcher.match(server, ctx));

        // String comparison
        matcher = new ConfigurableConditionMatcher("env = prod");
        assertTrue(matcher.match(server, ctx));
    }

    /**
     * Test missing metadata scenario - servers without corresponding metadata should not pass through
     */
    @Test
    public void testMissingMetadataScenario() {
        ServiceInstance server = mock(ServiceInstance.class);
        RoutingContext ctx = mock(RoutingContext.class);
        Map<String, Object> metadata = new HashMap<>();
        when(server.getMetadata()).thenReturn(metadata);

        // Should not pass through when metadata is missing
        ConfigurableConditionMatcher matcher = new ConfigurableConditionMatcher("version >= 2.0");
        assertFalse(matcher.match(server, ctx));
    }

    /**
     * Test invalid numeric comparison - perform numeric comparison on non-numeric values
     */
    @Test
    public void testInvalidNumericComparisonOnNonNumericValues() {
        ServiceInstance server = mock(ServiceInstance.class);
        RoutingContext ctx = mock(RoutingContext.class);
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("env", "prod");
        when(server.getMetadata()).thenReturn(metadata);

        // Should return false when performing numeric comparison on non-numeric values
        ConfigurableConditionMatcher matcher = new ConfigurableConditionMatcher("env >= 2.0");
        assertFalse(matcher.match(server, ctx));
    }
}
