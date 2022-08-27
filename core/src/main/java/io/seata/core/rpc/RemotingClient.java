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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;

import io.seata.core.rpc.netty.RmNettyRemotingClient;
import io.seata.core.rpc.netty.TmNettyRemotingClient;
import io.seata.core.rpc.processor.RemotingProcessor;

/**
 * The interface remoting client.
 *
 * @author zhaojun
 * @author zhangchenghui.dev@gmail.com
 * @since 1.3.0
 */
public interface RemotingClient {

    /**
     * client send sync request.
     * In this request, if
     * {@link RmNettyRemotingClient#isEnableClientBatchSendRequest()}
     * {@link TmNettyRemotingClient#isEnableClientBatchSendRequest()}
     * is enabled, the message will be sent in batches.
     *
     * @param msg transaction message {@link io.seata.core.protocol}
     * @return server result message
     * @throws TimeoutException TimeoutException
     */
    Object sendSyncRequest(Object msg) throws TimeoutException;

    /**
     * register processor
     *
     * @param messageType {@link io.seata.core.protocol.MessageType}
     * @param processor   {@link RemotingProcessor}
     * @param executor    thread pool
     */
    void registerProcessor(final int messageType, final RemotingProcessor processor, final ExecutorService executor);
}
