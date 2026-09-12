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
package org.apache.seata.core.rpc.netty.loadbalance;

import io.netty.channel.Channel;
import org.apache.seata.core.rpc.RpcContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test for server load balance behavior.
 */
public class ServerLoadBalanceBehaviorTest {

    @Test
    public void testRandomLoadBalanceShouldReturnValidCandidate() {
        RpcContext c1 = buildRpcContext("app:10.10.10.1:8091", "10.10.10.1", 8091);
        RpcContext c2 = buildRpcContext("app:10.10.10.2:8091", "10.10.10.2", 8091);
        List<RpcContext> candidates = Arrays.asList(c1, c2);

        ServerRandomLoadBalance lb = new ServerRandomLoadBalance();
        for (int i = 0; i < 100; i++) {
            RpcContext selected = lb.select(candidates);
            Assertions.assertNotNull(selected);
            Assertions.assertTrue(candidates.contains(selected));
        }
    }

    @Test
    public void testRoundRobinLoadBalanceShouldDistributeEvenly() {
        RpcContext c1 = buildRpcContext("app:10.10.10.1:8091", "10.10.10.1", 8091);
        RpcContext c2 = buildRpcContext("app:10.10.10.2:8091", "10.10.10.2", 8091);
        List<RpcContext> candidates = Arrays.asList(c1, c2);

        ServerRoundRobinLoadBalance lb = new ServerRoundRobinLoadBalance();
        int c1Count = 0;
        int c2Count = 0;
        for (int i = 0; i < 100; i++) {
            RpcContext selected = lb.select(candidates);
            if (selected == c1) {
                c1Count++;
            } else {
                c2Count++;
            }
        }
        // RoundRobin should distribute evenly
        Assertions.assertEquals(50, c1Count);
        Assertions.assertEquals(50, c2Count);
    }

    @Test
    public void testLeastActiveLoadBalanceShouldSelectLeastActive() {
        RpcContext c1 = buildRpcContext("app:10.10.10.1:8091", "10.10.10.1", 8091);
        RpcContext c2 = buildRpcContext("app:10.10.10.2:8091", "10.10.10.2", 8091);
        // c1 has lower active count
        c1.incrementActiveCount();
        c2.incrementActiveCount();
        c2.incrementActiveCount();
        c2.incrementActiveCount();

        List<RpcContext> candidates = Arrays.asList(c1, c2);
        ServerLeastActiveLoadBalance lb = new ServerLeastActiveLoadBalance();
        RpcContext selected = lb.select(candidates);
        Assertions.assertEquals(c1, selected);
    }

    @Test
    public void testLeastActiveLoadBalanceWithSameActiveShouldRandomSelect() {
        RpcContext c1 = buildRpcContext("app:10.10.10.1:8091", "10.10.10.1", 8091);
        RpcContext c2 = buildRpcContext("app:10.10.10.2:8091", "10.10.10.2", 8091);
        List<RpcContext> candidates = Arrays.asList(c1, c2);

        ServerLeastActiveLoadBalance lb = new ServerLeastActiveLoadBalance();
        // Both have same active count (0), should randomly select
        for (int i = 0; i < 100; i++) {
            RpcContext selected = lb.select(candidates);
            Assertions.assertNotNull(selected);
            Assertions.assertTrue(candidates.contains(selected));
        }
    }

    @Test
    public void testSingleCandidateShouldReturnItDirectly() {
        RpcContext c1 = buildRpcContext("app:10.10.10.1:8091", "10.10.10.1", 8091);
        List<RpcContext> candidates = Arrays.asList(c1);

        ServerRandomLoadBalance randomLb = new ServerRandomLoadBalance();
        Assertions.assertEquals(c1, randomLb.select(candidates));

        ServerRoundRobinLoadBalance roundRobinLb = new ServerRoundRobinLoadBalance();
        Assertions.assertEquals(c1, roundRobinLb.select(candidates));

        ServerLeastActiveLoadBalance leastActiveLb = new ServerLeastActiveLoadBalance();
        Assertions.assertEquals(c1, leastActiveLb.select(candidates));
    }

    private RpcContext buildRpcContext(String clientId, String ip, int port) {
        Channel channel = mock(Channel.class);
        when(channel.remoteAddress()).thenReturn(new InetSocketAddress(ip, port));
        when(channel.isActive()).thenReturn(true);

        RpcContext rpcContext = new RpcContext();
        rpcContext.setClientId(clientId);
        rpcContext.setChannel(channel);
        return rpcContext;
    }
}
