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

import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import org.apache.seata.common.store.SessionMode;
import org.apache.seata.core.exception.HttpRequestFilterException;
import org.apache.seata.core.rpc.netty.http.SimpleHttp2Request;
import org.apache.seata.core.rpc.netty.http.filter.*;
import org.apache.seata.server.cluster.listener.ClusterChangeEvent;
import org.apache.seata.server.cluster.raft.context.SeataClusterContext;
import org.apache.seata.server.store.StoreConfig;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.apache.seata.common.Constants.RAFT_GROUP_HEADER;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RaftRequestFilterUnitTest {
    private final RaftRequestFilter filter = new RaftRequestFilter();
    private Map<String, Boolean> prevent;
    private Map<String, Boolean> previous;

    @BeforeEach
    void open() {
        prevent = (Map<String, Boolean>) ReflectionTestUtils.getField(RaftRequestFilter.class, "GROUP_PREVENT");
        previous = new HashMap<>(prevent);
        prevent.clear();
    }

    @AfterEach
    void close() {
        prevent.clear();
        prevent.putAll(previous);
        SeataClusterContext.unbindGroup();
    }

    private HttpFilterContext<?> context(Object request, Map<String, List<String>> params) {
        HttpRequestParamWrapper wrapper = mock(HttpRequestParamWrapper.class);
        when(wrapper.getAllParamsAsMultiMap()).thenReturn(params);
        return new HttpFilterContext<>(request, null, true, "HTTP/1.1", () -> wrapper);
    }

    @Test
    void nonTargetRequestsPassThroughWithoutGroupBinding() throws Exception {
        HttpRequestFilterChain chain = mock(HttpRequestFilterChain.class);
        for (Object request :
                Arrays.asList(new Object(), new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/health"))) {
            HttpFilterContext<?> context = context(request, Collections.emptyMap());
            filter.doFilter(context, chain);
            verify(chain).doFilter(context);
        }
    }

    @Test
    void queryGroupTakesPrecedenceAndBindingIsAlwaysCleared() throws Exception {
        try (MockedStatic<StoreConfig> config = mockStatic(StoreConfig.class)) {
            config.when(StoreConfig::getSessionMode).thenReturn(SessionMode.RAFT);
            assertTrue(filter.shouldApply());
            filter.onApplicationEvent(new ClusterChangeEvent(this, "query", 1, true));
            DefaultHttpRequest request =
                    new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/vgroup/v1/add");
            request.headers().set(RAFT_GROUP_HEADER, "header");
            HttpFilterContext<?> context =
                    context(request, Collections.singletonMap("unit", Collections.singletonList("query")));
            HttpRequestFilterChain chain = mock(HttpRequestFilterChain.class);
            doAnswer(i -> {
                        assertEquals("query", SeataClusterContext.getGroup());
                        throw new HttpRequestFilterException("downstream");
                    })
                    .when(chain)
                    .doFilter(context);
            assertThrows(HttpRequestFilterException.class, () -> filter.doFilter(context, chain));
            assertNull(SeataClusterContext.getGroup());
            filter.onApplicationEvent(new ClusterChangeEvent(this, "query", 2, false));
            reset(chain);
            assertThrows(HttpRequestFilterException.class, () -> filter.doFilter(context, chain));
            verifyNoInteractions(chain);
            assertNull(SeataClusterContext.getGroup());
            config.when(StoreConfig::getSessionMode).thenReturn(SessionMode.FILE);
            assertFalse(filter.shouldApply());
            RaftRequestFilter.setPrevent("ignored", true);
            assertFalse(prevent.containsKey("ignored"));
        }
    }

    @Test
    void http1AndHttp2HeadersPermitLeadersAndReads() throws Exception {
        prevent.put("leader", true);
        HttpRequestFilterChain chain = mock(HttpRequestFilterChain.class);
        DefaultHttpRequest http1 =
                new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/api/v1/console/commit");
        http1.headers().set(RAFT_GROUP_HEADER, "leader");
        HttpFilterContext<?> c1 = context(http1, Collections.emptyMap());
        filter.doFilter(c1, chain);
        verify(chain).doFilter(c1);
        SimpleHttp2Request http2 = new SimpleHttp2Request(
                HttpMethod.POST, "/vgroup/v1/add", new DefaultHttp2Headers(false).set(RAFT_GROUP_HEADER, "leader"), "");
        HttpFilterContext<?> c2 = context(http2, Collections.emptyMap());
        filter.doFilter(c2, chain);
        verify(chain).doFilter(c2);
        HttpFilterContext<?> read = context(
                new SimpleHttp2Request(HttpMethod.GET, "/vgroup/v1/query", new DefaultHttp2Headers(false), ""),
                Collections.emptyMap());
        filter.doFilter(read, chain);
        verify(chain).doFilter(read);
        assertNull(SeataClusterContext.getGroup());
    }
}
