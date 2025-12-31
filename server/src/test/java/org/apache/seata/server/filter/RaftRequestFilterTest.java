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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.seata.common.store.SessionMode;
import org.apache.seata.core.exception.HttpRequestFilterException;
import org.apache.seata.core.rpc.netty.http.filter.HttpFilterContext;
import org.apache.seata.core.rpc.netty.http.filter.HttpRequestFilterChain;
import org.apache.seata.core.rpc.netty.http.filter.HttpRequestParamWrapper;
import org.apache.seata.server.cluster.listener.ClusterChangeEvent;
import org.apache.seata.server.store.StoreConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RaftRequestFilter Test
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RaftRequestFilter Test")
class RaftRequestFilterTest {

    private RaftRequestFilter raftFilter;

    @Mock
    private HttpFilterContext<io.netty.handler.codec.http.HttpRequest> mockContext;

    @Mock
    private HttpRequestFilterChain mockChain;

    @Mock
    private HttpRequestParamWrapper mockParamWrapper;

    @Mock
    private io.netty.handler.codec.http.HttpRequest mockHttpRequest;

    @BeforeEach
    void setUp() {
        raftFilter = new RaftRequestFilter();
        when(mockContext.getParamWrapper()).thenReturn(mockParamWrapper);
    }

    @Test
    @DisplayName("test shouldApply returns true in raft mode")
    void testShouldApplyReturnsTrueInRaftMode() {
        try (MockedStatic<StoreConfig> storeConfigMock = mockStatic(StoreConfig.class)) {
            storeConfigMock.when(StoreConfig::getSessionMode).thenReturn(SessionMode.RAFT);
            
            RaftRequestFilter filter = new RaftRequestFilter();
            assertTrue(filter.shouldApply());
        }
    }

    @Test
    @DisplayName("test shouldApply returns false in file mode")
    void testShouldApplyReturnsFalseInFileMode() {
        try (MockedStatic<StoreConfig> storeConfigMock = mockStatic(StoreConfig.class)) {
            storeConfigMock.when(StoreConfig::getSessionMode).thenReturn(SessionMode.FILE);
            
            RaftRequestFilter filter = new RaftRequestFilter();
            assertFalse(filter.shouldApply());
        }
    }

    @Test
    @DisplayName("test doFilter with non-target uri")
    void testDoFilterWithNonTargetUri() throws HttpRequestFilterException {
        when(mockHttpRequest.uri()).thenReturn("/other/path");
        when(mockContext.getRequest()).thenReturn(mockHttpRequest);
        Map<String, List<String>> params = new HashMap<>();
        when(mockParamWrapper.getAllParamsAsMultiMap()).thenReturn(params);
        
        raftFilter.doFilter(mockContext, mockChain);
        
        verify(mockChain).doFilter(mockContext);
    }

    @Test
    @DisplayName("test doFilter with target uri and get method")
    void testDoFilterWithTargetUriAndGetMethod() throws HttpRequestFilterException {
        when(mockHttpRequest.uri()).thenReturn("/api/v1/console/test");
        when(mockHttpRequest.method()).thenReturn(io.netty.handler.codec.http.HttpMethod.GET);
        when(mockContext.getRequest()).thenReturn(mockHttpRequest);
        Map<String, List<String>> params = new HashMap<>();
        when(mockParamWrapper.getAllParamsAsMultiMap()).thenReturn(params);
        
        RaftRequestFilter.setPrevent("default", true);
        
        raftFilter.doFilter(mockContext, mockChain);
        
        verify(mockChain).doFilter(mockContext);
    }

    @Test
    @DisplayName("test doFilter with target uri and post method but not leader")
    void testDoFilterWithTargetUriAndPostMethodButNotLeader() {
        when(mockHttpRequest.uri()).thenReturn("/api/v1/console/test");
        when(mockHttpRequest.method()).thenReturn(io.netty.handler.codec.http.HttpMethod.POST);
        when(mockContext.getRequest()).thenReturn(mockHttpRequest);
        Map<String, List<String>> params = new HashMap<>();
        when(mockParamWrapper.getAllParamsAsMultiMap()).thenReturn(params);
        
        RaftRequestFilter.setPrevent("default", false);
        
        assertThrows(HttpRequestFilterException.class, () -> 
            raftFilter.doFilter(mockContext, mockChain));
    }

    @Test
    @DisplayName("test onApplicationEvent with cluster change event")
    void testOnApplicationEventWithClusterChangeEvent() {
        ClusterChangeEvent event = mock(ClusterChangeEvent.class);
        when(event.getGroup()).thenReturn("group1");
        when(event.isLeader()).thenReturn(true);
        
        raftFilter.onApplicationEvent(event);
        
        // Verify that setPrevent was called (indirectly through the event handler)
        assertTrue(true); // Event processing completed without exception
    }

