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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http2.Http2Headers;
import org.apache.seata.core.rpc.netty.http.RequestParseUtils;
import org.apache.seata.core.rpc.netty.http.SimpleHttp2Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public HttpRequestParamWrapper(
            Map<String, List<String>> queryParams,
            Map<String, List<String>> formParams,
            Map<String, List<String>> headerParams,
            Map<String, List<String>> jsonParams) {
        mergeParams(this.queryParams, queryParams);
        mergeParams(this.formParams, formParams);
        mergeParams(this.headerParams, headerParams);
        mergeParams(this.jsonParams, jsonParams);
    }

    public HttpRequestParamWrapper(HttpRequest httpRequest) {
        if (!(httpRequest instanceof FullHttpRequest)) {
            throw new IllegalArgumentException("HttpRequest must be FullHttpRequest to read body.");
        }
        FullHttpRequest fullRequest = (FullHttpRequest) httpRequest;
        parseQueryParams(fullRequest.uri());
        parseHeaders(fullRequest.headers());
        RequestParseUtils.BodyParseResult bodyParseResult = RequestParseUtils.parseBody(OBJECT_MAPPER, fullRequest);
        mergeParams(formParams, bodyParseResult.getFormParams());
        mergeParams(jsonParams, bodyParseResult.getJsonParams());
    }

    public HttpRequestParamWrapper(SimpleHttp2Request request) {
        if (request.getQueryParams() != null) {
            mergeParams(queryParams, request.getQueryParams());
        } else {
            parseQueryParams(request.getPath());
        }

        if (request.getHeaderParams() != null) {
            mergeParams(headerParams, request.getHeaderParams());
        } else if (request.getHeaders() != null) {
            parseHeaders(request.getHeaders());
        }

        boolean hasBodyMaps = request.getFormParams() != null || request.getJsonParams() != null;
        if (request.getFormParams() != null) {
            mergeParams(formParams, request.getFormParams());
        }
        if (request.getJsonParams() != null) {
            mergeParams(jsonParams, request.getJsonParams());
        }

        if (!hasBodyMaps && request.getBody() != null) {
            RequestParseUtils.BodyParseResult bodyParseResult =
                    RequestParseUtils.parseBody(OBJECT_MAPPER, request.getBody(), request.getHeaders());
            mergeParams(formParams, bodyParseResult.getFormParams());
            mergeParams(jsonParams, bodyParseResult.getJsonParams());
        }
    }

    private void parseQueryParams(String uri) {
        QueryStringDecoder decoder = new QueryStringDecoder(uri);
        queryParams.putAll(decoder.parameters());
    }

    private void parseHeaders(HttpHeaders headers) {
        headers.forEach(entry -> headerParams
                .computeIfAbsent(entry.getKey(), k -> new ArrayList<>())
                .add(entry.getValue()));
    }

    private void parseHeaders(Http2Headers headers) {
        headers.forEach(entry -> headerParams
                .computeIfAbsent(entry.getKey().toString(), k -> new ArrayList<>())
                .add(entry.getValue().toString()));
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

    private void mergeParams(Map<String, List<String>> target, Map<String, List<String>> source) {
        if (source == null || source.isEmpty()) {
            return;
        }
        source.forEach((key, values) -> {
            if (values == null || values.isEmpty()) {
                return;
            }
            List<String> targetList = target.computeIfAbsent(key, k -> new ArrayList<>());
            targetList.addAll(values);
        });
    }
}
