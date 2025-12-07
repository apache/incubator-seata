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
package org.apache.seata.core.rpc.netty.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.codec.http2.Http2Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Utility class that parses HTTP headers, query parameters and body payloads so handlers can reuse the same results.
 */
public final class RequestParseUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestParseUtils.class);

    private RequestParseUtils() {}

    public static QueryParseResult parseQuery(String uri) {
        QueryStringDecoder decoder = new QueryStringDecoder(uri);
        return new QueryParseResult(decoder.path(), deepCopyParameters(decoder.parameters()));
    }

    public static Map<String, List<String>> copyHeaders(HttpHeaders headers) {
        Map<String, List<String>> headerParams = new HashMap<>();
        for (Map.Entry<String, String> entry : headers) {
            String key = entry.getKey();
            headerParams.computeIfAbsent(key, k -> new ArrayList<>()).add(entry.getValue());
            String lowerKey = key != null ? key.toLowerCase(Locale.ROOT) : null;
            if (lowerKey != null && !lowerKey.equals(key)) {
                headerParams.computeIfAbsent(lowerKey, k -> new ArrayList<>()).add(entry.getValue());
            }
        }
        return headerParams;
    }

    public static Map<String, List<String>> copyHeaders(Http2Headers headers) {
        Map<String, List<String>> headerParams = new HashMap<>();
        for (Map.Entry<CharSequence, CharSequence> entry : headers) {
            String key = entry.getKey().toString();
            String value = entry.getValue().toString();
            headerParams.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
            String lowerKey = key.toLowerCase(Locale.ROOT);
            if (!lowerKey.equals(key)) {
                headerParams.computeIfAbsent(lowerKey, k -> new ArrayList<>()).add(value);
            }
        }
        return headerParams;
    }

    public static Map<String, List<String>> deepCopyParameters(Map<String, List<String>> source) {
        Map<String, List<String>> target = new HashMap<>();
        if (source == null) {
            return target;
        }
        source.forEach((key, values) -> {
            if (values != null) {
                target.put(key, new ArrayList<>(values));
            }
        });
        return target;
    }

    public static BodyParseResult parseBody(ObjectMapper objectMapper, FullHttpRequest request) {
        if (request == null || !request.content().isReadable()) {
            return BodyParseResult.empty();
        }
        String contentType = request.headers().get(HttpHeaderNames.CONTENT_TYPE);
        if (contentType == null) {
            return BodyParseResult.empty();
        }
        String lowerCaseContentType = contentType.toLowerCase(Locale.ROOT);
        String bodyString = request.content().toString(StandardCharsets.UTF_8);

        Map<String, List<String>> formParams = new HashMap<>();
        Map<String, List<String>> jsonParams = new HashMap<>();
        ObjectNode bodyNode = null;

        try {
            if (lowerCaseContentType.contains("application/json")) {
                JsonNode node = objectMapper.readTree(bodyString);
                if (node instanceof ObjectNode) {
                    bodyNode = (ObjectNode) node;
                    bodyNode.fields().forEachRemaining(entry -> jsonParams
                            .computeIfAbsent(entry.getKey(), k -> new ArrayList<>())
                            .add(entry.getValue().asText()));
                }
            } else if (lowerCaseContentType.contains("application/x-www-form-urlencoded")) {
                bodyNode = objectMapper.createObjectNode();
                decodeUrlEncoded(bodyString, formParams, bodyNode);
            } else if (lowerCaseContentType.contains("multipart/form-data")) {
                bodyNode = objectMapper.createObjectNode();
                HttpPostRequestDecoder decoder = null;
                try {
                    decoder = new HttpPostRequestDecoder(request);
                    for (InterfaceHttpData data : decoder.getBodyHttpDatas()) {
                        if (data.getHttpDataType() != InterfaceHttpData.HttpDataType.Attribute) {
                            continue;
                        }
                        Attribute attr = (Attribute) data;
                        try {
                            String name = attr.getName();
                            String value = attr.getValue();
                            formParams
                                    .computeIfAbsent(name, k -> new ArrayList<>())
                                    .add(value);
                            bodyNode.put(name, value);
                        } finally {
                            attr.release();
                        }
                    }
                } finally {
                    if (decoder != null) {
                        decoder.destroy();
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to parse HTTP/1.x body: {}", e.getMessage());
        }
        return new BodyParseResult(formParams, jsonParams, bodyNode);
    }

    public static BodyParseResult parseBody(ObjectMapper objectMapper, String body, Http2Headers headers) {
        if (body == null || body.isEmpty()) {
            return BodyParseResult.empty();
        }
        CharSequence contentTypeSeq = headers.get(HttpHeaderNames.CONTENT_TYPE);
        if (contentTypeSeq == null) {
            return BodyParseResult.empty();
        }
        String contentType = contentTypeSeq.toString();
        String lowerCaseContentType = contentType.toLowerCase(Locale.ROOT);

        Map<String, List<String>> formParams = new HashMap<>();
        Map<String, List<String>> jsonParams = new HashMap<>();
        ObjectNode bodyNode = null;
        try {
            if (lowerCaseContentType.contains("application/json")) {
                JsonNode node = objectMapper.readTree(body);
                if (node instanceof ObjectNode) {
                    bodyNode = (ObjectNode) node;
                    bodyNode.fields().forEachRemaining(entry -> jsonParams
                            .computeIfAbsent(entry.getKey(), k -> new ArrayList<>())
                            .add(entry.getValue().asText()));
                }
            } else if (lowerCaseContentType.contains("application/x-www-form-urlencoded")) {
                bodyNode = objectMapper.createObjectNode();
                decodeUrlEncoded(body, formParams, bodyNode);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to parse HTTP/2 body: {}", e.getMessage());
        }
        return new BodyParseResult(formParams, jsonParams, bodyNode);
    }

    private static void decodeUrlEncoded(String body, Map<String, List<String>> formParams, ObjectNode bodyNode) {
        if (body == null || body.isEmpty()) {
            return;
        }
        String[] pairs = body.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            if (kv.length != 2) {
                continue;
            }
            String key = decodeComponent(kv[0]);
            String value = decodeComponent(kv[1]);
            formParams.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
            bodyNode.put(key, value);
        }
    }

    private static String decodeComponent(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            LOGGER.warn("Failed to decode form field: {}", value);
            return value;
        }
    }

    public static final class QueryParseResult {
        private final String path;
        private final Map<String, List<String>> parameters;

        QueryParseResult(String path, Map<String, List<String>> parameters) {
            this.path = path;
            this.parameters = parameters;
        }

        public String getPath() {
            return path;
        }

        public Map<String, List<String>> getParameters() {
            return parameters;
        }
    }

    public static final class BodyParseResult {
        private final Map<String, List<String>> formParams;
        private final Map<String, List<String>> jsonParams;
        private final ObjectNode bodyNode;

        BodyParseResult(
                Map<String, List<String>> formParams, Map<String, List<String>> jsonParams, ObjectNode bodyNode) {
            this.formParams = formParams;
            this.jsonParams = jsonParams;
            this.bodyNode = bodyNode;
        }

        public static BodyParseResult empty() {
            return new BodyParseResult(new HashMap<>(), new HashMap<>(), null);
        }

        public Map<String, List<String>> getFormParams() {
            return formParams;
        }

        public Map<String, List<String>> getJsonParams() {
            return jsonParams;
        }

        public ObjectNode getBodyNode() {
            return bodyNode;
        }
    }
}
