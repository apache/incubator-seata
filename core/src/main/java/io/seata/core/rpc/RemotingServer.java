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
import io.seata.core.rpc.netty.processor.NettyProcessor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;

/**
 * The interface Remoting server.
 *
 * @author slievrly
 * @author zhangchenghui.dev@gmail.com
 */
public interface RemotingServer {

    Object sendSyncRequest(String resourceId, String clientId, Object msg) throws TimeoutException;

    Object sendSyncRequest(Channel channel, Object msg) throws TimeoutException;

    void sendAsyncRequest(Channel channel, Object msg);

    void sendAsyncResponse(RpcMessage rpcMessage, Channel channel, Object msg);

    /**
     * register processor
     *
     * @param messageType {@link io.seata.core.protocol.MessageType}
     * @param processor   {@link NettyProcessor}
     * @param executor    thread pool
     */
    void registerProcessor(final int messageType, final NettyProcessor processor, final ExecutorService executor);

}
