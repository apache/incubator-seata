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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test for server load balance behavior.
 */
public class ServerLoadBalanceBehaviorTest {

    @Test
    public void testServerXidLoadBalanceShouldPreferExactAddressFromXid() {
        RpcContext target = buildRpcContext("app:10.10.10.10:8091", "10.10.10.10", 8091);
        RpcContext other = buildRpcContext("app:10.10.10.11:8091", "10.10.10.11", 8091);
        List<RpcContext> candidates = Arrays.asList(other, target);

        ServerXidLoadBalance loadBalance = new ServerXidLoadBalance();
        RpcContext selected = loadBalance.select(candidates, "10.10.10.10:8091:123456");

        Assertions.assertSame(target, selected);
    }

    @Test
    public void testConsistentHashFallbackKeyShouldBeOrderStable() {
        RpcContext c1 = buildRpcContext("app:10.10.10.1:8091", "10.10.10.1", 8091);
        RpcContext c2 = buildRpcContext("app:10.10.10.2:8091", "10.10.10.2", 8091);
        List<RpcContext> firstOrder = Arrays.asList(c1, c2);
        List<RpcContext> secondOrder = Arrays.asList(c2, c1);

        ServerConsistentHashLoadBalance lb1 = new ServerConsistentHashLoadBalance();
        ServerConsistentHashLoadBalance lb2 = new ServerConsistentHashLoadBalance();

        RpcContext result1 = lb1.select(firstOrder, null);
        RpcContext result2 = lb2.select(secondOrder, null);

        Assertions.assertEquals(result1.getClientId(), result2.getClientId());
    }

    @Test
    public void testConsistentHashShouldBeThreadSafeForConcurrentSelect() throws ExecutionException, InterruptedException {
        RpcContext c1 = buildRpcContext("app:10.10.10.1:8091", "10.10.10.1", 8091);
        RpcContext c2 = buildRpcContext("app:10.10.10.2:8091", "10.10.10.2", 8091);
        List<RpcContext> candidates = Collections.unmodifiableList(Arrays.asList(c1, c2));

        ServerConsistentHashLoadBalance loadBalance = new ServerConsistentHashLoadBalance();
        ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            List<CompletableFuture<String>> futures = new ArrayList<>(32);
            for (int i = 0; i < 32; i++) {
                futures.add(CompletableFuture.supplyAsync(
                        () -> loadBalance.select(candidates, "xid-test-1").getClientId(), executor));
            }
            String expected = futures.get(0).get();
            for (CompletableFuture<String> future : futures) {
                Assertions.assertEquals(expected, future.get());
            }
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(1, TimeUnit.SECONDS);
        }
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
