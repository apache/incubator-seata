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

import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2HeadersFrame;
import org.apache.seata.core.rpc.netty.grpc.GrpcHeaderEnum;
import org.apache.seata.core.rpc.netty.http.ControllerManager;
import org.apache.seata.core.rpc.netty.http.ParameterParser;
import org.apache.seata.core.rpc.netty.http.SeataHttpServletRequest;

public class Http2DispatchHandler extends ChannelDuplexHandler {

    private final AtomicBoolean headerSent = new AtomicBoolean(false);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof Http2HeadersFrame) {
            if (headerSent.compareAndSet(false, true)) {
                Http2Headers headers = new DefaultHttp2Headers();
                headers.add(GrpcHeaderEnum.HTTP2_STATUS.header, String.valueOf(200));
                ctx.writeAndFlush(new DefaultHttp2HeadersFrame(headers, false));
            }

            Http2HeadersFrame http2HeadersFrame = (Http2HeadersFrame) msg;
            String path = http2HeadersFrame.headers().path().toString();
            QueryStringDecoder queryStringDecoder = new QueryStringDecoder(path);
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode paramMap = objectMapper.valueToTree(ParameterParser.convertParamMap(queryStringDecoder.parameters()));
            InetSocketAddress inetSocketAddress = (InetSocketAddress) ctx.channel().remoteAddress();
            SeataHttpServletRequest seataHttpServletRequest = new SeataHttpServletRequest(inetSocketAddress.getAddress().getHostAddress());
            Object httpController = ControllerManager.getHttpController(path);
            Method handleMethod = ControllerManager.getHandleMethod(path);
            Object[] args = ParameterParser.getArgValues(seataHttpServletRequest, handleMethod, paramMap);
            Object result = handleMethod.invoke(httpController, args);
            if (seataHttpServletRequest.isAsyncStarted()) {
                seataHttpServletRequest.getAsyncContext().addListener(new AsyncListener() {
                    @Override
                    public void onComplete(AsyncEvent event) throws IOException {
                        ctx.channel().writeAndFlush(new DefaultHttp2DataFrame(Unpooled.wrappedBuffer("changed\n".getBytes())));
                    }

                    @Override
                    public void onTimeout(AsyncEvent event) throws IOException {

                    }

                    @Override
                    public void onError(AsyncEvent event) throws IOException {

                    }

                    @Override
                    public void onStartAsync(AsyncEvent event) throws IOException {

                    }
                });

                return;
            }

            ctx.writeAndFlush(new DefaultHttp2DataFrame(Unpooled.wrappedBuffer(objectMapper.writeValueAsBytes(result))));
        }
    }
}
