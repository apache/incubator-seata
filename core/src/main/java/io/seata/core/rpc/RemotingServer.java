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
import io.seata.core.protocol.RpcMessage;
import io.seata.core.rpc.processor.RemotingProcessor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;

/**
 * The interface Remoting server.
 *
 * @author slievrly
 * @author zhangchenghui.dev@gmail.com
 * @since 1.3.0
 */
public interface RemotingServer {

    /**
     * server send sync request.
     *
     * @param resourceId rm client resourceId
     * @param clientId   rm client id
     * @param msg        transaction message {@code io.seata.core.protocol}
     * @param tryOtherApp   try other app
     * @return client result message
     * @throws TimeoutException TimeoutException
     */
    Object sendSyncRequest(String resourceId, String clientId, Object msg, boolean tryOtherApp) throws TimeoutException;

    /**
     * server send sync request.
     *
     * @param channel client channel
     * @param msg     transaction message {@code io.seata.core.protocol}
     * @return client result message
     * @throws TimeoutException TimeoutException
     */
    Object sendSyncRequest(Channel channel, Object msg) throws TimeoutException;

    /**
     * server send async request.
     *
     * @param channel client channel
     * @param msg     transaction message {@code io.seata.core.protocol}
     */
    void sendAsyncRequest(Channel channel, Object msg);

    /**
     * server send async response.
     *
     * @param rpcMessage rpc message from client request
     * @param channel    client channel
     * @param msg        transaction message {@code io.seata.core.protocol}
     */
    void sendAsyncResponse(RpcMessage rpcMessage, Channel channel, Object msg);

    /**
     * register processor
     *
     * @param messageType {@link io.seata.core.protocol.MessageType}
     * @param processor   {@link RemotingProcessor}
     * @param executor    thread pool
     */
    void registerProcessor(final int messageType, final RemotingProcessor processor, final ExecutorService executor);

}