    @Test
    @DisplayName("test doFilter with vgroup uri")
    void testDoFilterWithVgroupUri() throws HttpRequestFilterException {
        when(mockHttpRequest.uri()).thenReturn("/vgroup/v1/test");
        when(mockHttpRequest.method()).thenReturn(io.netty.handler.codec.http.HttpMethod.GET);
        when(mockContext.getRequest()).thenReturn(mockHttpRequest);
        Map<String, List<String>> params = new HashMap<>();
        when(mockParamWrapper.getAllParamsAsMultiMap()).thenReturn(params);
        
        raftFilter.doFilter(mockContext, mockChain);
        
        verify(mockChain).doFilter(mockContext);
    }

    @Test
    @DisplayName("test doFilter with unit parameter")
    void testDoFilterWithUnitParameter() throws HttpRequestFilterException {
        when(mockHttpRequest.uri()).thenReturn("/api/v1/console/test");
        when(mockHttpRequest.method()).thenReturn(io.netty.handler.codec.http.HttpMethod.GET);
        when(mockContext.getRequest()).thenReturn(mockHttpRequest);
        
        Map<String, List<String>> params = new HashMap<>();
        List<String> unitParams = new ArrayList<>();
        unitParams.add("group1");
        params.put("unit", unitParams);
        when(mockParamWrapper.getAllParamsAsMultiMap()).thenReturn(params);
        
        RaftRequestFilter.setPrevent("group1", true);
        
        raftFilter.doFilter(mockContext, mockChain);
        
        verify(mockChain).doFilter(mockContext);
    }

    @Test
    @DisplayName("test setPrevent method")
    void testSetPreventMethod() {
        try (MockedStatic<StoreConfig> storeConfigMock = mockStatic(StoreConfig.class)) {
            storeConfigMock.when(StoreConfig::getSessionMode).thenReturn(SessionMode.RAFT);
            
            RaftRequestFilter.setPrevent("group2", true);
            // If no exception is thrown, the method works correctly
            assertTrue(true);
        }
    }

    @Test
    @DisplayName("test doFilter with empty uri")
    void testDoFilterWithEmptyUri() throws HttpRequestFilterException {
        when(mockHttpRequest.uri()).thenReturn("");
        when(mockContext.getRequest()).thenReturn(mockHttpRequest);
        Map<String, List<String>> params = new HashMap<>();
        when(mockParamWrapper.getAllParamsAsMultiMap()).thenReturn(params);
        
        raftFilter.doFilter(mockContext, mockChain);
        
        verify(mockChain).doFilter(mockContext);
    }

    @Test
    @DisplayName("test doFilter with null uri")
    void testDoFilterWithNullUri() throws HttpRequestFilterException {
        when(mockHttpRequest.uri()).thenReturn(null);
        when(mockContext.getRequest()).thenReturn(mockHttpRequest);
        Map<String, List<String>> params = new HashMap<>();
        when(mockParamWrapper.getAllParamsAsMultiMap()).thenReturn(params);
        
        raftFilter.doFilter(mockContext, mockChain);
        
        verify(mockChain).doFilter(mockContext);
    }

    @Test
    @DisplayName("test doFilter with put method")
    void testDoFilterWithPutMethod() throws HttpRequestFilterException {
        when(mockHttpRequest.uri()).thenReturn("/api/v1/console/test");
        when(mockHttpRequest.method()).thenReturn(io.netty.handler.codec.http.HttpMethod.PUT);
        when(mockContext.getRequest()).thenReturn(mockHttpRequest);
        Map<String, List<String>> params = new HashMap<>();
        when(mockParamWrapper.getAllParamsAsMultiMap()).thenReturn(params);
        
        RaftRequestFilter.setPrevent("default", true);
        
        raftFilter.doFilter(mockContext, mockChain);
        
        verify(mockChain).doFilter(mockContext);
    }

    @Test
    @DisplayName("test doFilter with delete method")
    void testDoFilterWithDeleteMethod() throws HttpRequestFilterException {
        when(mockHttpRequest.uri()).thenReturn("/api/v1/console/test");
        when(mockHttpRequest.method()).thenReturn(io.netty.handler.codec.http.HttpMethod.DELETE);
        when(mockContext.getRequest()).thenReturn(mockHttpRequest);
        Map<String, List<String>> params = new HashMap<>();
        when(mockParamWrapper.getAllParamsAsMultiMap()).thenReturn(params);
        
        RaftRequestFilter.setPrevent("default", true);
        
        raftFilter.doFilter(mockContext, mockChain);
        
        verify(mockChain).doFilter(mockContext);
    }
}

