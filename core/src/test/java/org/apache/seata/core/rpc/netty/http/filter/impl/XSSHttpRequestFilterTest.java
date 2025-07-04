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
package org.apache.seata.core.rpc.netty.http.filter.impl;

import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import org.apache.seata.core.exception.HttpRequestFilterException;
import org.apache.seata.core.rpc.netty.http.filter.HttpFilterContext;
import org.apache.seata.core.rpc.netty.http.filter.HttpRequestParamWrapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertThrows;

class XSSHttpRequestFilterTest {

    private final XSSHttpRequestFilter filter = new XSSHttpRequestFilter();

    private FullHttpRequest buildRequestWithQuery(String uri) {
        return new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uri);
    }

    @Test
    public void testFilter_withXssKeyword_shouldThrow() {
        FullHttpRequest request = buildRequestWithQuery("/path?param=<script>alert(1)</script>");
        HttpFilterContext context = new HttpFilterContext(request, () -> new HttpRequestParamWrapper(request));

        assertThrows(HttpRequestFilterException.class, () -> filter.doFilter(context));
    }

    @Test
    public void testFilter_withSafeQueryParam_shouldPass() {
        FullHttpRequest request = buildRequestWithQuery("/path?param=normalValue");
        HttpFilterContext context = new HttpFilterContext(request, () -> new HttpRequestParamWrapper(request));

        try {
            filter.doFilter(context);
        } catch (Exception e) {
            fail("Should not throw exception, but got: " + e.getMessage());
        }
    }

    @Test
    public void testFilter_withOnloadEvent_shouldThrow() {
        FullHttpRequest request = buildRequestWithQuery("/path?param=onload=alert(1)");
        HttpFilterContext context = new HttpFilterContext(request, () -> new HttpRequestParamWrapper(request));

        assertThrows(HttpRequestFilterException.class, () -> filter.doFilter(context));
    }

    @Test
    public void testFilter_withJavascriptUrl_shouldThrow() {
        FullHttpRequest request = buildRequestWithQuery("/path?url=javascript:alert(1)");
        HttpFilterContext context = new HttpFilterContext(request, () -> new HttpRequestParamWrapper(request));

        assertThrows(HttpRequestFilterException.class, () -> filter.doFilter(context));
    }

    @Test
    void testFilter_withRepeatedOn_shouldThrow() {
        FullHttpRequest request = buildRequestWithQuery("/test?param=ononononon123='xxx'");
        HttpFilterContext context = new HttpFilterContext(request, () -> new HttpRequestParamWrapper(request));

        assertThrows(HttpRequestFilterException.class, () -> filter.doFilter(context));
    }

    @Test
    void testFilter_withShortEventName_shouldThrow() {
        FullHttpRequest request = buildRequestWithQuery("/test?param=onclick=\"doSomething()\"");
        HttpFilterContext context = new HttpFilterContext(request, () -> new HttpRequestParamWrapper(request));

        assertThrows(HttpRequestFilterException.class, () -> filter.doFilter(context));
    }

    @Test
    void testFilter_withLongEventName_shouldThrow() {
        String longEventName = "on" + new String(new char[50]).replace('\0', 'a');
        FullHttpRequest request = buildRequestWithQuery("/test?param=" + longEventName + "=\"xss\"");
        HttpFilterContext context = new HttpFilterContext(request, () -> new HttpRequestParamWrapper(request));

        assertThrows(HttpRequestFilterException.class, () -> filter.doFilter(context));
    }

    /**
     * The simulated attacker constructs long onononononon... Characters,
     * attempting to freeze the regular expression backtracking
     */
    @Test
    public void testFilter_withLongOnString_shouldThrowQuickly() {
        StringBuilder attackBuilder = new StringBuilder("on");
        for (int i = 0; i < 10000; i++) {
            attackBuilder.append("on");
        }
        attackBuilder.append("=alert(1)");

        String attackString = attackBuilder.toString();

        FullHttpRequest request = buildRequestWithQuery("/path?param=" + attackString);
        HttpFilterContext context = new HttpFilterContext(request, () -> new HttpRequestParamWrapper(request));

        assertThrows(HttpRequestFilterException.class, () -> filter.doFilter(context));
    }

    /**
     * Simulate an extremely long but not continuously on ordinary long text to avoid accidental damage
     */
    @Test
    public void testFilter_withNormalLongText_shouldPass() {
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 20000; i++) {
            longText.append("safeText");
        }

        FullHttpRequest request = buildRequestWithQuery("/path?param=" + longText);
        HttpFilterContext context = new HttpFilterContext(request, () -> new HttpRequestParamWrapper(request));

        try {
            filter.doFilter(context);
        } catch (HttpRequestFilterException e) {
            throw new AssertionError("非XSS文本不应该被误拦", e);
        }
    }
}
