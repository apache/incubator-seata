/*
 *  Copyright 1999-2019 Seata.io Group.
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
package io.seata.core.rpc.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.collection.LongObjectHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Rpc client handler.
 *
 * @author jimin.jm @alibaba-inc.com
 * @date 2018 /9/12
 */
public class RpcClientHandler extends ChannelDuplexHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(RpcClientHandler.class);

    private final LongObjectHashMap compressTable = new LongObjectHashMap(8192, 0.5f);

    /**
     * Instantiates a new Rpc client handler.
     */
    public RpcClientHandler() {

    }

    @Override
    @SuppressWarnings("unchecked")
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
        final Channel channel = ctx.channel();

        final String request = (String)msg;
        try {
            ctx.writeAndFlush(request, ctx.voidPromise());
            LOGGER.info("client:" + msg);

        } catch (Exception e) {
            LOGGER.error("when try flush error", e);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        LOGGER.info("channel active for ClientProxyHandler at :[{}]", ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        LOGGER.info("channel inactive for ClientProxyHandler at :[{}]", ctx.channel());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
        LOGGER.info("channel error for ClientProxyHandler at :[{}]", ctx.channel());
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        LOGGER.info("channel write for ClientProxyHandler at :[{}]", msg);
        ctx.write(msg, promise);
    }

    @Override
    public void flush(ChannelHandlerContext ctx) throws Exception {
        LOGGER.info("channel flush for ClientProxyHandler at :[{}]", ctx.channel());
        ctx.flush();
    }
}
