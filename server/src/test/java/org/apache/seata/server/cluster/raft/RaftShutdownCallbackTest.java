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
package org.apache.seata.server.cluster.raft;

import com.alipay.sofa.jraft.RaftGroupService;
import org.apache.seata.common.holder.ObjectHolder;
import org.apache.seata.common.store.SessionMode;
import org.apache.seata.server.store.StoreConfig;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicLong;

import static org.apache.seata.common.Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RaftShutdownCallbackTest {
    @Test
    void stopsMetadataBeforeGroupShutdownAndIgnoresLateLeaderStart() throws Exception {
        Object environment = ObjectHolder.INSTANCE.getObject(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT);
        if (environment == null) {
            ObjectHolder.INSTANCE.setObject(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT, new StandardEnvironment());
        }
        try (MockedStatic<StoreConfig> store = mockStatic(StoreConfig.class);
                MockedStatic<RaftServerManager> managers = mockStatic(RaftServerManager.class)) {
            store.when(StoreConfig::getSessionMode).thenReturn(SessionMode.FILE);
            RaftStateMachine stateMachine = new RaftStateMachine("shutdown-test");
            AtomicLong leaderTerm = (AtomicLong) ReflectionTestUtils.getField(stateMachine, "leaderTerm");
            leaderTerm.set(5);
            stateMachine.getCurrentTerm().set(5);
            assertTrue(stateMachine.isLeader());
            ScheduledFuture<?> retry = mock(ScheduledFuture.class);
            ReflectionTestUtils.setField(stateMachine, "scheduledFuture", retry);
            RaftGroupService group = mock(RaftGroupService.class);
            RaftServer server = mock(RaftServer.class, CALLS_REAL_METHODS);
            ReflectionTestUtils.setField(server, "raftStateMachine", stateMachine);
            ReflectionTestUtils.setField(server, "raftGroupService", group);
            doAnswer(invocation -> {
                        assertFalse(stateMachine.isLeader(), "Stop submissions before group shutdown starts");
                        stateMachine.onLeaderStart(6);
                        stateMachine.syncMetadata();
                        return null;
                    })
                    .when(group)
                    .shutdown();

            server.destroy();

            assertFalse(stateMachine.isLeader());
            assertEquals(5, stateMachine.getCurrentTerm().get(), "Late callbacks must not advance the term");
            verify(retry).cancel(false);
            verify(group).join();
            managers.verifyNoInteractions();
        } finally {
            if (environment == null) {
                Map<?, ?> objects = (Map<?, ?>) ReflectionTestUtils.getField(ObjectHolder.class, "OBJECT_MAP");
                objects.remove(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT);
            }
        }
    }
}
