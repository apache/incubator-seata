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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import org.apache.seata.common.rpc.http.HttpContext;
import org.apache.seata.common.thread.NamedThreadFactory;
import org.apache.seata.core.rpc.netty.NettyServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * A Netty HTTP request handler that dispatches incoming requests to corresponding controller methods
 */
public class HttpDispatchHandler extends SimpleChannelInboundHandler<HttpRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpDispatchHandler.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * HTTP request processing thread pool, independent of Netty IO threads, to avoid blocking network processing.
     */
    private static final ExecutorService HTTP_HANDLER_THREADS = new ThreadPoolExecutor(
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

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpRequest httpRequest) {
        try {
            boolean keepAlive = HttpUtil.isKeepAlive(httpRequest)
                    && httpRequest.protocolVersion().isKeepAliveDefault();
            QueryStringDecoder queryStringDecoder = new QueryStringDecoder(httpRequest.uri());
            String path = queryStringDecoder.path();
            HttpInvocation httpInvocation = ControllerManager.getHttpInvocation(path);

            if (httpInvocation == null) {
                sendErrorResponse(ctx, HttpResponseStatus.NOT_FOUND, keepAlive);
                return;
            }

            HttpContext httpContext = new HttpContext(httpRequest, ctx, keepAlive);
            ObjectNode requestDataNode = OBJECT_MAPPER.createObjectNode();
            requestDataNode.putIfAbsent("param", ParameterParser.convertParamMap(queryStringDecoder.parameters()));

            if (httpRequest.method() == HttpMethod.POST) {
                HttpPostRequestDecoder httpPostRequestDecoder = null;
                try {
                    httpPostRequestDecoder = new HttpPostRequestDecoder(httpRequest);
                    ObjectNode bodyDataNode = OBJECT_MAPPER.createObjectNode();
                    for (InterfaceHttpData interfaceHttpData : httpPostRequestDecoder.getBodyHttpDatas()) {
                        if (interfaceHttpData.getHttpDataType() != InterfaceHttpData.HttpDataType.Attribute) {
                            continue;
                        }
                        Attribute attribute = (Attribute) interfaceHttpData;
                        bodyDataNode.put(attribute.getName(), attribute.getValue());
                    }
                    requestDataNode.putIfAbsent("body", bodyDataNode);
                } finally {
                    if (httpPostRequestDecoder != null) {
                        httpPostRequestDecoder.destroy();
                    }
                }
            }

            Object httpController = httpInvocation.getController();
            Method handleMethod = httpInvocation.getMethod();
            Object[] args = ParameterParser.getArgValues(
                    httpInvocation.getParamMetaData(), handleMethod, requestDataNode, httpContext);

            try {
                HTTP_HANDLER_THREADS.execute(() -> {
                    try {
                        Object result = handleMethod.invoke(httpController, args);
                        if (httpContext.isAsync()) {
                            return;
                        }

                        sendResponse(ctx, keepAlive, result);
                    } catch (IllegalArgumentException e) {
                        LOGGER.error("Illegal argument exception: {}", e.getMessage(), e);
                        sendErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST, false);
                    } catch (Exception e) {
                        LOGGER.error("Exception occurred while processing HTTP request: {}", e.getMessage(), e);
                        sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, false);
                    }
                });
            } catch (RejectedExecutionException e) {
                LOGGER.error("HTTP thread pool is full: {}", e.getMessage(), e);
                sendErrorResponse(ctx, HttpResponseStatus.SERVICE_UNAVAILABLE, false);
            }
        } catch (Exception e) {
            LOGGER.error("Exception occurred while processing HTTP request: {}", e.getMessage(), e);
            sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, false);
        }
    }

    private void sendResponse(ChannelHandlerContext ctx, boolean keepAlive, Object result)
            throws JsonProcessingException {
        FullHttpResponse response;
        if (result != null) {
            byte[] body = OBJECT_MAPPER.writeValueAsBytes(result);
            response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.wrappedBuffer(body));
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, body.length);
        } else {
            response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.wrappedBuffer(Unpooled.EMPTY_BUFFER));
        }
        if (!keepAlive) {
            ctx.writeAndFlush(response).addListeners(ChannelFutureListener.CLOSE);
        } else {
            ctx.writeAndFlush(response);
        }
    }

    private void sendErrorResponse(ChannelHandlerContext ctx, HttpResponseStatus status, boolean keepAlive) {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, status, Unpooled.wrappedBuffer(Unpooled.EMPTY_BUFFER));
        if (!keepAlive) {
            ctx.writeAndFlush(response).addListeners(ChannelFutureListener.CLOSE);
        } else {
            ctx.writeAndFlush(response);
        }
    }
}
