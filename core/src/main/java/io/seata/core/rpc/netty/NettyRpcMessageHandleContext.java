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

import io.netty.channel.ChannelHandlerContext;
import io.seata.core.protocol.RpcMessage;
import io.seata.core.rpc.processor.RpcMessageHandleContext;

/**
 * @author goodboycoder
 */
public class NettyRpcMessageHandleContext extends RpcMessageHandleContext {
    private final ChannelHandlerContext ctx;

    public NettyRpcMessageHandleContext(ChannelHandlerContext ctx, RpcMessage rpcMessage) {
        super(rpcMessage, new NettySeataChannel(ctx.channel()));
        this.ctx = ctx;
    }

    @Override
    public void close() {
        this.ctx.close();
    }

    @Override
    public void disconnect() {
        this.ctx.disconnect();
    }
}
