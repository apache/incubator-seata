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
package org.apache.seata.core.rpc.processor.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.netty.channel.ChannelHandlerContext;
import org.apache.seata.core.protocol.AbstractResultMessage;
import org.apache.seata.core.protocol.BatchResultMessage;
import org.apache.seata.core.protocol.MergeMessage;
import org.apache.seata.core.protocol.MergeResultMessage;
import org.apache.seata.core.protocol.MergedWarpMessage;
import org.apache.seata.core.protocol.MessageFuture;
import org.apache.seata.core.protocol.RegisterRMResponse;
import org.apache.seata.core.protocol.RegisterTMResponse;
import org.apache.seata.core.protocol.RpcMessage;
import org.apache.seata.core.protocol.transaction.BranchRegisterResponse;
import org.apache.seata.core.protocol.transaction.BranchReportResponse;
import org.apache.seata.core.protocol.transaction.GlobalBeginResponse;
import org.apache.seata.core.protocol.transaction.GlobalCommitResponse;
import org.apache.seata.core.protocol.transaction.GlobalLockQueryResponse;
import org.apache.seata.core.protocol.transaction.GlobalReportResponse;
import org.apache.seata.core.protocol.transaction.GlobalRollbackResponse;
import org.apache.seata.core.rpc.TransactionMessageHandler;
import org.apache.seata.core.rpc.processor.RemotingProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * process TC response message.
 * <p>
 * process message type:
 * RM:
 * 1) {@link MergeResultMessage}
 * 2) {@link RegisterRMResponse}
 * 3) {@link BranchRegisterResponse}
 * 4) {@link BranchReportResponse}
 * 5) {@link GlobalLockQueryResponse}
 * TM:
 * 1) {@link MergeResultMessage}
 * 2) {@link RegisterTMResponse}
 * 3) {@link GlobalBeginResponse}
 * 4) {@link GlobalCommitResponse}
 * 5) {@link GlobalReportResponse}
 * 6) {@link GlobalRollbackResponse}
 *
 * @since 1.3.0
 */
public class ClientOnResponseProcessor implements RemotingProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientOnResponseProcessor.class);

    /**
     * The Merge msg map from org.apache.seata.core.rpc.netty.AbstractNettyRemotingClient#mergeMsgMap.
     */
    private final Map<Integer, MergeMessage> mergeMsgMap;

    private final Map<Integer, Integer> childToParentMap;

    /**
     * The Futures from org.apache.seata.core.rpc.netty.AbstractNettyRemoting#futures
     */
    private final ConcurrentMap<Integer, MessageFuture> futures;

    /**
     * To handle the received RPC message on upper level.
     */
    private final TransactionMessageHandler transactionMessageHandler;

    public ClientOnResponseProcessor(Map<Integer, MergeMessage> mergeMsgMap,
                                     ConcurrentHashMap<Integer, MessageFuture> futures, Map<Integer,Integer> childToParentMap,
                                     TransactionMessageHandler transactionMessageHandler) {
        this.mergeMsgMap = mergeMsgMap;
        this.childToParentMap = childToParentMap;
        this.futures = futures;
        this.transactionMessageHandler = transactionMessageHandler;
    }

    @Override
    public void process(ChannelHandlerContext ctx, RpcMessage rpcMessage) throws Exception {
        if (rpcMessage.getBody() instanceof MergeResultMessage) {
            MergeResultMessage results = (MergeResultMessage) rpcMessage.getBody();
            MergedWarpMessage mergeMessage = (MergedWarpMessage) mergeMsgMap.remove(rpcMessage.getId());
            for (int i = 0; i < mergeMessage.msgs.size(); i++) {
                int msgId = mergeMessage.msgIds.get(i);
                MessageFuture future = futures.remove(msgId);
                // The old version of the server will return MergeResultMessage, so it is necessary to remove the msgId from the childToParentMap.
                childToParentMap.remove(msgId);
                if (future == null) {
                    LOGGER.error("msg: {} is not found in futures, result message: {}", msgId,results.getMsgs()[i]);
                } else {
                    future.setResultMessage(results.getMsgs()[i]);
                }
            }
        } else if (rpcMessage.getBody() instanceof BatchResultMessage) {
            BatchResultMessage batchResultMessage = (BatchResultMessage)rpcMessage.getBody();
            for (int i = 0; i < batchResultMessage.getMsgIds().size(); i++) {
                int msgId = batchResultMessage.getMsgIds().get(i);
                MessageFuture future = futures.remove(msgId);
                // The old version of the server will return BatchResultMessage, so it is necessary to remove the msgId
                // from the childToParentMap.
                Integer parentId = childToParentMap.remove(msgId);
                if (parentId != null) {
                    mergeMsgMap.remove(parentId);
                }
                if (future == null) {
                    LOGGER.error("msg: {} is not found in futures, result message: {}", msgId,
                        batchResultMessage.getResultMessages().get(i));
                } else {
                    future.setResultMessage(batchResultMessage.getResultMessages().get(i));
                }
            }
        } else {
            Integer id = rpcMessage.getId();
            try {
                MessageFuture messageFuture = futures.remove(id);
                if (messageFuture != null) {
                    messageFuture.setResultMessage(rpcMessage.getBody());
                } else {
                    if (rpcMessage.getBody() instanceof AbstractResultMessage) {
                        if (transactionMessageHandler != null) {
                            transactionMessageHandler.onResponse((AbstractResultMessage)rpcMessage.getBody(), null);
                        }
                    }
                }
            } finally {
                // In version 2.3.0, the server does not return MergeResultMessage and BatchResultMessage
                // so it is necessary to clear childToParentMap and mergeMsgMap here.
                Integer parentId = childToParentMap.remove(id);
                if (parentId != null) {
                    mergeMsgMap.remove(parentId);
                }
            }
        }
    }
}
