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

import io.netty.channel.ChannelHandlerContext;
import io.seata.core.protocol.RpcMessage;
import io.seata.core.protocol.transaction.UndoLogDeleteRequest;
import io.seata.core.rpc.TransactionMessageHandler;
import io.seata.core.rpc.processor.RemotingProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * process TC undo log delete command.
 * <p>
 * process message type:
 * {@link UndoLogDeleteRequest}
 *
 * @author zhangchenghui.dev@gmail.com
 * @since 1.3.0
 */
public class RmUndoLogProcessor implements RemotingProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(RmUndoLogProcessor.class);

    private TransactionMessageHandler handler;

    public RmUndoLogProcessor(TransactionMessageHandler handler) {
        this.handler = handler;
    }

    @Override
    public void process(ChannelHandlerContext ctx, RpcMessage rpcMessage) throws Exception {
        Object msg = rpcMessage.getBody();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("rm handle undo log process:" + msg);
        }
        handleUndoLogDelete((UndoLogDeleteRequest) msg);
    }

    private void handleUndoLogDelete(UndoLogDeleteRequest undoLogDeleteRequest) {
        try {
            handler.onRequest(undoLogDeleteRequest, null);
        } catch (Exception e) {
            LOGGER.error("Failed to delete undo log by undoLogDeleteRequest on" + undoLogDeleteRequest.getResourceId());
        }
    }
}
