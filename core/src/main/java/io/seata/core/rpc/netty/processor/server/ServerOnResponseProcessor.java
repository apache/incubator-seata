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
package io.seata.core.rpc.netty.processor.server;

import io.netty.channel.ChannelHandlerContext;
import io.seata.core.protocol.AbstractResultMessage;
import io.seata.core.protocol.MergeResultMessage;
import io.seata.core.protocol.MessageFuture;
import io.seata.core.protocol.RpcMessage;
import io.seata.core.protocol.transaction.BranchCommitResponse;
import io.seata.core.protocol.transaction.BranchRollbackResponse;
import io.seata.core.rpc.netty.ChannelManager;
import io.seata.core.rpc.RpcContext;
import io.seata.core.rpc.TransactionMessageHandler;
import io.seata.core.rpc.netty.processor.NettyProcessor;

import java.util.concurrent.ConcurrentHashMap;

/**
 * handle RM/TM response message.
 * <p>
 * message type:
 * RM:
 * 1) {@link BranchCommitResponse}
 * 2) {@link BranchRollbackResponse}
 *
 * @author zhangchenghui.dev@gmail.com
 * @since 1.2.0
 */
public class ServerOnResponseProcessor implements NettyProcessor {

    private TransactionMessageHandler transactionMessageHandler;

    private ConcurrentHashMap<Integer, MessageFuture> futures;

    public ServerOnResponseProcessor(TransactionMessageHandler transactionMessageHandler,
                                     ConcurrentHashMap<Integer, MessageFuture> futures) {
        this.transactionMessageHandler = transactionMessageHandler;
        this.futures = futures;
    }

    @Override
    public void process(ChannelHandlerContext ctx, RpcMessage rpcMessage) throws Exception {
        MessageFuture messageFuture = null;
        if (!(rpcMessage.getBody() instanceof MergeResultMessage)) {
            messageFuture = futures.remove(rpcMessage.getId());
        }
        if (messageFuture != null) {
            messageFuture.setResultMessage(rpcMessage.getBody());
        } else {
            if (rpcMessage.getBody() instanceof AbstractResultMessage) {
                RpcContext rpcContext = ChannelManager.getContextFromIdentified(ctx.channel());
                transactionMessageHandler.onResponse((AbstractResultMessage) rpcMessage.getBody(), rpcContext);
            }
        }
    }
}
