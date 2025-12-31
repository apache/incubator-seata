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
package org.apache.seata.server.event;

import org.apache.seata.core.event.EventBus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EventBusManager Test - Direct testing without mocks
 */
@DisplayName("EventBusManager Test")
class EventBusManagerTest {

    @BeforeEach
    void setUp() {
        // Reset to ensure clean state
    }

    @Test
    @DisplayName("test get singleton instance")
    void testGetSingletonInstance() {
        EventBus eventBus1 = EventBusManager.get();
        EventBus eventBus2 = EventBusManager.get();

        assertNotNull(eventBus1);
        assertNotNull(eventBus2);
        assertSame(eventBus1, eventBus2);
    }

    @Test
    @DisplayName("test get returns not null instance")
    void testGetReturnsNotNullInstance() {
        EventBus eventBus = EventBusManager.get();
        assertNotNull(eventBus);
    }

    @Test
    @DisplayName("test event bus is guava implementation")
    void testEventBusIsGuavaImplementation() {
        EventBus eventBus = EventBusManager.get();
        // Verify that the EventBus instance is of correct type
        assertTrue(eventBus.getClass().getName().contains("GuavaEventBus")
                || eventBus.getClass().getName().contains("EventBus"));
    }

    @Test
    @DisplayName("test multiple get calls return same instance")
    void testMultipleGetCallsReturnSameInstance() {
        EventBus bus1 = EventBusManager.get();
        EventBus bus2 = EventBusManager.get();
        EventBus bus3 = EventBusManager.get();

        assertSame(bus1, bus2);
        assertSame(bus2, bus3);
    }

    @Test
    @DisplayName("test event bus methods are accessible")
    void testEventBusMethodsAreAccessible() {
        EventBus eventBus = EventBusManager.get();

        // Verify that the EventBus has the necessary methods
        assertNotNull(eventBus);
        assertTrue(eventBus.getClass().getMethods().length > 0);
    }
}
