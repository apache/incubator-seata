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
package org.apache.seata.core.rpc.processor.server;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.netty.channel.ChannelHandlerContext;
import org.apache.seata.common.util.NetUtil;
import org.apache.seata.core.protocol.AbstractResultMessage;
import org.apache.seata.core.protocol.MessageFuture;
import org.apache.seata.core.protocol.RpcMessage;
import org.apache.seata.core.protocol.transaction.BranchCommitResponse;
import org.apache.seata.core.protocol.transaction.BranchRollbackResponse;
import org.apache.seata.core.rpc.RpcContext;
import org.apache.seata.core.rpc.TransactionMessageHandler;
import org.apache.seata.core.rpc.netty.ChannelManager;
import org.apache.seata.core.rpc.processor.RemotingProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * handle RM/TM response message.
 * <p>
 * process message type:
 * RM:
 * 1) {@link BranchCommitResponse}
 * 2) {@link BranchRollbackResponse}
 *
 * @since 1.3.0
 */
public class ServerOnResponseProcessor implements RemotingProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerOnRequestProcessor.class);

    /**
     * To handle the received RPC message on upper level.
     */
    private TransactionMessageHandler transactionMessageHandler;

    /**
     * The Futures from org.apache.seata.core.rpc.netty.AbstractNettyRemoting#futures
     */
    private ConcurrentMap<Integer, MessageFuture> futures;

    public ServerOnResponseProcessor(TransactionMessageHandler transactionMessageHandler,
                                     ConcurrentHashMap<Integer, MessageFuture> futures) {
        this.transactionMessageHandler = transactionMessageHandler;
        this.futures = futures;
    }

    @Override
    public void process(ChannelHandlerContext ctx, RpcMessage rpcMessage) throws Exception {
        MessageFuture messageFuture = futures.remove(rpcMessage.getId());
        String receiveMsgLog = String.format("receive msg[single]: %s, clientIp: %s, vgroup: %s", rpcMessage.getBody(), NetUtil.toIpAddress(ctx.channel().remoteAddress()),
            ChannelManager.getContextFromIdentified(ctx.channel()).getTransactionServiceGroup());
        if (LOGGER.isInfoEnabled()) {
            BatchLogHandler.INSTANCE.writeLog(receiveMsgLog);
        }
        if (messageFuture != null) {
            messageFuture.setResultMessage(rpcMessage.getBody());
        } else {
            if (ChannelManager.isRegistered(ctx.channel())) {
                onResponseMessage(ctx, rpcMessage);
            } else {
                try {
                    ctx.disconnect();
                    ctx.close();
                } catch (Exception exx) {
                    LOGGER.error(exx.getMessage());
                }
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info(String.format("close a unhandled connection! [%s]", ctx.channel().toString()));
                }
            }
        }
    }

    private void onResponseMessage(ChannelHandlerContext ctx, RpcMessage rpcMessage) {
        if (rpcMessage.getBody() instanceof AbstractResultMessage) {
            RpcContext rpcContext = ChannelManager.getContextFromIdentified(ctx.channel());
            transactionMessageHandler.onResponse((AbstractResultMessage) rpcMessage.getBody(), rpcContext);
        }
    }
}
