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
package org.apache.seata.server.coordinator;

import org.apache.seata.core.model.BranchStatus;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.core.protocol.transaction.BranchCommitRequest;
import org.apache.seata.core.protocol.transaction.BranchCommitResponse;
import org.apache.seata.core.protocol.transaction.BranchRollbackRequest;
import org.apache.seata.core.protocol.transaction.BranchRollbackResponse;
import org.apache.seata.core.rpc.RemotingServer;
import org.apache.seata.server.BaseSpringBootTest;
import org.apache.seata.server.session.BranchSession;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AbstractCoreLoadBalanceTest extends BaseSpringBootTest {
    @Test
    void phaseTwoRequestsPassBranchTypeToRemotingServer() throws Exception {
        for (BranchType branchType : new BranchType[] {BranchType.AT, BranchType.TCC, BranchType.XA}) {
            RemotingServer server = mock(RemotingServer.class);
            AbstractCore core = new AbstractCore(server) {
                @Override
                public BranchType getHandleBranchType() {
                    return branchType;
                }
            };
            BranchSession branch = new BranchSession();
            branch.setBranchType(branchType);
            branch.setResourceId("resource");
            branch.setClientId("app:127.0.0.1:18001");
            branch.setXid("127.0.0.1:8091:123");
            branch.setBranchId(456L);
            BranchCommitResponse commit = new BranchCommitResponse();
            commit.setBranchStatus(BranchStatus.PhaseTwo_Committed);
            BranchRollbackResponse rollback = new BranchRollbackResponse();
            rollback.setBranchStatus(BranchStatus.PhaseTwo_Rollbacked);
            when(server.sendSyncRequest(
                            eq(branch.getResourceId()),
                            eq(branch.getClientId()),
                            any(BranchCommitRequest.class),
                            eq(branch.isAT()),
                            eq(branchType)))
                    .thenAnswer(invocation -> {
                        BranchCommitRequest request = invocation.getArgument(2);
                        assertEquals(branch.getXid(), request.getXid());
                        assertEquals(branchType, request.getBranchType());
                        return commit;
                    });
            when(server.sendSyncRequest(
                            eq(branch.getResourceId()),
                            eq(branch.getClientId()),
                            any(BranchRollbackRequest.class),
                            eq(branch.isAT()),
                            eq(branchType)))
                    .thenReturn(rollback);
            assertEquals(BranchStatus.PhaseTwo_Committed, core.branchCommit(null, branch));
            assertEquals(BranchStatus.PhaseTwo_Rollbacked, core.branchRollback(null, branch));
            verify(server)
                    .sendSyncRequest(
                            eq(branch.getResourceId()),
                            eq(branch.getClientId()),
                            any(BranchCommitRequest.class),
                            eq(branch.isAT()),
                            eq(branchType));
            verify(server)
                    .sendSyncRequest(
                            eq(branch.getResourceId()),
                            eq(branch.getClientId()),
                            any(BranchRollbackRequest.class),
                            eq(branch.isAT()),
                            eq(branchType));
            verify(server, never()).sendSyncRequest(anyString(), anyString(), any(), anyBoolean());
        }
    }
}
