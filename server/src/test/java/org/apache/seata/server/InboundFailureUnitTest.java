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
package org.apache.seata.server;

import org.apache.seata.common.exception.StoreException;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.core.protocol.transaction.*;
import org.apache.seata.server.session.*;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InboundFailureUnitTest extends BaseSpringBootTest {
    @Test
    void commitAndRollbackFailuresReportPersistedStatus() throws Exception {
        AbstractTCInboundHandler handler = mock(AbstractTCInboundHandler.class, CALLS_REAL_METHODS);
        GlobalCommitRequest commit = new GlobalCommitRequest();
        commit.setXid("xid");
        GlobalRollbackRequest rollback = new GlobalRollbackRequest();
        rollback.setXid("xid");
        doThrow(new StoreException("offline")).when(handler).doGlobalCommit(eq(commit), any(), isNull());
        doThrow(new StoreException("offline")).when(handler).doGlobalRollback(eq(rollback), any(), isNull());
        GlobalSession session = mock(GlobalSession.class);
        when(session.getStatus()).thenReturn(GlobalStatus.CommitRetrying);
        try (MockedStatic<SessionHolder> holder = mockStatic(SessionHolder.class)) {
            holder.when(() -> SessionHolder.findGlobalSession("xid", false)).thenReturn(session);
            assertEquals(
                    GlobalStatus.CommitRetrying, handler.handle(commit, null).getGlobalStatus());
            assertEquals(
                    GlobalStatus.CommitRetrying, handler.handle(rollback, null).getGlobalStatus());
            holder.when(() -> SessionHolder.findGlobalSession("xid", false)).thenReturn(null);
            assertEquals(GlobalStatus.Finished, handler.handle(commit, null).getGlobalStatus());
            assertEquals(GlobalStatus.Finished, handler.handle(rollback, null).getGlobalStatus());
            holder.when(() -> SessionHolder.findGlobalSession("xid", false))
                    .thenThrow(new IllegalStateException("read failed"));
            assertEquals(GlobalStatus.Committing, handler.handle(commit, null).getGlobalStatus());
            assertEquals(
                    GlobalStatus.Rollbacking, handler.handle(rollback, null).getGlobalStatus());
        }
    }
}
