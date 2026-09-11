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
package io.seata.core.rpc.processor.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.netty.channel.ChannelHandlerContext;
import io.seata.core.protocol.AbstractResultMessage;
import io.seata.core.protocol.BatchResultMessage;
import io.seata.core.protocol.MergeMessage;
import io.seata.core.protocol.MergeResultMessage;
import io.seata.core.protocol.MergedWarpMessage;
import io.seata.core.protocol.MessageFuture;
import io.seata.core.protocol.RegisterRMResponse;
import io.seata.core.protocol.RegisterTMResponse;
import io.seata.core.protocol.RpcMessage;
import io.seata.core.protocol.transaction.BranchRegisterResponse;
import io.seata.core.protocol.transaction.BranchReportResponse;
import io.seata.core.protocol.transaction.GlobalBeginResponse;
import io.seata.core.protocol.transaction.GlobalCommitResponse;
import io.seata.core.protocol.transaction.GlobalLockQueryResponse;
import io.seata.core.protocol.transaction.GlobalReportResponse;
import io.seata.core.protocol.transaction.GlobalRollbackResponse;
import io.seata.core.rpc.TransactionMessageHandler;
import io.seata.core.rpc.processor.RemotingProcessor;
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
 * @author zhangchenghui.dev@gmail.com
 * @since 1.3.0
 */
public class ClientOnResponseProcessor implements RemotingProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientOnResponseProcessor.class);

    /**
     * The Merge msg map from io.seata.core.rpc.netty.AbstractNettyRemotingClient#mergeMsgMap.
     */
    private Map<Integer, MergeMessage> mergeMsgMap;

    /**
     * The Futures from io.seata.core.rpc.netty.AbstractNettyRemoting#futures
     */
    private final ConcurrentMap<Integer, MessageFuture> futures;

    /**
     * To handle the received RPC message on upper level.
     */
    private final TransactionMessageHandler transactionMessageHandler;

    public ClientOnResponseProcessor(Map<Integer, MergeMessage> mergeMsgMap,
                                     ConcurrentHashMap<Integer, MessageFuture> futures,
                                     TransactionMessageHandler transactionMessageHandler) {
        this.mergeMsgMap = mergeMsgMap;
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
                if (future == null) {
                    LOGGER.error("msg: {} is not found in futures, result message: {}", msgId,results.getMsgs()[i]);
                } else {
                    future.setResultMessage(results.getMsgs()[i]);
                }
            }
        } else if (rpcMessage.getBody() instanceof BatchResultMessage) {
            try {
                BatchResultMessage batchResultMessage = (BatchResultMessage) rpcMessage.getBody();
                for (int i = 0; i < batchResultMessage.getMsgIds().size(); i++) {
                    int msgId = batchResultMessage.getMsgIds().get(i);
                    MessageFuture future = futures.remove(msgId);
                    if (future == null) {
                        LOGGER.error("msg: {} is not found in futures, result message: {}", msgId, batchResultMessage.getResultMessages().get(i));
                    } else {
                        future.setResultMessage(batchResultMessage.getResultMessages().get(i));
                    }
                }
            } finally {
                // In order to be compatible with the old version, in the batch sending of version 1.5.0,
                // batch messages will also be placed in the local cache of mergeMsgMap,
                // but version 1.5.0 no longer needs to obtain batch messages from mergeMsgMap
                mergeMsgMap.clear();
            }
        } else {
            MessageFuture messageFuture = futures.remove(rpcMessage.getId());
            if (messageFuture != null) {
                messageFuture.setResultMessage(rpcMessage.getBody());
            } else {
                if (rpcMessage.getBody() instanceof AbstractResultMessage) {
                    if (transactionMessageHandler != null) {
                        transactionMessageHandler.onResponse((AbstractResultMessage) rpcMessage.getBody(), null);
                    }
                }
            }
        }
    }
}
