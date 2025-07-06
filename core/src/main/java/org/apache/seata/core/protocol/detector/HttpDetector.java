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
package org.apache.seata.core.protocol.detector;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerUpgradeHandler;
import io.netty.handler.codec.http2.Http2CodecUtil;
import io.netty.handler.codec.http2.Http2FrameCodecBuilder;
import io.netty.handler.codec.http2.Http2MultiplexHandler;
import io.netty.handler.codec.http2.Http2ServerUpgradeCodec;
import io.netty.handler.codec.http2.Http2StreamChannel;
import io.netty.util.AsciiString;
import org.apache.seata.core.rpc.netty.http.Http2HttpHandler;
import org.apache.seata.core.rpc.netty.http.HttpDispatchHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpDetector implements ProtocolDetector {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpDetector.class);
    private static final String[] HTTP_METHODS = {"GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS", "PATCH"};

    @Override
    public boolean detect(ByteBuf in) {
        if (in.readableBytes() < 8) {
            return false;
        }

        for (String method : HTTP_METHODS) {
            if (startsWith(in, method)) {
                return true;
            }
        }

        return false;
    }

    private boolean startsWith(ByteBuf buffer, String prefix) {
        for (int i = 0; i < prefix.length(); i++) {
            if (buffer.getByte(i) != (byte) prefix.charAt(i)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public ChannelHandler[] getHandlers() {
        HttpServerCodec sourceCodec = new HttpServerCodec();
        HttpServerUpgradeHandler upgradeHandler = getHttpServerUpgradeHandler(sourceCodec);

        ChannelInboundHandlerAdapter upgradeCleanupHandler = new ChannelInboundHandlerAdapter() {
            @Override
            public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
                if (evt instanceof HttpServerUpgradeHandler.UpgradeEvent) {
                    ChannelPipeline p = ctx.pipeline();
                    p.remove(HttpObjectAggregator.class);
                    p.remove(HttpDispatchHandler.class);
                }
                super.userEventTriggered(ctx, evt);
            }
        };

        ChannelInboundHandlerAdapter finalExceptionHandler = new ChannelInboundHandlerAdapter() {
            @Override
            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                if (cause instanceof java.io.IOException) {
                    LOGGER.trace("Connection closed by client: {}", cause.getMessage());
                } else {
                    LOGGER.error("Exception caught in HTTP pipeline: ", cause);
                }
                ctx.close();
            }
        };

        return new ChannelHandler[] {
            sourceCodec,
            upgradeHandler,
            upgradeCleanupHandler,
            new HttpObjectAggregator(1048576),
            new HttpDispatchHandler(),
            finalExceptionHandler
        };
    }

    private static HttpServerUpgradeHandler getHttpServerUpgradeHandler(HttpServerCodec sourceCodec) {
        HttpServerUpgradeHandler.UpgradeCodecFactory upgradeCodecFactory = protocol -> {
            if (AsciiString.contentEquals(Http2CodecUtil.HTTP_UPGRADE_PROTOCOL_NAME, protocol)) {
                return new Http2ServerUpgradeCodec(
                        Http2FrameCodecBuilder.forServer().build(),
                        new Http2MultiplexHandler(new ChannelInitializer<Http2StreamChannel>() {
                            @Override
                            protected void initChannel(Http2StreamChannel ch) {
                                ch.pipeline().addLast(new Http2HttpHandler());
                            }
                        }));
            } else {
                return null;
            }
        };

        return new HttpServerUpgradeHandler(sourceCodec, upgradeCodecFactory, 1048576);
    }
}
