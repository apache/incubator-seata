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
package io.seata.core.rpc.netty.v1;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.seata.core.protocol.RpcMessage;
import io.seata.core.rpc.netty.ProtocolRpcMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
@ChannelHandler.Sharable
public class ServerChannelHandler extends ChannelInboundHandlerAdapter {

    /**
     * Logger for ServerChannelHandler
     **/
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerChannelHandler.class);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        Channel channel = ctx.channel();

        RpcMessage rpcMessage = null;
        if (msg instanceof ProtocolRpcMessage) {
            rpcMessage = ((ProtocolRpcMessage) msg).protocolMsg2RpcMsg();
        } else {
            LOGGER.error("rpcMessage type error");
            return;
        }
        channel.writeAndFlush(rpcMessage);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        LOGGER.info(ctx.channel().remoteAddress() + " connected!");
        ctx.fireChannelActive();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        LOGGER.info(ctx.channel().remoteAddress() + " disconnected!");
        ctx.fireChannelInactive();
    }
}
