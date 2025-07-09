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
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http2.Http2FrameCodecBuilder;
import io.netty.handler.codec.http2.Http2HeadersFrame;
import io.netty.handler.codec.http2.Http2MultiplexHandler;
import io.netty.handler.codec.http2.Http2StreamChannel;
import io.netty.util.CharsetUtil;
import org.apache.seata.core.rpc.netty.grpc.GrpcDecoder;
import org.apache.seata.core.rpc.netty.grpc.GrpcEncoder;
import org.apache.seata.core.rpc.netty.http.Http2HttpHandler;

public class Http2Detector implements ProtocolDetector {
    private static final byte[] HTTP2_PREFIX_BYTES = "PRI * HTTP/2.0\r\n\r\nSM\r\n\r\n".getBytes(CharsetUtil.UTF_8);
    private final ChannelHandler[] serverHandlers;

    public Http2Detector(ChannelHandler[] serverHandlers) {
        this.serverHandlers = serverHandlers;
    }

    @Override
    public boolean detect(ByteBuf in) {
        if (in.readableBytes() < HTTP2_PREFIX_BYTES.length) {
            return false;
        }
        for (int i = 0; i < HTTP2_PREFIX_BYTES.length; i++) {
            if (in.getByte(i) != HTTP2_PREFIX_BYTES[i]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ChannelHandler[] getHandlers() {
        return new ChannelHandler[] {
            Http2FrameCodecBuilder.forServer().build(),
            new Http2MultiplexHandler(new ChannelInitializer<Http2StreamChannel>() {
                @Override
                protected void initChannel(Http2StreamChannel ch) {
                    ch.pipeline().addLast(new Http2SelectorHandler());
                }
            })
        };
    }

    private class Http2SelectorHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            if (msg instanceof Http2HeadersFrame) {
                Http2HeadersFrame headersFrame = (Http2HeadersFrame) msg;
                CharSequence contentType = headersFrame.headers().get(HttpHeaderNames.CONTENT_TYPE);
                final ChannelPipeline p = ctx.pipeline();
                if (contentType != null && contentType.toString().endsWith("grpc")) {
                    p.addLast(new GrpcDecoder());
                    p.addLast(new GrpcEncoder());
                    p.addLast(serverHandlers);
                } else {
                    p.addLast(new Http2HttpHandler());
                }
                p.remove(this);
            }
            ctx.fireChannelRead(msg);
        }
    }
}
