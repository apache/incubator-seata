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

import com.alipay.sofa.jraft.RouteTable;
import com.alipay.sofa.jraft.conf.Configuration;
import com.alipay.sofa.jraft.entity.PeerId;
import com.alipay.sofa.jraft.rpc.*;
import com.alipay.sofa.jraft.rpc.impl.cli.CliClientServiceImpl;
import org.apache.seata.common.metadata.*;
import org.apache.seata.server.BaseSpringBootTest;
import org.apache.seata.server.cluster.raft.processor.request.PutNodeMetadataRequest;
import org.apache.seata.server.cluster.raft.processor.response.PutNodeMetadataResponse;
import org.apache.seata.server.cluster.raft.sync.msg.dto.RaftClusterMetadata;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RaftMetadataSyncUnitTest extends BaseSpringBootTest {
    @Test
    void metadataSyncPublishesRoleAndHandlesSuccessRejectionAndTransportFailure() throws Exception {
        RaftStateMachine machine = mock(RaftStateMachine.class, CALLS_REAL_METHODS);
        RaftClusterMetadata metadata = new RaftClusterMetadata();
        Node leader = new Node();
        leader.setVersion("2.8.0");
        metadata.setLeader(leader);
        AtomicBoolean syncing = new AtomicBoolean(true);
        ScheduledFuture<?> future = mock(ScheduledFuture.class);
        ReflectionTestUtils.setField(machine, "group", "unit");
        ReflectionTestUtils.setField(machine, "initSync", syncing);
        ReflectionTestUtils.setField(machine, "scheduledFuture", future);
        machine.setRaftLeaderMetadata(metadata);
        PeerId local = new PeerId("127.0.0.1", 9091), remote = new PeerId("127.0.0.1", 9092);
        RaftServer server = mock(RaftServer.class);
        when(server.getServerId()).thenReturn(local);
        CliClientServiceImpl cli = mock(CliClientServiceImpl.class);
        RpcClient client = mock(RpcClient.class);
        when(cli.getRpcClient()).thenReturn(client);
        RouteTable routes = mock(RouteTable.class);
        when(routes.getConfiguration("unit")).thenReturn(new Configuration(Collections.singletonList(local)));
        try (MockedStatic<RaftServerManager> servers = mockStatic(RaftServerManager.class);
                MockedStatic<RouteTable> table = mockStatic(RouteTable.class)) {
            servers.when(() -> RaftServerManager.getRaftServer("unit")).thenReturn(server);
            servers.when(RaftServerManager::getCliClientServiceInstance).thenReturn(cli);
            table.when(RouteTable::getInstance).thenReturn(routes);
            ReflectionTestUtils.invokeMethod(machine, "syncCurrentNodeInfo", remote);
            ArgumentCaptor<InvokeCallback> callback = ArgumentCaptor.forClass(InvokeCallback.class);
            ArgumentCaptor<PutNodeMetadataRequest> request = ArgumentCaptor.forClass(PutNodeMetadataRequest.class);
            verify(client)
                    .invokeAsync(
                            eq(remote.getEndpoint()),
                            request.capture(),
                            any(InvokeContext.class),
                            callback.capture(),
                            eq(30000L));
            assertEquals(ClusterRole.FOLLOWER, request.getValue().getNode().getRole());
            PutNodeMetadataResponse response = new PutNodeMetadataResponse(true);
            response.setSuccess(true);
            callback.getValue().complete(response, null);
            verify(future).cancel(true);
            response.setSuccess(false);
            callback.getValue().complete(response, null);
            assertFalse(syncing.get());
            syncing.set(true);
            callback.getValue().complete(null, new IllegalStateException("offline"));
            assertFalse(syncing.get());
            metadata.setLeader(null);
            syncing.set(true);
            ReflectionTestUtils.invokeMethod(machine, "syncCurrentNodeInfo", remote);
            assertFalse(syncing.get());
            when(routes.selectLeader("unit")).thenReturn(null);
            ReflectionTestUtils.invokeMethod(machine, "syncCurrentNodeInfo", "unit");
            assertFalse(syncing.get());
        }
    }
}
