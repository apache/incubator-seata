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

import io.netty.channel.ChannelHandlerContext;
import org.apache.seata.common.rpc.http.HttpContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class HttpFilterContext<T> extends HttpContext<T> {
    private final Supplier<HttpRequestParamWrapper> paramWrapperSupplier;
    private volatile HttpRequestParamWrapper paramWrapper;
    private final Map<String, Object> attributes = new ConcurrentHashMap<>();
    private static final ThreadLocal<HttpFilterContext<?>> CURRENT_CONTEXT = new ThreadLocal<>();
    private Object response;

    public HttpFilterContext(
            T request,
            ChannelHandlerContext channelHandlerContext,
            boolean keepAlive,
            String httpVersion,
            Supplier<HttpRequestParamWrapper> paramWrapperSupplier) {
        super(request, channelHandlerContext, keepAlive, httpVersion);
        this.paramWrapperSupplier = paramWrapperSupplier;
    }

    public static HttpFilterContext<?> getCurrentContext() {
        return CURRENT_CONTEXT.get();
    }

    public static void setCurrentContext(HttpFilterContext<?> context) {
        CURRENT_CONTEXT.set(context);
    }

    public static void clearCurrentContext() {
        CURRENT_CONTEXT.remove();
    }

    public void setResponse(Object response) {
        this.response = response;
    }

    public Object getResponse() {
        return this.response;
    }

    public HttpRequestParamWrapper getParamWrapper() {
        if (paramWrapper == null) {
            synchronized (this) {
                if (paramWrapper == null) {
                    paramWrapper = paramWrapperSupplier.get();
                }
            }
        }
        return paramWrapper;
    }

    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <V> V getAttribute(String key) {
        return (V) attributes.get(key);
    }

    public Object removeAttribute(String key) {
        return attributes.remove(key);
    }
}
