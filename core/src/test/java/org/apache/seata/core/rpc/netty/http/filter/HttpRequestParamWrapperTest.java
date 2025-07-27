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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HttpRequestParamWrapperTest {

    @Test
    void testParseQueryParams() {
        FullHttpRequest req = mock(FullHttpRequest.class);
        when(req.uri()).thenReturn("/path?city=shanghai&city=beijing&hello=world");
        when(req.headers()).thenReturn(new DefaultHttpHeaders());
        when(req.content()).thenReturn(Unpooled.EMPTY_BUFFER);

        HttpRequestParamWrapper wrapper = new HttpRequestParamWrapper(req);

        Map<String, List<String>> all = wrapper.getAllParamsAsMultiMap();

        assertThat(all.get("city")).containsExactly("shanghai", "beijing");
        assertThat(all.get("hello")).containsExactly("world");
    }

    @Test
    void testParseHeaders() {
        FullHttpRequest req = mock(FullHttpRequest.class);
        when(req.uri()).thenReturn("/path");
        HttpHeaders headers = new DefaultHttpHeaders();
        headers.add("X-Custom", "value1");
        headers.add("X-Custom", "value2");
        when(req.headers()).thenReturn(headers);
        when(req.content()).thenReturn(Unpooled.EMPTY_BUFFER);

        HttpRequestParamWrapper wrapper = new HttpRequestParamWrapper(req);

        Map<String, List<String>> all = wrapper.getAllParamsAsMultiMap();

        assertThat(all.get("X-Custom")).containsExactly("value1", "value2");
    }

    @Test
    void testParseJsonBody() {
        FullHttpRequest req = mock(FullHttpRequest.class);
        when(req.uri()).thenReturn("/path");
        HttpHeaders headers = new DefaultHttpHeaders();
        headers.set(HttpHeaderNames.CONTENT_TYPE, "application/json");
        when(req.headers()).thenReturn(headers);

        String json = "{\"key1\":\"value1\", \"key2\":\"value2\"}";
        ByteBuf buf = Unpooled.copiedBuffer(json, StandardCharsets.UTF_8);
        when(req.content()).thenReturn(buf);

        HttpRequestParamWrapper wrapper = new HttpRequestParamWrapper(req);

        Map<String, List<String>> all = wrapper.getAllParamsAsMultiMap();

        assertThat(all.get("key1")).containsExactly("value1");
        assertThat(all.get("key2")).containsExactly("value2");
    }

    @Test
    void testParseFormBody() throws Exception {
        FullHttpRequest req = mock(FullHttpRequest.class);
        when(req.method()).thenReturn(HttpMethod.POST);
        when(req.uri()).thenReturn("/path");
        HttpHeaders headers = new DefaultHttpHeaders();
        headers.set(HttpHeaderNames.CONTENT_TYPE, "application/x-www-form-urlencoded");
        when(req.headers()).thenReturn(headers);

        String formBody = "param1=value1&param2=value2";
        ByteBuf buf = Unpooled.copiedBuffer(formBody, StandardCharsets.UTF_8);
        when(req.content()).thenReturn(buf);

        DefaultFullHttpRequest realReq =
                new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/path", buf.retainedDuplicate());
        realReq.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/x-www-form-urlencoded");

        HttpRequestParamWrapper wrapper = new HttpRequestParamWrapper(realReq);

        Map<String, List<String>> all = wrapper.getAllParamsAsMultiMap();

        assertThat(all.get("param1")).containsExactly("value1");
        assertThat(all.get("param2")).containsExactly("value2");
    }
}
