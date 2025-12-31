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
package org.apache.seata.server.filter;

import org.apache.seata.core.exception.HttpRequestFilterException;
import org.apache.seata.core.rpc.netty.http.filter.HttpFilterContext;
import org.apache.seata.core.rpc.netty.http.filter.HttpRequestFilterChain;
import org.apache.seata.core.rpc.netty.http.filter.HttpRequestParamWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * XSSHttpRequestFilter Test
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("XSSHttpRequestFilter Test")
class XSSHttpRequestFilterTest {

    private XSSHttpRequestFilter xssFilter;

    @Mock
    private HttpFilterContext<?> mockContext;

    @Mock
    private HttpRequestFilterChain mockChain;

    @Mock
    private HttpRequestParamWrapper mockParamWrapper;

    @BeforeEach
    void setUp() {
        xssFilter = new XSSHttpRequestFilter();
        when(mockContext.getParamWrapper()).thenReturn(mockParamWrapper);
    }

    @Test
    @DisplayName("test shouldApply returns true")
    void testShouldApplyReturnsTrue() {
        assertTrue(xssFilter.shouldApply());
    }

    @Test
    @DisplayName("test getOrder returns MIN_VALUE")
    void testGetOrderReturnsMinValue() {
        assertEquals(Integer.MIN_VALUE, xssFilter.getOrder());
    }

    @Test
    @DisplayName("test doFilter with clean parameters")
    void testDoFilterWithCleanParameters() throws HttpRequestFilterException {
        Map<String, List<String>> params = new HashMap<>();
        List<String> values = new ArrayList<>();
        values.add("normalValue");
        params.put("param1", values);

        when(mockParamWrapper.getAllParamsAsMultiMap()).thenReturn(params);

        assertDoesNotThrow(() -> xssFilter.doFilter(mockContext, mockChain));
        verify(mockChain).doFilter(mockContext);
    }

    @Test
    @DisplayName("test doFilter with XSS script tag")
    void testDoFilterWithXSSScriptTag() throws HttpRequestFilterException {
        Map<String, List<String>> params = new HashMap<>();
        List<String> values = new ArrayList<>();
        values.add("<script>alert('xss')</script>");
        params.put("param1", values);

        when(mockParamWrapper.getAllParamsAsMultiMap()).thenReturn(params);

        assertThrows(HttpRequestFilterException.class, () -> xssFilter.doFilter(mockContext, mockChain));
    }

    @Test
    @DisplayName("test doFilter with XSS javascript tag")
    void testDoFilterWithXSSJavascriptTag() throws HttpRequestFilterException {
        Map<String, List<String>> params = new HashMap<>();
        List<String> values = new ArrayList<>();
        values.add("javascript:alert('xss')");
        params.put("param1", values);

        when(mockParamWrapper.getAllParamsAsMultiMap()).thenReturn(params);

        assertThrows(HttpRequestFilterException.class, () -> xssFilter.doFilter(mockContext, mockChain));
    }

    @Test
    @DisplayName("test doFilter with empty parameters")
    void testDoFilterWithEmptyParameters() throws HttpRequestFilterException {
        Map<String, List<String>> params = new HashMap<>();
        when(mockParamWrapper.getAllParamsAsMultiMap()).thenReturn(params);

        assertDoesNotThrow(() -> xssFilter.doFilter(mockContext, mockChain));
        verify(mockChain).doFilter(mockContext);
    }

    @Test
    @DisplayName("test doFilter with null value")
    void testDoFilterWithNullValue() throws HttpRequestFilterException {
        Map<String, List<String>> params = new HashMap<>();
        List<String> values = new ArrayList<>();
        values.add(null);
        params.put("param1", values);

        when(mockParamWrapper.getAllParamsAsMultiMap()).thenReturn(params);

        assertDoesNotThrow(() -> xssFilter.doFilter(mockContext, mockChain));
        verify(mockChain).doFilter(mockContext);
    }

    @Test
    @DisplayName("test doFilter with XSS event handler")
    void testDoFilterWithXSSEventHandler() throws HttpRequestFilterException {
        Map<String, List<String>> params = new HashMap<>();
        List<String> values = new ArrayList<>();
        values.add("onclick=\"alert('xss')\"");
        params.put("param1", values);

        when(mockParamWrapper.getAllParamsAsMultiMap()).thenReturn(params);

        assertThrows(HttpRequestFilterException.class, () -> xssFilter.doFilter(mockContext, mockChain));
    }

    @Test
    @DisplayName("test doFilter with repeated 'on' keyword")
    void testDoFilterWithRepeatedOnKeyword() throws HttpRequestFilterException {
        Map<String, List<String>> params = new HashMap<>();
        List<String> values = new ArrayList<>();
        values.add("onononononon");
        params.put("param1", values);

        when(mockParamWrapper.getAllParamsAsMultiMap()).thenReturn(params);

        assertThrows(HttpRequestFilterException.class, () -> xssFilter.doFilter(mockContext, mockChain));
    }

    @Test
    @DisplayName("test doFilter with multiple parameters")
    void testDoFilterWithMultipleParameters() throws HttpRequestFilterException {
        Map<String, List<String>> params = new HashMap<>();

        List<String> values1 = new ArrayList<>();
        values1.add("normalValue");
        params.put("param1", values1);

        List<String> values2 = new ArrayList<>();
        values2.add("anotherNormalValue");
        params.put("param2", values2);

        when(mockParamWrapper.getAllParamsAsMultiMap()).thenReturn(params);

        assertDoesNotThrow(() -> xssFilter.doFilter(mockContext, mockChain));
        verify(mockChain).doFilter(mockContext);
    }

    @Test
    @DisplayName("test doFilter with multiple values in same parameter")
    void testDoFilterWithMultipleValuesInSameParameter() throws HttpRequestFilterException {
        Map<String, List<String>> params = new HashMap<>();
        List<String> values = new ArrayList<>();
        values.add("normalValue1");
        values.add("normalValue2");
        params.put("param1", values);

        when(mockParamWrapper.getAllParamsAsMultiMap()).thenReturn(params);

        assertDoesNotThrow(() -> xssFilter.doFilter(mockContext, mockChain));
        verify(mockChain).doFilter(mockContext);
    }

    @Test
    @DisplayName("test doFilter with case insensitive XSS script tag")
    void testDoFilterWithCaseInsensitiveXSSScriptTag() throws HttpRequestFilterException {
        Map<String, List<String>> params = new HashMap<>();
        List<String> values = new ArrayList<>();
        values.add("<SCRIPT>alert('xss')</SCRIPT>");
        params.put("param1", values);

        when(mockParamWrapper.getAllParamsAsMultiMap()).thenReturn(params);

        assertThrows(HttpRequestFilterException.class, () -> xssFilter.doFilter(mockContext, mockChain));
    }
}
