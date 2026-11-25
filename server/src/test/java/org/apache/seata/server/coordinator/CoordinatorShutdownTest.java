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

import io.netty.channel.Channel;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.protocol.transaction.BranchCommitRequest;
import org.apache.seata.core.rpc.netty.NettyRemotingServer;
import org.apache.seata.server.BaseSpringBootTest;
import org.apache.seata.server.session.BranchSession;
import org.apache.seata.server.session.BranchSessionHandler;
import org.apache.seata.server.session.SessionHelper;
import org.apache.seata.server.session.SessionHolder;
import org.apache.seata.server.session.SessionManager;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.apache.seata.common.Constants.RETRY_COMMITTING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CoordinatorShutdownTest extends BaseSpringBootTest {
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void waitsForParallelRetryBeforeClosingResources(boolean interruptCloser) throws Exception {
        NettyRemotingServer server = mock(NettyRemotingServer.class);
        Channel channel = mock(Channel.class);
        BranchCommitRequest request = new BranchCommitRequest();
        SessionManager store = mock(SessionManager.class);
        Runnable unlock = mock(Runnable.class);
        CountDownLatch waitingForReply = new CountDownLatch(1);
        CountDownLatch allowReply = new CountDownLatch(1);
        when(server.sendSyncRequest(channel, request)).thenAnswer(invocation -> {
            waitingForReply.countDown();
            assertTrue(allowReply.await(30, TimeUnit.SECONDS));
            return null;
        });

        BranchSession first = new BranchSession();
        first.setResourceId("db1");
        BranchSession pending = new BranchSession();
        pending.setResourceId("db2");
        BranchSessionHandler handler = branch -> {
            if (branch == first) {
                return null;
            }
            try {
                server.sendSyncRequest(channel, request);
                store.findGlobalSession("xid");
                return null;
            } catch (IOException | TimeoutException e) {
                throw new TransactionException(e);
            }
        };

        ExecutorService closer = Executors.newSingleThreadExecutor();
        Object previousCoordinator = ReflectionTestUtils.getField(DefaultCoordinator.class, "instance");
        try (MockedConstruction<DefaultCore> ignored = mockConstruction(DefaultCore.class)) {
            DefaultCoordinator coordinator = new DefaultCoordinator(server);
            ScheduledThreadPoolExecutor retry =
                    (ScheduledThreadPoolExecutor) ReflectionTestUtils.getField(coordinator, "retryCommitting");
            Future<?> scan = retry.submit(() -> runLockedRetry(first, pending, handler, unlock));
            try {
                assertTrue(waitingForReply.await(10, TimeUnit.SECONDS));
                CountDownLatch closing = new CountDownLatch(1);
                Future<Boolean> shutdown = closer.submit(() -> {
                    try (MockedStatic<SessionHolder> holder = mockStatic(SessionHolder.class)) {
                        holder.when(SessionHolder::destroy).thenAnswer(invocation -> {
                            store.destroy();
                            return null;
                        });
                        if (interruptCloser) {
                            Thread.currentThread().interrupt();
                        }
                        closing.countDown();
                        coordinator.destroy();
                        return Thread.interrupted();
                    }
                });
                assertTrue(closing.await(10, TimeUnit.SECONDS));
                // Keep the ordinary shutdown pending beyond the previous five-second interruption threshold.
                long holdMillis = interruptCloser ? 250 : 6000;
                assertThrows(TimeoutException.class, () -> shutdown.get(holdMillis, TimeUnit.MILLISECONDS));
                verify(unlock, never()).run();
                verify(server, never()).destroy();
                verify(store, never()).destroy();
                allowReply.countDown();
                assertEquals(interruptCloser, shutdown.get(10, TimeUnit.SECONDS).booleanValue());
                scan.get(10, TimeUnit.SECONDS);
                InOrder order = inOrder(store, unlock, server);
                order.verify(store).findGlobalSession("xid");
                order.verify(unlock).run();
                order.verify(server).destroy();
                order.verify(store).destroy();
            } finally {
                allowReply.countDown();
                retry.shutdown();
                closer.shutdown();
                assertTrue(retry.awaitTermination(15, TimeUnit.SECONDS));
                assertTrue(closer.awaitTermination(15, TimeUnit.SECONDS));
            }
        } finally {
            closer.shutdownNow();
            ReflectionTestUtils.setField(DefaultCoordinator.class, "instance", previousCoordinator);
        }
    }

    private void runLockedRetry(
            BranchSession first, BranchSession pending, BranchSessionHandler handler, Runnable unlock) {
        try (MockedStatic<SessionHolder> holder = mockStatic(SessionHolder.class)) {
            holder.when(() -> SessionHolder.distributedLockAndExecute(eq(RETRY_COMMITTING), any()))
                    .thenCallRealMethod();
            holder.when(() -> SessionHolder.acquireDistributedLock(RETRY_COMMITTING))
                    .thenReturn(true);
            holder.when(() -> SessionHolder.releaseDistributedLock(RETRY_COMMITTING))
                    .thenAnswer(invocation -> {
                        unlock.run();
                        return true;
                    });
            SessionHolder.distributedLockAndExecute(RETRY_COMMITTING, () -> {
                try {
                    SessionHelper.forEach(Arrays.asList(first, pending), handler, true);
                } catch (TransactionException e) {
                    throw new AssertionError(e);
                }
            });
        }
    }
}
