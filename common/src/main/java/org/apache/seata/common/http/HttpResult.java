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
package org.apache.seata.common.http;

public class HttpResult<T> {

    private int statusCode;
    private String responseBody;
    private T rawResponse;

    public HttpResult() {}

    public HttpResult(int statusCode, String responseBody, T rawResponse) {
        this.statusCode = statusCode;
        this.responseBody = responseBody;
        this.rawResponse = rawResponse;
    }

    public int getStatusCode() { return statusCode; }
    public void setStatusCode(int statusCode) { this.statusCode = statusCode; }

    public String getResponseBody() { return responseBody; }
    public void setResponseBody(String responseBody) { this.responseBody = responseBody; }

    public T getRawResponse() { return rawResponse; }
    public void setRawResponse(T rawResponse) { this.rawResponse = rawResponse; }
}
