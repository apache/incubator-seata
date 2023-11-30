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
package io.seata.mockserver.processor;

import io.netty.channel.ChannelHandlerContext;
import io.seata.core.protocol.MessageFuture;
import io.seata.core.protocol.RpcMessage;
import io.seata.core.rpc.RemotingServer;
import io.seata.core.rpc.TransactionMessageHandler;
import io.seata.core.rpc.netty.ChannelManager;
import io.seata.core.rpc.processor.RemotingProcessor;

import java.util.concurrent.ConcurrentMap;

/**
 * Mock Remoting Processor
 *
 * @author minghua.xie
 * @date 2023/11/14
 **/
public class MockOnRespProcessor extends MockRemotingProcessor {

    private ConcurrentMap<Integer, MessageFuture> futures;


    public MockOnRespProcessor(RemotingServer remotingServer, TransactionMessageHandler handler
            , ConcurrentMap<Integer, MessageFuture> futures) {
        super(remotingServer, handler);
        this.futures = futures;
    }

    @Override
    public void process(ChannelHandlerContext ctx, RpcMessage rpcMessage) throws Exception {
        super.process(ctx, rpcMessage);
        MessageFuture messageFuture = futures.remove(rpcMessage.getId());
        if (messageFuture != null) {
            messageFuture.setResultMessage(rpcMessage.getBody());
        }
    }


}
