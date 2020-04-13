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
import io.seata.core.rpc.netty.RmNettyClient;
import io.seata.core.rpc.processor.RemotingProcessor;
import io.seata.core.rpc.processor.Pair;
import io.seata.core.rpc.processor.client.ClientHeartbeatProcessor;
import io.seata.core.rpc.processor.client.RmBranchCommitProcessor;
import io.seata.core.rpc.processor.client.RmBranchRollbackProcessor;
import io.seata.core.rpc.processor.client.RmUndoLogProcessor;
import io.seata.core.rpc.processor.client.ClientOnResponseProcessor;

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
        RmNettyClient rmNettyClient = RmNettyClient.getInstance(applicationId, transactionServiceGroup);
        rmNettyClient.setResourceManager(DefaultResourceManager.get());
        AbstractRMHandler handler = DefaultRMHandler.get();

        Map<Integer, Pair<RemotingProcessor, Boolean>> processorMap = new HashMap<>();
        // rm client handle branch commit processor
        Pair<RemotingProcessor, Boolean> branchCommitProcessor =
            new Pair<>(new RmBranchCommitProcessor(handler, rmNettyClient), true);
        processorMap.put((int) MessageType.TYPE_BRANCH_COMMIT, branchCommitProcessor);
        // rm client handle branch commit processor
        Pair<RemotingProcessor, Boolean> branchRollbackProcessor =
            new Pair<>(new RmBranchRollbackProcessor(handler, rmNettyClient), true);
        processorMap.put((int) MessageType.TYPE_BRANCH_ROLLBACK, branchRollbackProcessor);
        // rm handler undo log processor
        Pair<RemotingProcessor, Boolean> deleteUndoLogProcessor =
            new Pair<>(new RmUndoLogProcessor(handler), true);
        processorMap.put((int) MessageType.TYPE_RM_DELETE_UNDOLOG, deleteUndoLogProcessor);
        // process TC response processor
        Pair<RemotingProcessor, Boolean> onResponseProcessor =
            new Pair<>(new ClientOnResponseProcessor(rmNettyClient.getMergeMsgMap(), rmNettyClient.getFutures(), handler), false);
        processorMap.put((int) MessageType.TYPE_SEATA_MERGE_RESULT, onResponseProcessor);
        processorMap.put((int) MessageType.TYPE_BRANCH_REGISTER_RESULT, onResponseProcessor);
        processorMap.put((int) MessageType.TYPE_BRANCH_STATUS_REPORT_RESULT, onResponseProcessor);
        processorMap.put((int) MessageType.TYPE_GLOBAL_LOCK_QUERY_RESULT, onResponseProcessor);
        processorMap.put((int) MessageType.TYPE_REG_RM_RESULT, onResponseProcessor);
        // handle heartbeat message processor
        Pair<RemotingProcessor, Boolean> heartbeatMessageProcessor = new Pair<>(new ClientHeartbeatProcessor(), false);
        processorMap.put((int) MessageType.TYPE_HEARTBEAT_MSG, heartbeatMessageProcessor);
        rmNettyClient.setRmProcessor(processorMap);

        rmNettyClient.init();
    }

}
