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

import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.core.model.BranchType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test for ServerLoadBalanceFactory.
 */
public class ServerLoadBalanceFactoryTest {

    @Test
    public void testXaReturnsNull() {
        // XA does not support server-side load balancing
        ServerLoadBalance loadBalance = ServerLoadBalanceFactory.getInstance(BranchType.XA);
        Assertions.assertNull(loadBalance);
    }

    @Test
    public void testSagaReturnsNull() {
        // SAGA does not support server-side load balancing
        ServerLoadBalance loadBalance = ServerLoadBalanceFactory.getInstance(BranchType.SAGA);
        Assertions.assertNull(loadBalance);
    }

    @Test
    public void testAtNotConfiguredReturnsNull() {
        // When no type is configured, should return null (use original logic)
        ServerLoadBalance loadBalance = ServerLoadBalanceFactory.getInstance(BranchType.AT);
        Assertions.assertNull(loadBalance);
    }

    @Test
    public void testTccNotConfiguredReturnsNull() {
        ServerLoadBalance loadBalance = ServerLoadBalanceFactory.getInstance(BranchType.TCC);
        Assertions.assertNull(loadBalance);
    }

    @Test
    public void testSpiLoadRandomLoadBalance() {
        ServerLoadBalance loadBalance = EnhancedServiceLoader.load(ServerLoadBalance.class, "RandomLoadBalance");
        Assertions.assertNotNull(loadBalance);
        Assertions.assertTrue(loadBalance instanceof ServerRandomLoadBalance);
    }

    @Test
    public void testSpiLoadRoundRobinLoadBalance() {
        ServerLoadBalance loadBalance = EnhancedServiceLoader.load(ServerLoadBalance.class, "RoundRobinLoadBalance");
        Assertions.assertNotNull(loadBalance);
        Assertions.assertTrue(loadBalance instanceof ServerRoundRobinLoadBalance);
    }

    @Test
    public void testSpiLoadLeastActiveLoadBalance() {
        ServerLoadBalance loadBalance = EnhancedServiceLoader.load(ServerLoadBalance.class, "LeastActiveLoadBalance");
        Assertions.assertNotNull(loadBalance);
        Assertions.assertTrue(loadBalance instanceof ServerLeastActiveLoadBalance);
    }

    @Test
    public void testSpiLoadInvalidTypeThrowsException() {
        // Loading a non-existent LB type should throw EnhancedServiceNotFoundException
        Assertions.assertThrows(
                Exception.class, () -> EnhancedServiceLoader.load(ServerLoadBalance.class, "NonExistentLoadBalance"));
    }

    @Test
    public void testFactoryConfigKeyFormat() {
        // Verify the config key format matches the expected pattern
        Assertions.assertEquals("server.loadBalance.at.type", ServerLoadBalanceFactory.SERVER_LB_AT_TYPE);
        Assertions.assertEquals("server.loadBalance.tcc.type", ServerLoadBalanceFactory.SERVER_LB_TCC_TYPE);
    }
}
