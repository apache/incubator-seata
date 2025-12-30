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
package org.apache.seata.mcp.exception;

import org.springframework.http.HttpStatusCode;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class ServiceCallException extends RuntimeException {
    private final HttpStatusCode httpStatus;
    private final Instant timestamp;

    public ServiceCallException(String message, Throwable cause) {
        super(message, cause);
        this.httpStatus = null;
        this.timestamp = Instant.now();
    }

    public ServiceCallException(String message) {
        super(message);
        this.httpStatus = null;
        this.timestamp = Instant.now();
    }

    public ServiceCallException(String message, HttpStatusCode httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
        this.timestamp = Instant.now();
    }

    public Map<String, Object> toErrorResponse() {
        Map<String, Object> error = new HashMap<>();
        error.put("message", this.getMessage());
        error.put("timestamp", timestamp.toString());
        error.put("httpStatus", httpStatus != null ? httpStatus.value() : null);
        return error;
    }
}
