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
package io.seata.core.rpc.processor.server;

import io.netty.channel.ChannelHandlerContext;
import io.seata.common.util.NetUtil;
import io.seata.core.protocol.AbstractMessage;
import io.seata.core.protocol.AbstractResultMessage;
import io.seata.core.protocol.MergeResultMessage;
import io.seata.core.protocol.MergedWarpMessage;
import io.seata.core.protocol.RpcMessage;
import io.seata.core.protocol.transaction.BranchRegisterRequest;
import io.seata.core.protocol.transaction.BranchReportRequest;
import io.seata.core.protocol.transaction.GlobalBeginRequest;
import io.seata.core.protocol.transaction.GlobalCommitRequest;
import io.seata.core.protocol.transaction.GlobalLockQueryRequest;
import io.seata.core.protocol.transaction.GlobalReportRequest;
import io.seata.core.protocol.transaction.GlobalRollbackRequest;
import io.seata.core.protocol.transaction.GlobalStatusRequest;
import io.seata.core.rpc.netty.ChannelManager;
import io.seata.core.rpc.RemotingServer;
import io.seata.core.rpc.RpcContext;
import io.seata.core.rpc.TransactionMessageHandler;
import io.seata.core.rpc.processor.RemotingProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * process RM/TM client request message.
 * <p>
 * message type:
 * RM:
 * 1) {@link MergedWarpMessage}
 * 2) {@link BranchRegisterRequest}
 * 3) {@link BranchReportRequest}
 * 4) {@link GlobalLockQueryRequest}
 * TM:
 * 1) {@link MergedWarpMessage}
 * 2) {@link GlobalBeginRequest}
 * 3) {@link GlobalCommitRequest}
 * 4) {@link GlobalReportRequest}
 * 5) {@link GlobalRollbackRequest}
 * 6) {@link GlobalStatusRequest}
 *
 * @author zhangchenghui.dev@gmail.com
 * @since 1.3.0
 */
public class ServerOnRequestProcessor implements RemotingProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerOnRequestProcessor.class);

    private RemotingServer remotingServer;

    private TransactionMessageHandler transactionMessageHandler;

    public ServerOnRequestProcessor(RemotingServer remotingServer, TransactionMessageHandler transactionMessageHandler) {
        this.remotingServer = remotingServer;
        this.transactionMessageHandler = transactionMessageHandler;
    }

    @Override
    public void process(ChannelHandlerContext ctx, RpcMessage rpcMessage) throws Exception {
        if (ChannelManager.isRegistered(ctx.channel())) {
            onRequestMessage(ctx, rpcMessage);
        } else {
            try {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("closeChannelHandlerContext channel:" + ctx.channel());
                }
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

    private void onRequestMessage(ChannelHandlerContext ctx, RpcMessage rpcMessage) {
        Object message = rpcMessage.getBody();
        RpcContext rpcContext = ChannelManager.getContextFromIdentified(ctx.channel());
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("server received:{},clientIp:{},vgroup:{}", message,
                NetUtil.toIpAddress(ctx.channel().remoteAddress()), rpcContext.getTransactionServiceGroup());
        } else {
            try {
                BatchLogHandler.INSTANCE.getLogQueue()
                    .put(message + ",clientIp:" + NetUtil.toIpAddress(ctx.channel().remoteAddress()) + ",vgroup:"
                        + rpcContext.getTransactionServiceGroup());
            } catch (InterruptedException e) {
                LOGGER.error("put message to logQueue error: {}", e.getMessage(), e);
            }
        }
        if (!(message instanceof AbstractMessage)) {
            return;
        }
        if (message instanceof MergedWarpMessage) {
            AbstractResultMessage[] results = new AbstractResultMessage[((MergedWarpMessage) message).msgs.size()];
            for (int i = 0; i < results.length; i++) {
                final AbstractMessage subMessage = ((MergedWarpMessage) message).msgs.get(i);
                results[i] = transactionMessageHandler.onRequest(subMessage, rpcContext);
            }
            MergeResultMessage resultMessage = new MergeResultMessage();
            resultMessage.setMsgs(results);
            remotingServer.sendAsyncResponse(rpcMessage, ctx.channel(), resultMessage);
        } else {
            // the single send request message
            final AbstractMessage msg = (AbstractMessage) message;
            AbstractResultMessage result = transactionMessageHandler.onRequest(msg, rpcContext);
            remotingServer.sendAsyncResponse(rpcMessage, ctx.channel(), result);
        }
    }

}
