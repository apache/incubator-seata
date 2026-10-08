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
package org.apache.seata.server.session;

import org.apache.seata.core.context.RootContext;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.model.*;
import org.apache.seata.server.BaseSpringBootTest;
import org.apache.seata.server.cluster.raft.context.SeataClusterContext;
import org.apache.seata.server.metrics.MetricsPublisher;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.slf4j.MDC;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SessionHelperUnitTest extends BaseSpringBootTest {
    @Test
    void branchIterationPreservesResourceOrderingAndPropagatesResults() throws Exception {
        BranchSession first = SessionHelper.newBranch(BranchType.AT, "xid", 1, "resource", "data");
        BranchSession second = SessionHelper.newBranch(BranchType.AT, "xid", 2, "resource", "data");
        List<BranchSession> branches = Arrays.asList(first, second);
        List<Long> visited = Collections.synchronizedList(new ArrayList<>());
        BranchSessionHandler handler = b -> {
            assertEquals(String.valueOf(b.getBranchId()), MDC.get(RootContext.MDC_KEY_BRANCH_ID));
            visited.add(b.getBranchId());
            return null;
        };
        assertNull(SessionHelper.parallelForEach(branches, handler));
        assertEquals(Arrays.asList(1L, 2L), visited);
        assertNull(SessionHelper.singleForEach(Collections.<BranchSession>emptyList(), handler));
        assertTrue(SessionHelper.forEach(branches, (BranchSessionHandler) b -> true));
        assertFalse(SessionHelper.singleForEach(branches, (BranchSessionHandler) b -> false));
        assertFalse(SessionHelper.parallelForEach(branches, (BranchSessionHandler) b -> false));
        TransactionException failure = new TransactionException("failed");
        assertSame(
                failure,
                assertThrows(
                        TransactionException.class,
                        () -> SessionHelper.parallelForEach(branches, (BranchSessionHandler) b -> {
                            throw failure;
                        })));
        assertThrows(
                TransactionException.class,
                () -> SessionHelper.parallelForEach(branches, (BranchSessionHandler) b -> {
                    throw new IllegalStateException("failed");
                }));
        assertNull(MDC.get(RootContext.MDC_KEY_BRANCH_ID));
    }

    @Test
    void globalIterationIsolatesFailuresAndClearsContext() {
        GlobalSession a = new GlobalSession();
        a.setXid("a");
        GlobalSession b = new GlobalSession();
        b.setXid("b");
        AtomicInteger calls = new AtomicInteger();
        GlobalSessionHandler handler = s -> {
            assertEquals(s.getXid(), MDC.get(RootContext.MDC_KEY_XID));
            calls.incrementAndGet();
            throw new IllegalStateException("failed");
        };
        SessionHelper.singleForEach(Arrays.asList(a, b), handler);
        assertEquals(2, calls.get());
        SessionHelper.parallelForEach(Arrays.asList(a, b), handler);
        SessionHelper.forEach(Arrays.asList(a, b), handler);
        assertEquals(6, calls.get());
        assertNull(MDC.get(RootContext.MDC_KEY_XID));
        assertNull(SeataClusterContext.getGroup());
    }

    @Test
    void branchRemovalUnlocksAllBranchesAndFinalStatusClosesSession() throws Exception {
        GlobalSession global = mock(GlobalSession.class);
        BranchSession branch = new BranchSession();
        when(global.getSortedBranches()).thenReturn(Collections.singletonList(branch));
        SessionHelper.removeAllBranch(global, false);
        verify(global).removeAndUnlockBranch(branch);
        when(global.getSortedBranches()).thenReturn(Collections.emptyList());
        SessionHelper.removeAllBranch(global, false);
        verify(global, times(1)).removeAndUnlockBranch(branch);
        try (MockedStatic<MetricsPublisher> metrics = mockStatic(MetricsPublisher.class)) {
            for (GlobalStatus status : Arrays.asList(
                    GlobalStatus.Committed,
                    GlobalStatus.Finished,
                    GlobalStatus.Rollbacked,
                    GlobalStatus.TimeoutRollbacked)) {
                reset(global);
                when(global.getStatus()).thenReturn(status);
                SessionHelper.processEndState(global);
                verify(global).end();
            }
            when(global.getStatus()).thenReturn(GlobalStatus.Begin);
            assertThrows(TransactionException.class, () -> SessionHelper.processEndState(global));
        }
    }
}
