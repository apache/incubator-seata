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

import org.apache.seata.core.model.BranchType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test for ServerLoadBalanceFactory.
 */
public class ServerLoadBalanceFactoryTest {

    @AfterEach
    public void cleanUp() {
        System.clearProperty("seata.server.loadBalance.at.enabled");
        System.clearProperty("seata.server.loadBalance.at.type");
        System.clearProperty("seata.server.loadBalance.tcc.enabled");
        System.clearProperty("seata.server.loadBalance.tcc.type");
    }

    @Test
    public void testAtDefaultEnabledAndUseRoundRobinType() {
        ServerLoadBalance loadBalance = ServerLoadBalanceFactory.getInstance(BranchType.AT);
        Assertions.assertNotNull(loadBalance);
        Assertions.assertTrue(ServerLoadBalanceFactory.isEnabled(BranchType.AT));
        Assertions.assertTrue(loadBalance instanceof ServerRoundRobinLoadBalance);
    }

    @Test
    public void testTccNeedExplicitTypeWhenEnabled() {
        System.setProperty("seata.server.loadBalance.tcc.enabled", "true");
        ServerLoadBalance loadBalance = ServerLoadBalanceFactory.getInstance(BranchType.TCC);
        Assertions.assertNull(loadBalance);
    }

    @Test
    public void testTccEnabledWithType() {
        System.setProperty("seata.server.loadBalance.tcc.enabled", "true");
        System.setProperty("seata.server.loadBalance.tcc.type", "RandomLoadBalance");
        ServerLoadBalance loadBalance = ServerLoadBalanceFactory.getInstance(BranchType.TCC);
        Assertions.assertNotNull(loadBalance);
        Assertions.assertTrue(loadBalance instanceof ServerRandomLoadBalance);
    }
}
