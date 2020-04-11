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
package io.seata.rm;

import io.seata.core.protocol.MessageType;
import io.seata.core.rpc.netty.RmRpcClient;
import io.seata.core.rpc.netty.processor.NettyProcessor;
import io.seata.core.rpc.netty.processor.Pair;
import io.seata.core.rpc.netty.processor.client.ClientHeartbeatMessageProcessor;
import io.seata.core.rpc.netty.processor.client.RmHandleBranchCommitProcessor;
import io.seata.core.rpc.netty.processor.client.RmHandleBranchRollbackProcessor;
import io.seata.core.rpc.netty.processor.client.RmHandleUndoLogProcessor;
import io.seata.core.rpc.netty.processor.client.ClientOnResponseMessageProcessor;

import java.util.HashMap;
import java.util.Map;

/**
 * The Rm client Initiator.
 *
 * @author slievrly
 */
public class RMClient {

    /**
     * Init.
     *
     * @param applicationId           the application id
     * @param transactionServiceGroup the transaction service group
     */
    public static void init(String applicationId, String transactionServiceGroup) {
        RmRpcClient rmRpcClient = RmRpcClient.getInstance(applicationId, transactionServiceGroup);
        rmRpcClient.setResourceManager(DefaultResourceManager.get());
        AbstractRMHandler handler = DefaultRMHandler.get();

        Map<Integer, Pair<NettyProcessor, Boolean>> processorMap = new HashMap<>();
        // rm client handle branch commit processor
        Pair<NettyProcessor, Boolean> branchCommitProcessor =
            new Pair<>(new RmHandleBranchCommitProcessor(handler, rmRpcClient), true);
        processorMap.put((int) MessageType.TYPE_BRANCH_COMMIT, branchCommitProcessor);
        // rm client handle branch commit processor
        Pair<NettyProcessor, Boolean> branchRollbackProcessor =
            new Pair<>(new RmHandleBranchRollbackProcessor(handler, rmRpcClient), true);
        processorMap.put((int) MessageType.TYPE_BRANCH_ROLLBACK, branchRollbackProcessor);
        // rm handler undo log processor
        Pair<NettyProcessor, Boolean> deleteUndoLogProcessor =
            new Pair<>(new RmHandleUndoLogProcessor(handler), true);
        processorMap.put((int) MessageType.TYPE_RM_DELETE_UNDOLOG, deleteUndoLogProcessor);
        // handle TC response processor
        Pair<NettyProcessor, Boolean> onResponseMessageProcessor =
            new Pair<>(new ClientOnResponseMessageProcessor(rmRpcClient.getMergeMsgMap(), rmRpcClient.getFutures(), handler), false);
        processorMap.put((int) MessageType.TYPE_SEATA_MERGE_RESULT, onResponseMessageProcessor);
        processorMap.put((int) MessageType.TYPE_BRANCH_REGISTER_RESULT, onResponseMessageProcessor);
        processorMap.put((int) MessageType.TYPE_BRANCH_STATUS_REPORT_RESULT, onResponseMessageProcessor);
        processorMap.put((int) MessageType.TYPE_GLOBAL_LOCK_QUERY_RESULT, onResponseMessageProcessor);
        processorMap.put((int) MessageType.TYPE_REG_RM_RESULT, onResponseMessageProcessor);
        // handle heartbeat message processor
        Pair<NettyProcessor, Boolean> heartbeatMessageProcessor = new Pair<>(new ClientHeartbeatMessageProcessor(), false);
        processorMap.put((int) MessageType.TYPE_HEARTBEAT_MSG, heartbeatMessageProcessor);
        rmRpcClient.setRmProcessor(processorMap);

        rmRpcClient.init();
    }

}
