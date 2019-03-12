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
package com.alibaba.fescar.core.rpc;

import com.alibaba.fescar.core.protocol.RegisterRMRequest;
import com.alibaba.fescar.core.protocol.RegisterTMRequest;
import com.alibaba.fescar.core.rpc.netty.RegisterCheckAuthHandler;

import io.netty.channel.ChannelHandlerContext;

/**
 * The interface Server message listener.
 *
 * @author jimin.jm @alibaba-inc.com
 * @date 2018 /10/15
 */
public interface ServerMessageListener {

    /**
     * On trx message.
     *
     * @param msgId   the msg id
     * @param ctx     the ctx
     * @param message the message
     * @param sender  the sender
     */
    void onTrxMessage(long msgId, ChannelHandlerContext ctx, Object message, ServerMessageSender sender);

    /**
     * On reg rm message.
     *
     * @param msgId            the msg id
     * @param ctx              the ctx
     * @param message          the message
     * @param sender           the sender
     * @param checkAuthHandler the check auth handler
     */
    void onRegRmMessage(long msgId, ChannelHandlerContext ctx, RegisterRMRequest message,
                        ServerMessageSender sender, RegisterCheckAuthHandler checkAuthHandler);

    /**
     * On reg tm message.
     *
     * @param msgId            the msg id
     * @param ctx              the ctx
     * @param message          the message
     * @param sender           the sender
     * @param checkAuthHandler the check auth handler
     */
    void onRegTmMessage(long msgId, ChannelHandlerContext ctx, RegisterTMRequest message,
                        ServerMessageSender sender, RegisterCheckAuthHandler checkAuthHandler);

    /**
     * On check message.
     *
     * @param msgId  the msg id
     * @param ctx    the ctx
     * @param sender the sender
     */
    void onCheckMessage(long msgId, ChannelHandlerContext ctx, ServerMessageSender sender);

}
