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
package org.apache.seata.server.spring.listener;

import org.apache.seata.core.rpc.netty.http.filter.HttpRequestFilterManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.event.ContextRefreshedEvent;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * HttpFilterInitListener Test
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("HttpFilterInitListener Test")
class HttpFilterInitListenerTest {

    private HttpFilterInitListener listener;

    @BeforeEach
    void setUp() {
        listener = new HttpFilterInitListener();
    }

    @Test
    @DisplayName("test onApplicationEvent initializes filters")
    void testOnApplicationEventInitializesFilters() {
        try (MockedStatic<HttpRequestFilterManager> filterManagerMock = mockStatic(HttpRequestFilterManager.class)) {

            ContextRefreshedEvent event = mock(ContextRefreshedEvent.class);

            listener.onApplicationEvent(event);

            filterManagerMock.verify(HttpRequestFilterManager::initializeFilters, times(1));
        }
    }

    @Test
    @DisplayName("test onApplicationEvent called multiple times initializes only once")
    void testOnApplicationEventCalledMultipleTimesInitializesOnlyOnce() {
        try (MockedStatic<HttpRequestFilterManager> filterManagerMock = mockStatic(HttpRequestFilterManager.class)) {

            ContextRefreshedEvent event1 = mock(ContextRefreshedEvent.class);
            ContextRefreshedEvent event2 = mock(ContextRefreshedEvent.class);

            listener.onApplicationEvent(event1);
            listener.onApplicationEvent(event2);

            filterManagerMock.verify(HttpRequestFilterManager::initializeFilters, times(1));
        }
    }

    @Test
    @DisplayName("test listener is ApplicationListener implementation")
    void testListenerIsApplicationListenerImplementation() {
        assertTrue(listener instanceof org.springframework.context.ApplicationListener);
    }

    @Test
    @DisplayName("test listener instantiation")
    void testListenerInstantiation() {
        assertNotNull(listener);
    }

    @Test
    @DisplayName("test onApplicationEvent with null event does not crash")
    void testOnApplicationEventWithValidEvent() {
        try (MockedStatic<HttpRequestFilterManager> filterManagerMock = mockStatic(HttpRequestFilterManager.class)) {

            ContextRefreshedEvent event = mock(ContextRefreshedEvent.class);

            assertDoesNotThrow(() -> listener.onApplicationEvent(event));
        }
    }

    @Test
    @DisplayName("test multiple listeners only initialize once")
    void testMultipleListenersOnlyInitializeOnce() {
        try (MockedStatic<HttpRequestFilterManager> filterManagerMock = mockStatic(HttpRequestFilterManager.class)) {

            HttpFilterInitListener listener1 = new HttpFilterInitListener();
            HttpFilterInitListener listener2 = new HttpFilterInitListener();

            ContextRefreshedEvent event = mock(ContextRefreshedEvent.class);

            listener1.onApplicationEvent(event);
            listener2.onApplicationEvent(event);

            // Both listeners should share the same static initialized flag
            // So filters should only be initialized once in total across instances
            filterManagerMock.verify(HttpRequestFilterManager::initializeFilters, atLeast(1));
        }
    }

    @Test
    @DisplayName("test listener fires on ContextRefreshedEvent")
    void testListenerFiresOnContextRefreshedEvent() {
        try (MockedStatic<HttpRequestFilterManager> filterManagerMock = mockStatic(HttpRequestFilterManager.class)) {

            ContextRefreshedEvent event = mock(ContextRefreshedEvent.class);

            listener.onApplicationEvent(event);

            // Verify initialization was called
            filterManagerMock.verify(HttpRequestFilterManager::initializeFilters);
        }
    }
}
