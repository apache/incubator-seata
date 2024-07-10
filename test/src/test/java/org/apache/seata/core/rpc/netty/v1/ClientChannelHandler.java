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
package org.apache.seata.core.rpc.netty.v1;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.concurrent.DefaultPromise;
import org.apache.seata.core.protocol.RpcMessage;
import org.apache.seata.core.rpc.netty.ProtocolRpcMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class ClientChannelHandler extends ChannelInboundHandlerAdapter {

    /**
     * Logger for ClientChannelHandler
     **/
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientChannelHandler.class);

    private ProtocolV1Client client;

    public ClientChannelHandler(ProtocolV1Client client) {
        this.client = client;
    }

    public void channelActive(final ChannelHandlerContext ctx) throws Exception {
        LOGGER.info("Channel active: {}", ctx.channel());
    }

    @Override
    public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
        LOGGER.info("Channel inactive: {}", ctx.channel());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof RpcMessage) {
            RpcMessage rpcMessage = (RpcMessage)msg;
            int msgId = rpcMessage.getId();
            DefaultPromise future = (DefaultPromise) client.futureMap.remove(msgId);
            if (future != null) {
                future.setSuccess(msg);
            } else {
                LOGGER.warn("miss msg id:{}", msgId);
            }
        }else {
            LOGGER.warn("msg is not ProtocolRpcMessage");
        }

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, final Throwable cause) throws Exception {
        LOGGER.warn("", cause);
    }

}
