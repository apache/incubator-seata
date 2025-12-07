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

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http2.Http2Headers;

import java.util.List;
import java.util.Map;

public class SimpleHttp2Request {
    private final HttpMethod method;
    private final String path;
    private final Http2Headers headers;
    private final String body;
    private final Map<String, List<String>> queryParams;
    private final Map<String, List<String>> formParams;
    private final Map<String, List<String>> headerParams;
    private final Map<String, List<String>> jsonParams;
    private final ObjectNode bodyNode;

    public SimpleHttp2Request(HttpMethod method, String path, Http2Headers headers, String body) {
        this(method, path, headers, body, null, null, null, null, null);
    }

    public SimpleHttp2Request(
            HttpMethod method,
            String path,
            Http2Headers headers,
            String body,
            Map<String, List<String>> queryParams,
            Map<String, List<String>> formParams,
            Map<String, List<String>> headerParams,
            Map<String, List<String>> jsonParams,
            ObjectNode bodyNode) {
        this.method = method;
        this.path = path;
        this.headers = headers;
        this.body = body;
        this.queryParams = queryParams;
        this.formParams = formParams;
        this.headerParams = headerParams;
        this.jsonParams = jsonParams;
        this.bodyNode = bodyNode;
    }

    public HttpMethod getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public Http2Headers getHeaders() {
        return headers;
    }

    public String getBody() {
        return body;
    }

    public Map<String, List<String>> getQueryParams() {
        return queryParams;
    }

    public Map<String, List<String>> getFormParams() {
        return formParams;
    }

    public Map<String, List<String>> getHeaderParams() {
        return headerParams;
    }

    public Map<String, List<String>> getJsonParams() {
        return jsonParams;
    }

    public ObjectNode getBodyNode() {
        return bodyNode;
    }
}
