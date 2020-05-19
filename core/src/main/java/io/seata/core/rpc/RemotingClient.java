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
package io.seata.core.rpc;

import io.netty.channel.Channel;
import io.seata.core.protocol.AbstractMessage;
import io.seata.core.protocol.RpcMessage;
import io.seata.core.rpc.processor.RemotingProcessor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;

/**
 * The interface remoting client.
 *
 * @author zhaojun
 * @author zhangchenghui.dev@gmail.com
 * @since 1.3.0
 */
public interface RemotingClient {

    /**
     * Send msg with response object.
     *
     * @param msg     the msg
     * @param timeout the timeout
     * @return the object
     * @throws TimeoutException the timeout exception
     */
    Object sendMsgWithResponse(Object msg, long timeout) throws TimeoutException;

    /**
     * Send msg with response object.
     *
     * @param serverAddress the server address
     * @param msg           the msg
     * @param timeout       the timeout
     * @return the object
     * @throws TimeoutException the timeout exception
     */
    Object sendMsgWithResponse(String serverAddress, Object msg, long timeout) throws TimeoutException;

    /**
     * Send msg with response object.
     *
     * @param msg the msg
     * @return the object
     * @throws TimeoutException the timeout exception
     */
    Object sendMsgWithResponse(Object msg) throws TimeoutException;

    /**
     * Send response.
     *
     * @param request       the msg id
     * @param serverAddress the server address
     * @param msg           the msg
     */
    void sendResponse(RpcMessage request, String serverAddress, Object msg);

    /**
     * On register msg success.
     *
     * @param serverAddress  the server address
     * @param channel        the channel
     * @param response       the response
     * @param requestMessage the request message
     */
    void onRegisterMsgSuccess(String serverAddress, Channel channel, Object response, AbstractMessage requestMessage);

    /**
     * On register msg fail.
     *
     * @param serverAddress  the server address
     * @param channel        the channel
     * @param response       the response
     * @param requestMessage the request message
     */
    void onRegisterMsgFail(String serverAddress, Channel channel, Object response, AbstractMessage requestMessage);

    /**
     * register processor
     *
     * @param messageType {@link io.seata.core.protocol.MessageType}
     * @param processor   {@link RemotingProcessor}
     * @param executor    thread pool
     */
    void registerProcessor(final int messageType, final RemotingProcessor processor, final ExecutorService executor);
}
