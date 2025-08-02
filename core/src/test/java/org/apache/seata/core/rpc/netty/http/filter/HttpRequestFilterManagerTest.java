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
package org.apache.seata.core.rpc.netty.http.filter;

import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class HttpRequestFilterManagerTest {

    interface MockFilter extends HttpRequestFilter {}

    @AfterEach
    void reset() throws Exception {
        Field filtersField = HttpRequestFilterManager.class.getDeclaredField("HTTP_REQUEST_FILTERS");
        filtersField.setAccessible(true);
        List<?> filters = (List<?>) filtersField.get(null);
        filters.clear();

        Field chainField = HttpRequestFilterManager.class.getDeclaredField("HTTP_REQUEST_FILTER_CHAIN");
        chainField.setAccessible(true);
        chainField.set(null, null);

        Field initializedField = HttpRequestFilterManager.class.getDeclaredField("initialized");
        initializedField.setAccessible(true);
        initializedField.setBoolean(null, false);
    }

    @Test
    void testInitializeFilters_andGetFilterChain() {
        MockFilter filter1 = mock(MockFilter.class);
        MockFilter filter2 = mock(MockFilter.class);

        when(filter1.shouldApply()).thenReturn(true);
        when(filter1.getOrder()).thenReturn(10);
        when(filter2.shouldApply()).thenReturn(true);
        when(filter2.getOrder()).thenReturn(5);

        try (MockedStatic<ConfigurationFactory> configMock = mockStatic(ConfigurationFactory.class);
                MockedStatic<EnhancedServiceLoader> mockedLoader = mockStatic(EnhancedServiceLoader.class)) {

            Configuration mockConfig = mock(Configuration.class);
            when(mockConfig.getBoolean(ConfigurationKeys.SERVER_HTTP_FILTER_ENABLE, true))
                    .thenReturn(true);
            configMock.when(ConfigurationFactory::getInstance).thenReturn(mockConfig);

            mockedLoader
                    .when(() -> EnhancedServiceLoader.loadAll(HttpRequestFilter.class))
                    .thenReturn(Arrays.asList(filter1, filter2));

            // init
            HttpRequestFilterManager.initializeFilters();

            // init again,expect no add
            HttpRequestFilterManager.initializeFilters();

            HttpRequestFilterChain chain = HttpRequestFilterManager.getFilterChain();
            assertNotNull(chain);

            List<HttpRequestFilter> filters = chain.getFilters();
            assertEquals(2, filters.size());
            assertSame(filter2, filters.get(0));
            assertSame(filter1, filters.get(1));
        }
    }

    @Test
    void testInitializeFilters_filterShouldApplyFalse() {
        MockFilter filter = mock(MockFilter.class);
        when(filter.shouldApply()).thenReturn(false);

        try (MockedStatic<ConfigurationFactory> configMock = mockStatic(ConfigurationFactory.class);
                MockedStatic<EnhancedServiceLoader> mockedLoader = mockStatic(EnhancedServiceLoader.class)) {
            Configuration mockConfig = mock(Configuration.class);
            when(mockConfig.getBoolean(ConfigurationKeys.SERVER_HTTP_FILTER_ENABLE, true))
                    .thenReturn(true);
            configMock.when(ConfigurationFactory::getInstance).thenReturn(mockConfig);
            mockedLoader
                    .when(() -> EnhancedServiceLoader.loadAll(HttpRequestFilter.class))
                    .thenReturn(Arrays.asList(filter));

            HttpRequestFilterManager.initializeFilters();

            HttpRequestFilterChain chain = HttpRequestFilterManager.getFilterChain();
            assertNotNull(chain);
            assertTrue(chain.getFilters().isEmpty(), "Filters list should be empty when shouldApply returns false");
        }
    }

    @Test
    void testInitializeFilters_filterEnabledFalse() {
        MockFilter filter = mock(MockFilter.class);
        when(filter.shouldApply()).thenReturn(true);

        try (MockedStatic<ConfigurationFactory> configMock = mockStatic(ConfigurationFactory.class)) {
            Configuration mockConfig = mock(Configuration.class);
            when(mockConfig.getBoolean(ConfigurationKeys.SERVER_HTTP_FILTER_ENABLE, true))
                    .thenReturn(false);
            configMock.when(ConfigurationFactory::getInstance).thenReturn(mockConfig);
            HttpRequestFilterManager.initializeFilters();

            HttpRequestFilterChain chain = HttpRequestFilterManager.getFilterChain();
            assertNotNull(chain);
            assertTrue(chain.getFilters().isEmpty(), "Filters list should be empty when filter config is false");
        }
    }

    @Test
    void testGetFilterChain_beforeInitialization_shouldThrow() {
        IllegalStateException exception =
                assertThrows(IllegalStateException.class, HttpRequestFilterManager::getFilterChain);
        assertEquals("HttpRequestFilterManager not initialized.", exception.getMessage());
    }
}
