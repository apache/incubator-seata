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
package io.seata.core.rpc.netty;

import io.seata.common.exception.FrameworkErrorCode;
import io.seata.core.protocol.ResultCode;
import io.seata.core.protocol.transaction.BranchCommitRequest;
import io.seata.core.protocol.transaction.BranchCommitResponse;
import io.seata.core.protocol.transaction.BranchRollbackRequest;
import io.seata.core.protocol.transaction.BranchRollbackResponse;
import io.seata.core.rpc.ClientMessageListener;
import io.seata.core.rpc.ClientMessageSender;
import io.seata.core.rpc.TransactionMessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Rm message listener.
 *
 * @author jimin.jm @alibaba-inc.com
 * @date 2018 /10/11
 */
public class RmMessageListener implements ClientMessageListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(RmMessageListener.class);

    private TransactionMessageHandler handler;

    /**
     * Instantiates a new Rm message listener.
     *
     * @param handler the handler
     */
    public RmMessageListener(TransactionMessageHandler handler) {
        this.handler = handler;
    }

    /**
     * Sets handler.
     *
     * @param handler the handler
     */
    public void setHandler(TransactionMessageHandler handler) {
        this.handler = handler;
    }

    @Override
    public void onMessage(long msgId, String serverAddress, Object msg, ClientMessageSender sender) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("onMessage:" + msg);
        }
        if (msg instanceof BranchCommitRequest) {
            handleBranchCommit(msgId, serverAddress, (BranchCommitRequest)msg, sender);
        } else if (msg instanceof BranchRollbackRequest) {
            handleBranchRollback(msgId, serverAddress, (BranchRollbackRequest)msg, sender);
        }
    }

    private void handleBranchRollback(long msgId, String serverAddress,
                                      BranchRollbackRequest branchRollbackRequest,
                                      ClientMessageSender sender) {
        BranchRollbackResponse resultMessage = null;
        resultMessage = (BranchRollbackResponse)handler.onRequest(branchRollbackRequest, null);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("branch rollback result:" + resultMessage);
        }
        try {
            sender.sendResponse(msgId, serverAddress, resultMessage);
        } catch (Throwable throwable) {
            LOGGER.error("", "send response error", throwable);
        }
    }

    private void handleBranchCommit(long msgId, String serverAddress,
                                    BranchCommitRequest branchCommitRequest,
                                    ClientMessageSender sender) {

        BranchCommitResponse resultMessage = null;
        try {
            resultMessage = (BranchCommitResponse)handler.onRequest(branchCommitRequest, null);
            sender.sendResponse(msgId, serverAddress, resultMessage);
        } catch (Exception e) {
            LOGGER.error(FrameworkErrorCode.NetOnMessage.getErrCode(), e.getMessage(), e);
            if (resultMessage == null) {
                resultMessage = new BranchCommitResponse();
            }
            resultMessage.setResultCode(ResultCode.Failed);
            resultMessage.setMsg(e.getMessage());
            sender.sendResponse(msgId, serverAddress, resultMessage);
        }
    }
}
