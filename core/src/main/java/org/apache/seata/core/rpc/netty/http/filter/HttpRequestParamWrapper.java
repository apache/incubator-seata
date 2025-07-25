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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.codec.http2.Http2Headers;
import org.apache.seata.core.rpc.netty.http.SimpleHttp2Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Wrapper for HTTP request parameters from multiple sources: query, form, header, JSON body.
 */
public class HttpRequestParamWrapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpRequestParamWrapper.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final Map<String, List<String>> queryParams = new HashMap<>();
    private final Map<String, List<String>> formParams = new HashMap<>();
    private final Map<String, List<String>> headerParams = new HashMap<>();
    private final Map<String, List<String>> jsonParams = new HashMap<>();

    public HttpRequestParamWrapper(HttpRequest httpRequest) {
        if (!(httpRequest instanceof FullHttpRequest)) {
            throw new IllegalArgumentException("HttpRequest must be FullHttpRequest to read body.");
        }
        FullHttpRequest fullRequest = (FullHttpRequest) httpRequest;
        parseQueryParams(fullRequest);
        parseHeaders(fullRequest);
        parseBody(fullRequest);
    }

    public HttpRequestParamWrapper(SimpleHttp2Request request) {
        parseQueryParams(request.getPath());
        parseHeaders(request.getHeaders());

        String contentType = (String) request.getHeaders().get(HttpHeaderNames.CONTENT_TYPE);
        if (contentType == null) {
            return;
        }

        try {
            if (contentType.contains("application/json")) {
                parseJsonBody(request.getBody());
            } else if (contentType.contains("application/x-www-form-urlencoded")) {
                parseFormUrlEncodedBody(request.getBody());
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to parse HTTP/2 body: {}", e.getMessage(), e);
        }
    }

    private void parseQueryParams(FullHttpRequest request) {
        QueryStringDecoder decoder = new QueryStringDecoder(request.uri());
        queryParams.putAll(decoder.parameters());
    }

    private void parseQueryParams(String path) {
        QueryStringDecoder decoder = new QueryStringDecoder(path);
        queryParams.putAll(decoder.parameters());
    }

    private void parseHeaders(FullHttpRequest request) {
        for (Map.Entry<String, String> entry : request.headers()) {
            headerParams.computeIfAbsent(entry.getKey(), k -> new ArrayList<>()).add(entry.getValue());
        }
    }

    private void parseHeaders(Http2Headers headers) {
        for (Map.Entry<CharSequence, CharSequence> entry : headers) {
            headerParams
                    .computeIfAbsent(entry.getKey().toString(), k -> new ArrayList<>())
                    .add(entry.getValue().toString());
        }
    }

    private void parseFormUrlEncodedBody(String body) {
        if (body == null || body.trim().isEmpty()) {
            return;
        }
        String[] pairs = body.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                String key = decode(kv[0]);
                String value = decode(kv[1]);
                formParams.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
            }
        }
    }

    private String decode(String s) {
        try {
            return java.net.URLDecoder.decode(s, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            LOGGER.warn("Failed to decode form field: {}", s, e);
            return s;
        }
    }

    private void parseBody(FullHttpRequest request) {
        String contentType = request.headers().get(HttpHeaderNames.CONTENT_TYPE);
        if (contentType == null) {
            return;
        }

        ByteBuf originalContent = request.content();
        ByteBuf copiedBuf = Unpooled.copiedBuffer(originalContent);

        String bodyStr = copiedBuf.toString(StandardCharsets.UTF_8);

        try {
            if (contentType.contains("application/json")) {
                parseJsonBody(bodyStr);
            } else if (contentType.contains("application/x-www-form-urlencoded")
                    || contentType.contains("multipart/form-data")) {
                // Replace user-controlled URI with constant string during internal FullHttpRequest construction for
                // decoding form parameters.
                FullHttpRequest copiedRequest = new DefaultFullHttpRequest(
                        request.protocolVersion(), request.method(), "/internal-safe-uri", copiedBuf);
                parseFormBody(copiedRequest);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to parse HTTP body: {}", e.getMessage(), e);
        }
    }

    private void parseJsonBody(String bodyStr) {
        try {
            JsonNode jsonNode = OBJECT_MAPPER.readTree(bodyStr);
            if (jsonNode != null && jsonNode.isObject()) {
                jsonNode.fields().forEachRemaining(e -> jsonParams
                        .computeIfAbsent(e.getKey(), k -> new ArrayList<>())
                        .add(e.getValue().asText()));
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to parse JSON body: {}", e.getMessage(), e);
        }
    }

    private void parseFormBody(FullHttpRequest request) {
        HttpPostRequestDecoder decoder = null;
        try {
            decoder = new HttpPostRequestDecoder(request);
            for (InterfaceHttpData data : decoder.getBodyHttpDatas()) {
                if (data.getHttpDataType() == InterfaceHttpData.HttpDataType.Attribute) {
                    Attribute attr = (Attribute) data;
                    formParams
                            .computeIfAbsent(attr.getName(), k -> new ArrayList<>())
                            .add(attr.getValue());
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to parse form body: {}", e.getMessage(), e);
        } finally {
            if (decoder != null) {
                decoder.destroy();
            }
        }
    }

    /**
     * Return all parameters from query, form, header and json, merged into a multi-value map.
     */
    public Map<String, List<String>> getAllParamsAsMultiMap() {
        Map<String, List<String>> all = new HashMap<>();

        queryParams.forEach(
                (k, v) -> all.computeIfAbsent(k, key -> new ArrayList<>()).addAll(v));
        formParams.forEach(
                (k, v) -> all.computeIfAbsent(k, key -> new ArrayList<>()).addAll(v));
        headerParams.forEach(
                (k, v) -> all.computeIfAbsent(k, key -> new ArrayList<>()).addAll(v));
        jsonParams.forEach(
                (k, v) -> all.computeIfAbsent(k, key -> new ArrayList<>()).addAll(v));

        return all;
    }
}
