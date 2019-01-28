/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.alibaba.fescar.core.rpc.netty;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.collection.LongObjectHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Rpc server handler.
 *
 * @Author: jimin.jm @alibaba-inc.com
 * @Project: fescar -all
 * @DateTime: 2018 /9/12 16:51
 * @FileName: RpcServerHandler
 * @Description:
 */
@Sharable
public class RpcServerHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(RpcServerHandler.class);

    private final LongObjectHashMap compressTable = new LongObjectHashMap(16, 0.5f);

    /**
     * Instantiates a new Rpc server handler.
     */
    public RpcServerHandler() {
        LOGGER.info("init server handler");

    }

    /*
    @Override
    @SuppressWarnings("unchecked")
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
        LOGGER.info("server:" + msg.toString());
        final Channel channel = ctx.channel();
        final String request = (String)msg;
        try {
            ctx.writeAndFlush(request, ctx.voidPromise());
            super.channelRead(ctx, msg);
            LOGGER.info("server:" + msg);

        } catch (Exception e) {
            LOGGER.error("when try flush error", e);
        }
    }
    */

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        LOGGER.info("Server channel read:");
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        ctx.fireUserEventTriggered(evt);
        if (evt instanceof IdleStateEvent) {

            IdleStateEvent event = (IdleStateEvent)evt;

            if (event.state().equals(IdleState.READER_IDLE)) {

                LOGGER.error("READER_IDLE");
                ctx.close();

            } else if (event.state().equals(IdleState.WRITER_IDLE)) {

            } else if (event.state().equals(IdleState.ALL_IDLE)) {

                LOGGER.error("ALL_IDLE");

            }

        }
    }

    //@Override
    //protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
    //    LOGGER.info("server channel read0:" + msg.toString());
    //    final Channel channel = ctx.channel();
    //    try {
    //        ctx.writeAndFlush(msg, ctx.voidPromise());
    //        super.channelRead(ctx, msg);
    //        LOGGER.info("server:" + msg);
    //
    //    } catch (Exception e) {
    //        LOGGER.error("when try flush error", e);
    //    }
    //}

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        LOGGER.info("channel active for ServerProxyHandler at :[{}]", ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        LOGGER.info("channel inactive for ServerProxyHandler at :[{}],[{}]", ctx.channel(),ctx.channel().remoteAddress());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
        LOGGER.info("channel error:" + cause.getMessage());
    }
}
