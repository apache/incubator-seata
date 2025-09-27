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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.seata.common.thread.NamedThreadFactory;
import org.apache.seata.core.exception.HttpRequestFilterException;
import org.apache.seata.core.rpc.netty.NettyServerConfig;
import org.apache.seata.core.rpc.netty.http.filter.HttpFilterContext;
import org.apache.seata.core.rpc.netty.http.filter.HttpRequestFilterChain;
import org.apache.seata.core.rpc.netty.http.filter.HttpRequestFilterManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public abstract class BaseHttpChannelHandler<T> extends SimpleChannelInboundHandler<T> {

    protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    /**
     * HTTP request processing thread pool, independent of Netty IO threads, to avoid blocking network processing.
     */
    protected static final ExecutorService HTTP_HANDLER_THREADS = new ThreadPoolExecutor(
            NettyServerConfig.getMinHttpPoolSize(),
            NettyServerConfig.getMaxHttpPoolSize(),
            NettyServerConfig.getHttpKeepAliveTime(),
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(NettyServerConfig.getMaxHttpTaskQueueSize()),
            new NamedThreadFactory("HTTPHandlerThread", NettyServerConfig.getMaxHttpPoolSize()),
            new ThreadPoolExecutor.AbortPolicy());

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(HTTP_HANDLER_THREADS::shutdown));
    }

    /**
     * The filter has a unified entry point and is called by subclasses at an appropriate time
     */
    protected final void doFilterInternal(HttpFilterContext<?> context) throws HttpRequestFilterException {
        HttpRequestFilterChain filterChain = HttpRequestFilterManager.getFilterChain();
        filterChain.doFilter(context);
    }
}
