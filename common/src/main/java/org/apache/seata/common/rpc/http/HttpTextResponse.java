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
package org.apache.seata.common.rpc.http;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A controller response whose body is sent as UTF-8 text.
 */
public final class HttpTextResponse {
    private final int status;
    private final String contentType;
    private final String body;
    private final Map<String, String> headers;

    public HttpTextResponse(int status, String contentType, String body) {
        this(status, contentType, body, Collections.emptyMap());
    }

    public HttpTextResponse(int status, String contentType, String body, Map<String, String> headers) {
        this.status = status;
        this.contentType = Objects.requireNonNull(contentType, "contentType");
        this.body = Objects.requireNonNull(body, "body");
        this.headers = Collections.unmodifiableMap(new LinkedHashMap<>(headers));
    }

    public int getStatus() {
        return status;
    }

    public String getContentType() {
        return contentType;
    }

    public String getBody() {
        return body;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }
}
