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
package io.seata.mockserver.call;

import io.netty.channel.Channel;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.BranchType;
import io.seata.core.protocol.transaction.AbstractBranchEndRequest;
import io.seata.core.protocol.transaction.BranchCommitRequest;
import io.seata.core.protocol.transaction.BranchCommitResponse;
import io.seata.core.protocol.transaction.BranchRollbackRequest;
import io.seata.core.protocol.transaction.BranchRollbackResponse;
import io.seata.core.protocol.transaction.UndoLogDeleteRequest;
import io.seata.core.rpc.RemotingServer;
import io.seata.core.rpc.netty.ChannelManager;
import io.seata.server.session.BranchSession;

import java.util.concurrent.TimeoutException;

/**
 * call rm
 **/
public class CallRm {

    /**
     * call branchCommit :TYPE_BRANCH_COMMIT = 3 , TYPE_BRANCH_COMMIT_RESULT = 4
     *
     * @param remotingServer
     * @return
     */
    public static BranchStatus branchCommit(RemotingServer remotingServer, BranchSession branchSession) {
        BranchCommitRequest request = new BranchCommitRequest();
        setReq(request, branchSession);

        try {
            BranchCommitResponse response = (BranchCommitResponse) remotingServer.sendSyncRequest(
                    branchSession.getResourceId(), branchSession.getClientId(), request, false);
            return response.getBranchStatus();
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * call branchRollback :TYPE_BRANCH_ROLLBACK = 5 , TYPE_BRANCH_ROLLBACK_RESULT = 6
     *
     * @param remotingServer
     * @return
     */
    public static BranchStatus branchRollback(RemotingServer remotingServer,  BranchSession branchSession) {
        BranchRollbackRequest request = new BranchRollbackRequest();
        setReq(request, branchSession);

        try {
            BranchRollbackResponse response = (BranchRollbackResponse) remotingServer.sendSyncRequest(
                    branchSession.getResourceId(), branchSession.getClientId(), request, false);
            return response.getBranchStatus();
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * call deleteUndoLog :TYPE_RM_DELETE_UNDOLOG = 111
     *
     * @param remotingServer
     * @return
     */
    public static void deleteUndoLog(RemotingServer remotingServer, BranchSession branchSession) {
        UndoLogDeleteRequest request = new UndoLogDeleteRequest();
        request.setResourceId(branchSession.getResourceId());
        request.setSaveDays((short) 1);
        request.setBranchType(BranchType.TCC);
        try {
            Channel channel = ChannelManager.getChannel(branchSession.getResourceId(), branchSession.getClientId(), false);
            remotingServer.sendAsyncRequest(channel, request);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void setReq(AbstractBranchEndRequest request, BranchSession branchSession) {
        request.setXid(branchSession.getXid());
        request.setBranchId(branchSession.getBranchId());
        request.setResourceId(branchSession.getResourceId());
        request.setApplicationData("{\"k\":\"v\"}");
        request.setBranchType(BranchType.TCC);
        // todo AT SAGA
    }
}
