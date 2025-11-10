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
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2Headers;
import org.apache.seata.core.rpc.netty.http.SimpleHttp2Request;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    @Test
    void testIllegalArgumentExceptionForNonFullHttpRequest() {
        HttpRequest req = mock(HttpRequest.class);

        assertThatThrownBy(() -> new HttpRequestParamWrapper(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("HttpRequest must be FullHttpRequest to read body");
    }

    @Test
    void testSimpleHttp2RequestWithJsonBody() {
        Http2Headers headers = new DefaultHttp2Headers();
        headers.set(HttpHeaderNames.CONTENT_TYPE, "application/json");

        String json = "{\"key1\":\"value1\", \"key2\":\"value2\"}";
        SimpleHttp2Request request = new SimpleHttp2Request(HttpMethod.POST, "/path?query=param", headers, json);

        HttpRequestParamWrapper wrapper = new HttpRequestParamWrapper(request);

        Map<String, List<String>> all = wrapper.getAllParamsAsMultiMap();

        assertThat(all.get("query")).containsExactly("param");
        assertThat(all.get("key1")).containsExactly("value1");
        assertThat(all.get("key2")).containsExactly("value2");
    }

    @Test
    void testSimpleHttp2RequestWithFormUrlEncodedBody() {
        Http2Headers headers = new DefaultHttp2Headers();
        headers.set(HttpHeaderNames.CONTENT_TYPE, "application/x-www-form-urlencoded");

        String formBody = "param1=value1&param2=value2";
        SimpleHttp2Request request = new SimpleHttp2Request(HttpMethod.POST, "/path", headers, formBody);

        HttpRequestParamWrapper wrapper = new HttpRequestParamWrapper(request);

        Map<String, List<String>> all = wrapper.getAllParamsAsMultiMap();

        assertThat(all.get("param1")).containsExactly("value1");
        assertThat(all.get("param2")).containsExactly("value2");
    }

    @Test
    void testSimpleHttp2RequestWithNoContentType() {
        Http2Headers headers = new DefaultHttp2Headers();
        SimpleHttp2Request request = new SimpleHttp2Request(HttpMethod.GET, "/path?key=value", headers, "");

        HttpRequestParamWrapper wrapper = new HttpRequestParamWrapper(request);

        Map<String, List<String>> all = wrapper.getAllParamsAsMultiMap();

        assertThat(all.get("key")).containsExactly("value");
    }

    @Test
    void testSimpleHttp2RequestWithHeaders() {
        Http2Headers headers = new DefaultHttp2Headers();
        headers.add("x-custom-header", "value1");
        headers.add("x-custom-header", "value2");

        SimpleHttp2Request request = new SimpleHttp2Request(HttpMethod.GET, "/path", headers, "");

        HttpRequestParamWrapper wrapper = new HttpRequestParamWrapper(request);

        Map<String, List<String>> all = wrapper.getAllParamsAsMultiMap();

        assertThat(all.get("x-custom-header")).containsExactly("value1", "value2");
    }

    @Test
    void testInvalidJsonBodyHandling() {
        FullHttpRequest req = mock(FullHttpRequest.class);
        when(req.uri()).thenReturn("/path");
        HttpHeaders headers = new DefaultHttpHeaders();
        headers.set(HttpHeaderNames.CONTENT_TYPE, "application/json");
        when(req.headers()).thenReturn(headers);

        String invalidJson = "{invalid json}";
        ByteBuf buf = Unpooled.copiedBuffer(invalidJson, StandardCharsets.UTF_8);
        when(req.content()).thenReturn(buf);

        HttpRequestParamWrapper wrapper = new HttpRequestParamWrapper(req);

        Map<String, List<String>> all = wrapper.getAllParamsAsMultiMap();

        // Should contain content-type header but no JSON parameters due to invalid JSON
        assertThat(all).containsKey("content-type");
        assertThat(all.get("content-type")).containsExactly("application/json");
    }

    @Test
    void testEmptyBody() {
        FullHttpRequest req = mock(FullHttpRequest.class);
        when(req.uri()).thenReturn("/path");
        HttpHeaders headers = new DefaultHttpHeaders();
        headers.set(HttpHeaderNames.CONTENT_TYPE, "application/json");
        when(req.headers()).thenReturn(headers);
        when(req.content()).thenReturn(Unpooled.EMPTY_BUFFER);

        HttpRequestParamWrapper wrapper = new HttpRequestParamWrapper(req);

        Map<String, List<String>> all = wrapper.getAllParamsAsMultiMap();

        // Should contain content-type header but no JSON parameters due to empty body
        assertThat(all).containsKey("content-type");
        assertThat(all.get("content-type")).containsExactly("application/json");
    }

    @Test
    void testNullContentType() {
        FullHttpRequest req = mock(FullHttpRequest.class);
        when(req.uri()).thenReturn("/path?key=value");
        when(req.headers()).thenReturn(new DefaultHttpHeaders());
        when(req.content()).thenReturn(Unpooled.EMPTY_BUFFER);

        HttpRequestParamWrapper wrapper = new HttpRequestParamWrapper(req);

        Map<String, List<String>> all = wrapper.getAllParamsAsMultiMap();

        assertThat(all.get("key")).containsExactly("value");
    }

    @Test
    void testUrlEncodedParametersInQuery() {
        FullHttpRequest req = mock(FullHttpRequest.class);
        when(req.uri()).thenReturn("/path?name=hello%20world&city=%E4%B8%8A%E6%B5%B7");
        when(req.headers()).thenReturn(new DefaultHttpHeaders());
        when(req.content()).thenReturn(Unpooled.EMPTY_BUFFER);

        HttpRequestParamWrapper wrapper = new HttpRequestParamWrapper(req);

        Map<String, List<String>> all = wrapper.getAllParamsAsMultiMap();

        assertThat(all.get("name")).containsExactly("hello world");
        assertThat(all.get("city")).containsExactly("上海");
    }

    @Test
    void testUrlEncodedParametersInForm() {
        Http2Headers headers = new DefaultHttp2Headers();
        headers.set(HttpHeaderNames.CONTENT_TYPE, "application/x-www-form-urlencoded");

        String formBody = "name=hello%20world&city=%E4%B8%8A%E6%B5%B7";
        SimpleHttp2Request request = new SimpleHttp2Request(HttpMethod.POST, "/path", headers, formBody);

        HttpRequestParamWrapper wrapper = new HttpRequestParamWrapper(request);

        Map<String, List<String>> all = wrapper.getAllParamsAsMultiMap();

        assertThat(all.get("name")).containsExactly("hello world");
        assertThat(all.get("city")).containsExactly("上海");
    }

    @Test
    void testEmptyFormUrlEncodedBody() {
        Http2Headers headers = new DefaultHttp2Headers();
        headers.set(HttpHeaderNames.CONTENT_TYPE, "application/x-www-form-urlencoded");

        SimpleHttp2Request request = new SimpleHttp2Request(HttpMethod.POST, "/path", headers, "");

        HttpRequestParamWrapper wrapper = new HttpRequestParamWrapper(request);

        Map<String, List<String>> all = wrapper.getAllParamsAsMultiMap();

        // Should contain content-type header but no form parameters due to empty body
        assertThat(all).containsKey("content-type");
        assertThat(all.get("content-type")).containsExactly("application/x-www-form-urlencoded");
    }

    @Test
    void testFormUrlEncodedBodyWithMalformedPairs() {
        Http2Headers headers = new DefaultHttp2Headers();
        headers.set(HttpHeaderNames.CONTENT_TYPE, "application/x-www-form-urlencoded");

        String formBody = "key1=value1&invalidpair&key2=value2";
        SimpleHttp2Request request = new SimpleHttp2Request(HttpMethod.POST, "/path", headers, formBody);

        HttpRequestParamWrapper wrapper = new HttpRequestParamWrapper(request);

        Map<String, List<String>> all = wrapper.getAllParamsAsMultiMap();

        assertThat(all.get("key1")).containsExactly("value1");
        assertThat(all.get("key2")).containsExactly("value2");
        assertThat(all.get("invalidpair")).isNull();
    }

    @Test
    void testMultipartFormData() {
        // Create proper multipart form data
        String multipartData = "--boundary\r\n" + "Content-Disposition: form-data; name=\"field1\"\r\n"
                + "\r\n"
                + "value1\r\n"
                + "--boundary\r\n"
                + "Content-Disposition: form-data; name=\"field2\"\r\n"
                + "\r\n"
                + "value2\r\n"
                + "--boundary--\r\n";

        ByteBuf buf = Unpooled.copiedBuffer(multipartData, StandardCharsets.UTF_8);

        DefaultFullHttpRequest req = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/path", buf);
        req.headers().set(HttpHeaderNames.CONTENT_TYPE, "multipart/form-data; boundary=boundary");

        HttpRequestParamWrapper wrapper = new HttpRequestParamWrapper(req);

        Map<String, List<String>> all = wrapper.getAllParamsAsMultiMap();

        // Should contain both form parameters and content-type header
        assertThat(all).containsKey("content-type");
        assertThat(all.get("content-type")).containsExactly("multipart/form-data; boundary=boundary");
        assertThat(all.get("field1")).containsExactly("value1");
        assertThat(all.get("field2")).containsExactly("value2");
    }

    @Test
    void testMergeParamsFromMultipleSources() {
        Http2Headers headers = new DefaultHttp2Headers();
        headers.set(HttpHeaderNames.CONTENT_TYPE, "application/json");
        headers.add("x-param", "headerValue");

        String json = "{\"jsonKey\":\"jsonValue\"}";
        SimpleHttp2Request request =
                new SimpleHttp2Request(HttpMethod.POST, "/path?queryKey=queryValue", headers, json);

        HttpRequestParamWrapper wrapper = new HttpRequestParamWrapper(request);

        Map<String, List<String>> all = wrapper.getAllParamsAsMultiMap();

        assertThat(all.get("queryKey")).containsExactly("queryValue");
        assertThat(all.get("jsonKey")).containsExactly("jsonValue");
        assertThat(all.get("x-param")).containsExactly("headerValue");
    }

    @Test
    void testSameKeyFromMultipleSources() {
        Http2Headers headers = new DefaultHttp2Headers();
        headers.set(HttpHeaderNames.CONTENT_TYPE, "application/json");
        headers.add("key", "headerValue");

        String json = "{\"key\":\"jsonValue\"}";
        SimpleHttp2Request request = new SimpleHttp2Request(HttpMethod.POST, "/path?key=queryValue", headers, json);

        HttpRequestParamWrapper wrapper = new HttpRequestParamWrapper(request);

        Map<String, List<String>> all = wrapper.getAllParamsAsMultiMap();

        assertThat(all.get("key")).containsExactlyInAnyOrder("queryValue", "jsonValue", "headerValue");
    }

    @Test
    void testJsonNonObjectBody() {
        FullHttpRequest req = mock(FullHttpRequest.class);
        when(req.uri()).thenReturn("/path");
        HttpHeaders headers = new DefaultHttpHeaders();
        headers.set(HttpHeaderNames.CONTENT_TYPE, "application/json");
        when(req.headers()).thenReturn(headers);

        String jsonArray = "[\"value1\", \"value2\"]";
        ByteBuf buf = Unpooled.copiedBuffer(jsonArray, StandardCharsets.UTF_8);
        when(req.content()).thenReturn(buf);

        HttpRequestParamWrapper wrapper = new HttpRequestParamWrapper(req);

        Map<String, List<String>> all = wrapper.getAllParamsAsMultiMap();

        // Should contain content-type header but no JSON parameters due to non-object JSON
        assertThat(all).containsKey("content-type");
        assertThat(all.get("content-type")).containsExactly("application/json");
    }

    @Test
    void testSimpleHttp2RequestInvalidJsonBody() {
        Http2Headers headers = new DefaultHttp2Headers();
        headers.set(HttpHeaderNames.CONTENT_TYPE, "application/json");

        String invalidJson = "{invalid}";
        SimpleHttp2Request request = new SimpleHttp2Request(HttpMethod.POST, "/path", headers, invalidJson);

        HttpRequestParamWrapper wrapper = new HttpRequestParamWrapper(request);

        Map<String, List<String>> all = wrapper.getAllParamsAsMultiMap();

        // Should contain content-type header but no JSON parameters due to invalid JSON
        assertThat(all).containsKey("content-type");
        assertThat(all.get("content-type")).containsExactly("application/json");
    }

    @Test
    void testInvalidMultipartFormDataHandling() {
        // Create malformed multipart form data without proper content-disposition
        String malformedMultipartData =
                "--boundary\r\n" + "Invalid-Header: value\r\n" + "\r\n" + "some data\r\n" + "--boundary--\r\n";

        ByteBuf buf = Unpooled.copiedBuffer(malformedMultipartData, StandardCharsets.UTF_8);

        DefaultFullHttpRequest req = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/path", buf);
        // Set malformed content-type to trigger exception in parseFormBody
        req.headers().set(HttpHeaderNames.CONTENT_TYPE, "multipart/form-data");

        // Should handle exception gracefully and not throw
        HttpRequestParamWrapper wrapper = new HttpRequestParamWrapper(req);

        Map<String, List<String>> all = wrapper.getAllParamsAsMultiMap();

        // Should still contain content-type header
        assertThat(all).containsKey("content-type");
        assertThat(all.get("content-type")).containsExactly("multipart/form-data");
    }
}
