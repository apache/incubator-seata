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

import io.netty.channel.ChannelHandlerContext;

public class HttpContext<T> {

    public static final String HTTP_1_1 = "HTTP/1.1";
    public static final String HTTP_2_0 = "HTTP/2.0";

    private T request;

    private ChannelHandlerContext context;

    private boolean keepAlive;

    private boolean async = false;

    private String httpVersion;

    public HttpContext(T request, ChannelHandlerContext context, boolean keepAlive, String httpVersion) {
        this.request = request;
        this.context = context;
        this.keepAlive = keepAlive;
        this.httpVersion = httpVersion;
    }

    public HttpContext(T request, ChannelHandlerContext context, boolean keepAlive) {
        this.request = request;
        this.context = context;
        this.keepAlive = keepAlive;
        this.httpVersion = HTTP_1_1;
    }

    public boolean isHttp2() {
        return HTTP_2_0.equals(httpVersion);
    }

    public T getRequest() {
        return request;
    }

    public void setRequest(T request) {
        this.request = request;
    }

    public ChannelHandlerContext getContext() {
        return context;
    }

    public void setContext(ChannelHandlerContext context) {
        this.context = context;
    }

    public boolean isKeepAlive() {
        return keepAlive;
    }

    public void setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
    }

    public boolean isAsync() {
        return async;
    }

    public void setAsync(boolean async) {
        this.async = async;
    }

    public String getHttpVersion() {
        return httpVersion;
    }

    public void setHttpVersion(String httpVersion) {
        this.httpVersion = httpVersion;
    }
}
