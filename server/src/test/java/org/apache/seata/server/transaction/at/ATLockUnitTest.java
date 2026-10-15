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
package org.apache.seata.server.transaction.at;

import org.apache.seata.common.exception.StoreException;
import org.apache.seata.core.exception.*;
import org.apache.seata.core.rpc.RemotingServer;
import org.apache.seata.server.BaseSpringBootTest;
import org.apache.seata.server.session.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ATLockUnitTest extends BaseSpringBootTest {
    @Test
    void applicationDataControlsLockFlagsAndMalformedJsonFallsBack() throws Exception {
        ATCore core = new ATCore(mock(RemotingServer.class));
        GlobalSession global = mock(GlobalSession.class);
        BranchSession branch = mock(BranchSession.class);
        when(branch.lock(anyBoolean(), anyBoolean())).thenReturn(true);
        when(branch.getApplicationData()).thenReturn("{\"autoCommit\":false,\"skipCheckLock\":true}");
        core.branchSessionLock(global, branch);
        verify(branch).lock(false, true);
        when(branch.getApplicationData()).thenReturn("invalid-json");
        core.branchSessionLock(global, branch);
        verify(branch).lock(true, false);
        when(branch.lock(true, false)).thenReturn(false);
        assertEquals(
                TransactionExceptionCode.LockKeyConflict,
                assertThrows(BranchTransactionException.class, () -> core.branchSessionLock(global, branch))
                        .getCode());
        doThrow(new StoreException(new BranchTransactionException(TransactionExceptionCode.LockKeyConflictFailFast)))
                .when(branch)
                .lock(anyBoolean(), anyBoolean());
        assertEquals(
                TransactionExceptionCode.LockKeyConflictFailFast,
                assertThrows(BranchTransactionException.class, () -> core.branchSessionLock(global, branch))
                        .getCode());
        doThrow(new StoreException("offline")).when(branch).lock(anyBoolean(), anyBoolean());
        assertThrows(StoreException.class, () -> core.branchSessionLock(global, branch));
        core.branchSessionUnlock(branch);
        verify(branch).unlock();
    }

    @Test
    void transportFailuresAreTranslatedToBranchErrors() throws Exception {
        RemotingServer remoting = mock(RemotingServer.class);
        ATCore core = new ATCore(remoting);
        BranchSession branch = new BranchSession();
        branch.setResourceId("db");
        branch.setClientId("client");
        when(remoting.sendSyncRequest(eq("db"), eq("client"), any(), anyBoolean()))
                .thenThrow(new IOException("offline"));
        assertEquals(
                TransactionExceptionCode.FailedToSendBranchCommitRequest,
                assertThrows(BranchTransactionException.class, () -> core.branchDelete(new GlobalSession(), branch))
                        .getCode());
        assertEquals(
                TransactionExceptionCode.FailedToSendBranchRollbackRequest,
                assertThrows(BranchTransactionException.class, () -> core.branchRollback(new GlobalSession(), branch))
                        .getCode());
    }
}
