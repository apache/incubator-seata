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
package org.apache.seata.core.rpc.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler class for Netty channel events.
 * Detects channel activation, deactivation, exceptions, and idle state events
 * and forwards these events to the AbstractNettyRemotingClient.
 */
@Sharable
public class ChannelEventHandler extends ChannelDuplexHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChannelEventHandler.class);
    private final AbstractNettyRemotingClient remotingClient;

    public ChannelEventHandler(AbstractNettyRemotingClient remotingClient) {
        this.remotingClient = remotingClient;
    }

    /**
     * Called when a channel becomes active.
     * Logs the channel activation event and notifies the remoting client.
     *
     * @param ctx the channel handler context
     * @throws Exception if an exception occurs
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Channel active: {}", ctx.channel().remoteAddress());
        }
        remotingClient.onChannelActive(ctx.channel());
        super.channelActive(ctx);
    }

    /**
     * Called when a channel becomes inactive.
     * Logs the channel deactivation event and notifies the remoting client.
     *
     * @param ctx the channel handler context
     * @throws Exception if an exception occurs
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        LOGGER.warn("Channel inactive: {}", channel.remoteAddress());
        remotingClient.onChannelInactive(channel);
        super.channelInactive(ctx);
    }

    /**
     * Called when an exception occurs in a channel.
     * Logs the exception event and notifies the remoting client.
     *
     * @param ctx the channel handler context
     * @param cause the thrown exception
     * @throws Exception if an exception occurs
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        Channel channel = ctx.channel();
        LOGGER.warn("Channel exception: {}, cause: {}", channel.remoteAddress(), cause.getMessage());
        remotingClient.onChannelException(channel, cause);
        super.exceptionCaught(ctx, cause);
    }

    /**
     * Called when a user event is triggered.
     * Primarily handles IdleStateEvent, logs the event and notifies the remoting client.
     *
     * @param ctx the channel handler context
     * @param evt the triggered event
     * @throws Exception if an exception occurs
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Channel idle: {}", ctx.channel().remoteAddress());
            }
            remotingClient.onChannelIdle(ctx.channel());
        }
        super.userEventTriggered(ctx, evt);
    }
}
