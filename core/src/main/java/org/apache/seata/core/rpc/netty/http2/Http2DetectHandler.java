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

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http2.Http2HeadersFrame;
import org.apache.seata.core.rpc.netty.grpc.GrpcDecoder;
import org.apache.seata.core.rpc.netty.grpc.GrpcEncoder;
import org.apache.seata.core.rpc.netty.grpc.GrpcHeaderEnum;

public class Http2DetectHandler extends ChannelDuplexHandler {

    private ChannelHandler[] serverHandlers;

    public Http2DetectHandler(ChannelHandler[] serverHandlers) {
        this.serverHandlers = serverHandlers;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof Http2HeadersFrame) {
            Http2HeadersFrame headersFrame = (Http2HeadersFrame) msg;
            CharSequence contentType = headersFrame.headers().get(GrpcHeaderEnum.GRPC_CONTENT_TYPE.header);
            if ("application/grpc".equals(contentType.toString())) {
                ctx.pipeline().addLast(new GrpcDecoder());
                ctx.pipeline().addLast(new GrpcEncoder());
                ctx.pipeline().addLast(serverHandlers);
            } else {
                ctx.pipeline().addLast(new Http2DispatchHandler());
            }

            ctx.pipeline().remove(this);
        }
    }
}
