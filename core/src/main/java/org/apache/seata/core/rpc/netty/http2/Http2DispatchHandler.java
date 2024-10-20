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
package org.apache.seata.core.rpc.netty.http2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2HeadersFrame;
import org.apache.seata.core.rpc.netty.grpc.GrpcHeaderEnum;
import org.apache.seata.core.rpc.netty.http.ControllerManager;
import org.apache.seata.core.rpc.netty.http.HttpInvocation;
import org.apache.seata.core.rpc.netty.http.ParameterParser;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

public class Http2DispatchHandler extends ChannelDuplexHandler {

    private final AtomicBoolean headerSent = new AtomicBoolean(false);

    private ObjectNode requestDataNode;

    private String path;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof Http2HeadersFrame) {
            if (headerSent.compareAndSet(false, true)) {
                Http2Headers headers = new DefaultHttp2Headers();
                headers.add(GrpcHeaderEnum.HTTP2_STATUS.header, String.valueOf(200));
                ctx.writeAndFlush(new DefaultHttp2HeadersFrame(headers, false));
            }

            Http2HeadersFrame http2HeadersFrame = (Http2HeadersFrame) msg;
            path = http2HeadersFrame.headers().path().toString();
            QueryStringDecoder queryStringDecoder = new QueryStringDecoder(path);
            ObjectMapper objectMapper = new ObjectMapper();
            requestDataNode = objectMapper.createObjectNode();
            requestDataNode.putIfAbsent("param", ParameterParser.convertParamMap(queryStringDecoder.parameters()));
            requestDataNode.putPOJO("channel", ctx.channel());
            if (http2HeadersFrame.isEndStream()) {
                invoke(ctx);
            }
        } else if (msg instanceof Http2DataFrame) {
            ObjectMapper objectMapper = new ObjectMapper();
            Http2DataFrame http2DataFrame = (Http2DataFrame) msg;
            ByteBuf byteBuf = http2DataFrame.content();
            byte[] bytes = new byte[byteBuf.readableBytes()];
            byteBuf.readBytes(bytes);
            requestDataNode.putIfAbsent("body", objectMapper.readTree(bytes));
            invoke(ctx);
        }
    }

    private void invoke(ChannelHandlerContext ctx) throws JsonProcessingException, IllegalAccessException, InvocationTargetException {
        ObjectMapper objectMapper = new ObjectMapper();
        HttpInvocation httpInvocation = ControllerManager.getHttpInvocation(path);
        Object httpController = httpInvocation.getController();
        Method handleMethod = httpInvocation.getMethod();
        handleMethod.setAccessible(true);
        Object[] args = ParameterParser.getArgValues(httpInvocation.getParamMetaData(), handleMethod, requestDataNode);
        Object result = handleMethod.invoke(httpController, args);
        if (requestDataNode.get("channel") == null) {
            return;
        }

        ctx.writeAndFlush(new DefaultHttp2DataFrame(Unpooled.wrappedBuffer(objectMapper.writeValueAsBytes(result)), true));
    }
}
