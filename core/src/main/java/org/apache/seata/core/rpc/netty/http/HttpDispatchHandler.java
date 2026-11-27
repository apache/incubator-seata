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
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import org.apache.seata.common.rpc.http.HttpContext;
import org.apache.seata.core.exception.HttpRequestFilterException;
import org.apache.seata.core.rpc.netty.http.RequestParseUtils.BodyParseResult;
import org.apache.seata.core.rpc.netty.http.RequestParseUtils.QueryParseResult;
import org.apache.seata.core.rpc.netty.http.filter.HttpFilterContext;
import org.apache.seata.core.rpc.netty.http.filter.HttpRequestFilterChain;
import org.apache.seata.core.rpc.netty.http.filter.HttpRequestFilterManager;
import org.apache.seata.core.rpc.netty.http.filter.HttpRequestParamWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/**
 * A Netty HTTP request handler that dispatches incoming requests to corresponding controller methods
 */
public class HttpDispatchHandler extends BaseHttpChannelHandler<HttpRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpDispatchHandler.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpRequest httpRequest) {
        FullHttpRequest fullHttpRequest = httpRequest instanceof FullHttpRequest ? (FullHttpRequest) httpRequest : null;
        QueryParseResult queryParseResult = RequestParseUtils.parseQuery(httpRequest.uri());
        String path = queryParseResult.getPath();
        Map<String, List<String>> queryParams = queryParseResult.getParameters();
        Map<String, List<String>> headerParams = RequestParseUtils.copyHeaders(httpRequest.headers());
        BodyParseResult bodyParseResult = fullHttpRequest != null
                ? RequestParseUtils.parseBody(OBJECT_MAPPER, fullHttpRequest)
                : RequestParseUtils.BodyParseResult.empty();

        HttpFilterContext<HttpRequest> context = new HttpFilterContext<>(
                httpRequest,
                ctx,
                HttpUtil.isKeepAlive(httpRequest)
                        && httpRequest.protocolVersion().isKeepAliveDefault(),
                HttpContext.HTTP_1_1,
                () -> new HttpRequestParamWrapper(
                        queryParams, bodyParseResult.getFormParams(), headerParams, bodyParseResult.getJsonParams()));

        HttpInvocation httpInvocation = ControllerManager.getHttpInvocation(path);

        if (httpInvocation == null) {
            FullHttpResponse errorResponse = addErrorResponse(context, HttpResponseStatus.NOT_FOUND);
            sendErrorResponse(ctx, errorResponse, false);
            return;
        }

        context.setAttribute("httpInvocation", httpInvocation);
        context.setAttribute("httpController", httpInvocation.getController());
        context.setAttribute("handleMethod", httpInvocation.getMethod());

        ObjectNode requestDataNode = OBJECT_MAPPER.createObjectNode();
        requestDataNode.set("param", ParameterParser.convertParamMap(queryParams));
        if (httpRequest.method() == HttpMethod.POST && bodyParseResult.getBodyNode() != null) {
            requestDataNode.set("body", bodyParseResult.getBodyNode());
        }

        Object[] args;
        try {
            args = ParameterParser.getArgValues(
                    httpInvocation.getParamMetaData(), httpInvocation.getMethod(), requestDataNode, context);
        } catch (Exception e) {
            LOGGER.error("Error parsing request arguments: {}", e.getMessage(), e);
            FullHttpResponse errorResponse = addErrorResponse(context, HttpResponseStatus.BAD_REQUEST);
            sendErrorResponse(ctx, errorResponse, false);
            return;
        }
        context.setAttribute("args", args);

        // Execute filter chain in HTTP thread pool
        HttpRequestFilterChain filterChain = HttpRequestFilterManager.getFilterChain(this::executeFinalAction);
        HTTP_HANDLER_THREADS.execute(() -> {
            HttpFilterContext.setCurrentContext(context);
            try {
                filterChain.doFilter(context);
            } catch (HttpRequestFilterException e) {
                LOGGER.warn("Request blocked by filter: {}", e.getMessage());
                FullHttpResponse errorResponse = addErrorResponse(context, HttpResponseStatus.BAD_REQUEST);
                sendErrorResponse(ctx, errorResponse, false);
            } catch (Exception e) {
                LOGGER.error("Unexpected error during request processing: {}", e.getMessage(), e);
                FullHttpResponse errorResponse = addErrorResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR);
                sendErrorResponse(ctx, errorResponse, false);
            } finally {
                HttpFilterContext.clearCurrentContext();
            }
        });
    }

    private void executeFinalAction(HttpFilterContext<?> context) {
        HttpInvocation httpInvocation = context.getAttribute("httpInvocation");
        Object httpController = context.getAttribute("httpController");
        Method handleMethod = context.getAttribute("handleMethod");
        Object[] args = context.getAttribute("args");

        try {
            Object result = handleMethod.invoke(httpController, args);
            if (context.isAsync()) {
                return;
            }

            sendResponse(context.getContext(), context.isKeepAlive(), result, context);
        } catch (IllegalArgumentException e) {
            LOGGER.error("Illegal argument exception: {}", e.getMessage(), e);
            FullHttpResponse errorResponse = addErrorResponse(context, HttpResponseStatus.BAD_REQUEST);
            sendErrorResponse(context.getContext(), errorResponse, false);
        } catch (Exception e) {
            LOGGER.error("Exception occurred while processing HTTP request: {}", e.getMessage(), e);
            FullHttpResponse errorResponse = addErrorResponse(context, HttpResponseStatus.INTERNAL_SERVER_ERROR);
            sendErrorResponse(context.getContext(), errorResponse, false);
        }
    }

    private void sendResponse(ChannelHandlerContext ctx, boolean keepAlive, Object result, HttpFilterContext<?> context)
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
        context.setResponse(response);
        if (!keepAlive) {
            ctx.writeAndFlush(response).addListeners(ChannelFutureListener.CLOSE);
        } else {
            ctx.writeAndFlush(response);
        }
    }

    private FullHttpResponse addErrorResponse(HttpFilterContext<?> context, HttpResponseStatus status) {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, status, Unpooled.wrappedBuffer(Unpooled.EMPTY_BUFFER));
        context.setResponse(response);
        return response;
    }

    private void sendErrorResponse(ChannelHandlerContext ctx, FullHttpResponse response, boolean keepAlive) {
        if (!keepAlive) {
            ctx.writeAndFlush(response).addListeners(ChannelFutureListener.CLOSE);
        } else {
            ctx.writeAndFlush(response);
        }
    }
}
