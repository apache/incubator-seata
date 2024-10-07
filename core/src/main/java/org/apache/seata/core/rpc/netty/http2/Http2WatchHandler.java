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

import com.alibaba.fastjson.JSONObject;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2HeadersFrame;
import org.apache.seata.core.rpc.netty.grpc.GrpcHeaderEnum;
import org.apache.seata.core.rpc.netty.http.ControllerManager;
import org.apache.seata.core.rpc.netty.http.SeataHttpServletRequest;

import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class Http2WatchHandler extends ChannelDuplexHandler {

    public static final String WATCH_PATH = "/metadata/v1/watch";

    private final AtomicBoolean headerSent = new AtomicBoolean(false);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws InvocationTargetException, IllegalAccessException {
        if (msg instanceof Http2HeadersFrame) {
            if (headerSent.compareAndSet(false, true)) {
                Http2Headers headers = new DefaultHttp2Headers();
                headers.add(GrpcHeaderEnum.HTTP2_STATUS.header, String.valueOf(200));
                ctx.writeAndFlush(new DefaultHttp2HeadersFrame(headers, false));
            }

            Http2HeadersFrame http2HeadersFrame = (Http2HeadersFrame) msg;
            CharSequence groupTermMapString = http2HeadersFrame.headers().get("seata-group-term-map");
            CharSequence watchTimeout = http2HeadersFrame.headers().get("seata-watch-timeout");
            Map<String, Object>  groupTermMap = JSONObject.parseObject(groupTermMapString.toString(), Map.class);
            InetSocketAddress inetSocketAddress = (InetSocketAddress) ctx.channel().remoteAddress();

            SeataHttpServletRequest seataHttpServletRequest = new SeataHttpServletRequest(inetSocketAddress.getAddress().getHostAddress());
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

            Object httpController = ControllerManager.getHttpController(WATCH_PATH);
            Method handleMethod = ControllerManager.getHandleMethod(WATCH_PATH);
            handleMethod.invoke(httpController, seataHttpServletRequest, groupTermMap, Integer.valueOf(watchTimeout.toString()));
        }
    }
}
