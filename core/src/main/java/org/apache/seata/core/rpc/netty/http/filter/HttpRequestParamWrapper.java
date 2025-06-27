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
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.codec.http.QueryStringDecoder;
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

    private void parseQueryParams(FullHttpRequest request) {
        QueryStringDecoder decoder = new QueryStringDecoder(request.uri());
        decoder.parameters().forEach(queryParams::put);
    }

    private void parseHeaders(FullHttpRequest request) {
        for (Map.Entry<String, String> entry : request.headers()) {
            headerParams.computeIfAbsent(entry.getKey(), k -> new ArrayList<>()).add(entry.getValue());
        }
    }

    private void parseBody(FullHttpRequest request) {
        String contentType = request.headers().get(HttpHeaderNames.CONTENT_TYPE);
        if (contentType == null) {
            return;
        }

        ByteBuf content = request.content();
        String bodyStr = content.toString(StandardCharsets.UTF_8);

        try {
            if (contentType.contains("application/json")) {
                parseJsonBody(bodyStr);
            } else if (contentType.contains("application/x-www-form-urlencoded") ||
                    contentType.contains("multipart/form-data")) {
                parseFormBody(request);
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
                    formParams.computeIfAbsent(attr.getName(), k -> new ArrayList<>()).add(attr.getValue());
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

        queryParams.forEach((k, v) -> all.computeIfAbsent(k, key -> new ArrayList<>()).addAll(v));
        formParams.forEach((k, v) -> all.computeIfAbsent(k, key -> new ArrayList<>()).addAll(v));
        headerParams.forEach((k, v) -> all.computeIfAbsent(k, key -> new ArrayList<>()).addAll(v));
        jsonParams.forEach((k, v) -> all.computeIfAbsent(k, key -> new ArrayList<>()).addAll(v));

        return all;
    }

}
