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

import java.util.concurrent.TimeoutException;

/**
 * call rm
 *
 * @author minghua.xie
 * @date 2023/11/21
 **/
public class CallRm {

    /**
     * call branchCommit :TYPE_BRANCH_COMMIT = 3 , TYPE_BRANCH_COMMIT_RESULT = 4
     *
     * @param remotingServer
     * @return
     */
    public static BranchStatus branchCommit(RemotingServer remotingServer, String resourceId, String clientId) {
        BranchCommitRequest request = new BranchCommitRequest();
        setReq(request, resourceId);

        try {
            BranchCommitResponse response = (BranchCommitResponse) remotingServer.sendSyncRequest(
                    resourceId, clientId, request, false);
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
    public static BranchStatus branchRollback(RemotingServer remotingServer, String resourceId, String clientId) {
        BranchRollbackRequest request = new BranchRollbackRequest();
        setReq(request, resourceId);

        try {
            BranchRollbackResponse response = (BranchRollbackResponse) remotingServer.sendSyncRequest(
                    resourceId, clientId, request, false);
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
    public static void deleteUndoLog(RemotingServer remotingServer, String resourceId, String clientId) {
        UndoLogDeleteRequest request = new UndoLogDeleteRequest();
        request.setResourceId(resourceId);
        request.setSaveDays((short) 1);
        request.setBranchType(BranchType.TCC);
        try {
            Channel channel = ChannelManager.getChannel(resourceId, clientId, false);
            remotingServer.sendAsyncRequest(channel, request);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void setReq(AbstractBranchEndRequest request, String resourceId) {
        request.setXid("1");
        request.setBranchId(1L);
        request.setResourceId(resourceId);
        request.setApplicationData("{\"k\":\"v\"}");
        request.setBranchType(BranchType.TCC);
        // todo AT SAGA
    }
}
