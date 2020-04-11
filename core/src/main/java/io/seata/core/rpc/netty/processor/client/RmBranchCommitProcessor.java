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
package io.seata.core.rpc.netty.processor.client;

import io.netty.channel.ChannelHandlerContext;
import io.seata.common.exception.FrameworkErrorCode;
import io.seata.common.util.NetUtil;
import io.seata.core.protocol.ResultCode;
import io.seata.core.protocol.RpcMessage;
import io.seata.core.protocol.transaction.BranchCommitRequest;
import io.seata.core.protocol.transaction.BranchCommitResponse;
import io.seata.core.rpc.RemotingClient;
import io.seata.core.rpc.TransactionMessageHandler;
import io.seata.core.rpc.netty.processor.NettyProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * process TC global commit command.
 * <p>
 * message type:
 * {@link BranchCommitRequest}
 *
 * @author zhangchenghui.dev@gmail.com
 * @since 1.2.0
 */
public class RmBranchCommitProcessor implements NettyProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(RmBranchCommitProcessor.class);

    private TransactionMessageHandler handler;

    private RemotingClient remotingClient;

    public RmBranchCommitProcessor(TransactionMessageHandler handler, RemotingClient remotingClient) {
        this.handler = handler;
        this.remotingClient = remotingClient;
    }

    @Override
    public void process(ChannelHandlerContext ctx, RpcMessage rpcMessage) throws Exception {
        String remoteAddress = NetUtil.toStringAddress(ctx.channel().remoteAddress());
        Object msg = rpcMessage.getBody();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("rm client handle branch commit process:" + msg);
        }
        doBranchCommit(rpcMessage, remoteAddress, (BranchCommitRequest) msg);
    }

    private void doBranchCommit(RpcMessage request, String serverAddress, BranchCommitRequest branchCommitRequest) {

        BranchCommitResponse resultMessage = null;
        try {
            resultMessage = (BranchCommitResponse) handler.onRequest(branchCommitRequest, null);
            this.remotingClient.sendResponse(request, serverAddress, resultMessage);
        } catch (Exception e) {
            LOGGER.error(FrameworkErrorCode.NetOnMessage.getErrCode(), e.getMessage(), e);
            if (resultMessage == null) {
                resultMessage = new BranchCommitResponse();
            }
            resultMessage.setResultCode(ResultCode.Failed);
            resultMessage.setMsg(e.getMessage());
            this.remotingClient.sendResponse(request, serverAddress, resultMessage);
        }
    }
}
