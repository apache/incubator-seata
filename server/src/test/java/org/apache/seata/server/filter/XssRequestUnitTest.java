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

import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.config.ConfigurationCache;
import org.apache.seata.core.exception.HttpRequestFilterException;
import org.apache.seata.core.rpc.netty.http.filter.*;
import org.apache.seata.server.BaseSpringBootTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class XssRequestUnitTest extends BaseSpringBootTest {
    @Test
    void configuredKeywordsAndEventHandlersAreRejectedBeforeDispatch() throws Exception {
        String key = ConfigurationKeys.SERVER_HTTP_FILTER_XSS_FILTER_KEYWORDS, previous = System.getProperty(key);
        Object keywords = ReflectionTestUtils.getField(XSSHttpRequestFilter.class, "xssKeywords");
        try {
            System.setProperty(key, "[\"dangerous\",\"dangerous\"]");
            ConfigurationCache.clear();
            XSSHttpRequestFilter filter = new XSSHttpRequestFilter();
            assertTrue(filter.shouldApply());
            assertEquals(Integer.MIN_VALUE, filter.getOrder());
            HttpRequestParamWrapper params = mock(HttpRequestParamWrapper.class);
            HttpFilterContext<?> context = new HttpFilterContext<>(new Object(), null, true, "HTTP/1.1", () -> params);
            HttpRequestFilterChain chain = mock(HttpRequestFilterChain.class);
            for (String value : Arrays.asList("D A N G E R O U S", "ononononon", "oncustom='payload'")) {
                when(params.getAllParamsAsMultiMap())
                        .thenReturn(Collections.singletonMap("input", Collections.singletonList(value)));
                assertThrows(HttpRequestFilterException.class, () -> filter.doFilter(context, chain));
            }
            verifyNoInteractions(chain);
            when(params.getAllParamsAsMultiMap())
                    .thenReturn(Collections.singletonMap("input", Arrays.asList(null, "normal text")));
            filter.doFilter(context, chain);
            verify(chain).doFilter(context);
            System.setProperty(key, "not-json");
            ConfigurationCache.clear();
            assertThrows(IllegalArgumentException.class, XSSHttpRequestFilter::new);
        } finally {
            ReflectionTestUtils.setField(XSSHttpRequestFilter.class, "xssKeywords", keywords);
            if (previous == null) {
                System.clearProperty(key);
            } else {
                System.setProperty(key, previous);
            }
            ConfigurationCache.clear();
        }
    }
}
