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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class RoutingContextTest {

    /**
     * Test set and get attributes - verify basic attribute operations
     */
    @Test
    public void testSetAndGetAttribute() {
        RoutingContext ctx = new RoutingContext();
        ctx.setAttribute("key1", "value1");
        ctx.setAttribute("key2", 123);

        assertEquals("value1", ctx.getAttribute("key1"));
        assertEquals(123, ctx.getAttribute("key2"));
    }

    /**
     * Test get non-existent attribute - verify null return value
     */
    @Test
    public void testGetNonExistentAttribute() {
        RoutingContext ctx = new RoutingContext();
        assertNull(ctx.getAttribute("non-existent"));
    }

    /**
     * Test overwrite attribute - verify attribute update mechanism
     */
    @Test
    public void testSetAttributeOverwrite() {
        RoutingContext ctx = new RoutingContext();
        ctx.setAttribute("key", "value1");
        ctx.setAttribute("key", "value2");

        assertEquals("value2", ctx.getAttribute("key"));
    }
}
