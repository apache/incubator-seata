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
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2HeadersFrame;
import io.netty.handler.codec.http2.Http2StreamFrame;
import org.apache.seata.common.rpc.http.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

/**
 * The http2 http handler.
 */
public class Http2HttpHandler extends BaseHttpChannelHandler<Http2StreamFrame> {
    private static final Logger LOGGER = LoggerFactory.getLogger(Http2HttpHandler.class);
    private Http2Headers http2Headers;
    private ByteBuf bodyBuffer;
    private boolean headersEndStream = false;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Http2StreamFrame msg) throws Exception {
        if (bodyBuffer == null) {
            bodyBuffer = ctx.alloc().buffer();
        }
        try {
            if (msg instanceof Http2HeadersFrame) {
                Http2HeadersFrame headersFrame = (Http2HeadersFrame) msg;
                this.http2Headers = headersFrame.headers();
                headersEndStream = headersFrame.isEndStream();
                if (headersEndStream) {
                    handleRequest(ctx);
                }
            } else if (msg instanceof Http2DataFrame) {
                Http2DataFrame dataFrame = (Http2DataFrame) msg;
                bodyBuffer.writeBytes(dataFrame.content());
                if (dataFrame.isEndStream()) {
                    handleRequest(ctx);
                }
            }
        } catch (Exception e) {
            if (bodyBuffer != null) {
                bodyBuffer.release();
                bodyBuffer = null;
            }
            throw e;
        }
    }

    private void handleRequest(ChannelHandlerContext ctx) {
        try {
            if (http2Headers == null || http2Headers.method() == null || http2Headers.path() == null) {
                sendErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST);
                return;
            }
            HttpMethod method = HttpMethod.valueOf(http2Headers.method().toString());
            String path = http2Headers.path().toString();
            String body = bodyBuffer != null ? bodyBuffer.toString(StandardCharsets.UTF_8) : "";
            SimpleHttp2Request request = new SimpleHttp2Request(method, path, http2Headers, body);

            // reuse HttpDispatchHandler logic
            boolean keepAlive = true; // In HTTP/2, connections are persistent by default
            QueryStringDecoder queryStringDecoder = new QueryStringDecoder(request.getPath());
            String requestPath = queryStringDecoder.path();
            HttpInvocation httpInvocation = ControllerManager.getHttpInvocation(requestPath);
            if (httpInvocation == null) {
                sendErrorResponse(ctx, HttpResponseStatus.NOT_FOUND);
                return;
            }
            HttpContext<SimpleHttp2Request> httpContext =
                    new HttpContext<>(request, ctx, keepAlive, HttpContext.HTTP_2_0);
            ObjectNode requestDataNode = OBJECT_MAPPER.createObjectNode();
            requestDataNode.set("param", ParameterParser.convertParamMap(queryStringDecoder.parameters()));
            if (request.getMethod() == HttpMethod.POST
                    && request.getBody() != null
                    && !request.getBody().isEmpty()) {
                // assume body is json
                try {
                    ObjectNode bodyDataNode = (ObjectNode) OBJECT_MAPPER.readTree(request.getBody());
                    requestDataNode.set("body", bodyDataNode);
                } catch (Exception e) {
                    LOGGER.warn("Failed to parse http2 body as json: {}", e.getMessage());
                }
            }
            Object httpController = httpInvocation.getController();
            Method handleMethod = httpInvocation.getMethod();
            Object[] args = ParameterParser.getArgValues(
                    httpInvocation.getParamMetaData(), handleMethod, requestDataNode, httpContext);
            handle(httpController, handleMethod, args, ctx, httpContext);
        } catch (Exception e) {
            LOGGER.error("Exception occurred while processing HTTP2 request: {}", e.getMessage(), e);
            sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR);
        } finally {
            if (bodyBuffer != null) {
                bodyBuffer.release();
                bodyBuffer = null;
            }
            http2Headers = null;
            headersEndStream = false;
        }
    }

    private void handle(
            Object httpController,
            Method handleMethod,
            Object[] args,
            ChannelHandlerContext ctx,
            HttpContext<SimpleHttp2Request> httpContext) {
        HTTP_HANDLER_THREADS.execute(() -> {
            Object result;
            try {
                result = handleMethod.invoke(httpController, args);
                if (!httpContext.isAsync()) {
                    sendResponse(ctx, result);
                }
            } catch (IllegalAccessException e) {
                LOGGER.error("Illegal argument exception: {}", e.getMessage(), e);
                sendErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST);
            } catch (Exception e) {
                LOGGER.error("Exception occurred while processing HTTP2 request: {}", e.getMessage(), e);
                sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR);
            }
        });
    }

    private void sendResponse(ChannelHandlerContext ctx, Object result) throws Exception {
        byte[] body = result != null ? OBJECT_MAPPER.writeValueAsBytes(result) : new byte[0];
        Http2Headers headers = new DefaultHttp2Headers().status(HttpResponseStatus.OK.codeAsText());
        headers.set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");
        headers.set(HttpHeaderNames.CONTENT_LENGTH, String.valueOf(body.length));

        ctx.write(new DefaultHttp2HeadersFrame(headers));
        if (body.length > 0) {
            ByteBuf content = Unpooled.wrappedBuffer(body);
            ctx.write(new DefaultHttp2DataFrame(content, true));
        } else {
            ctx.write(new DefaultHttp2DataFrame(Unpooled.EMPTY_BUFFER, true));
        }
        ctx.flush();
    }

    private void sendErrorResponse(ChannelHandlerContext ctx, HttpResponseStatus status) {
        Http2Headers headers = new DefaultHttp2Headers().status(status.codeAsText());
        ctx.writeAndFlush(new DefaultHttp2HeadersFrame(headers, true));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // This is a common exception when the client (like curl) closes the connection after receiving the response.
        // We can safely ignore it by simply closing the context.
        if (cause instanceof java.io.IOException) {
            LOGGER.trace("Client connection closed: {}", cause.getMessage());
        } else {
            LOGGER.error("Exception caught in Http2HttpHandler: ", cause);
        }
        ctx.close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        try {
            if (bodyBuffer != null) {
                bodyBuffer.release();
                bodyBuffer = null;
            }
        } finally {
            super.channelInactive(ctx);
        }
    }
}
