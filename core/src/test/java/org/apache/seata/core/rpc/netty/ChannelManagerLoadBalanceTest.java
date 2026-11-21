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
package org.apache.seata.core.rpc.netty;

import io.netty.channel.Channel;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.core.protocol.RegisterRMRequest;
import org.apache.seata.core.rpc.RpcContext;
import org.apache.seata.core.rpc.netty.loadbalance.ServerLoadBalanceFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class ChannelManagerLoadBalanceTest {
    private static final String RESOURCE = "lb-test-resource";
    private final List<Channel> channels = new ArrayList<>();
    private Configuration configuration;
    private MockedStatic<ConfigurationFactory> factory;

    @BeforeEach
    void setUp() {
        configuration = mock(Configuration.class);
        factory = mockStatic(ConfigurationFactory.class);
        factory.when(ConfigurationFactory::getInstance).thenReturn(configuration);
        when(configuration.getConfig(ServerLoadBalanceFactory.SERVER_LB_AT_TYPE))
                .thenReturn("RoundRobinLoadBalance");
        when(configuration.getConfig(ServerLoadBalanceFactory.SERVER_LB_TCC_TYPE))
                .thenReturn("RoundRobinLoadBalance");
    }

    @AfterEach
    void tearDown() {
        channels.forEach(channel -> ChannelManager.unregisterRMChannel(channel, Collections.singleton(RESOURCE)));
        factory.close();
    }

    private Channel register(String app, String ip, int port) throws Exception {
        Channel channel = mock(Channel.class);
        when(channel.remoteAddress()).thenReturn(new InetSocketAddress(ip, port));
        when(channel.isActive()).thenReturn(true);
        RegisterRMRequest request = new RegisterRMRequest();
        request.setApplicationId(app);
        request.setTransactionServiceGroup("lb-test-group");
        request.setVersion("2.0.0");
        request.setResourceIds(RESOURCE);
        ChannelManager.registerRMChannel(request, channel);
        channels.add(channel);
        return channel;
    }

    @Test
    void roundRobinAcrossRealLookupsAndApplicationBoundary() throws Exception {
        Channel first = register("app", "127.0.0.1", 18001);
        Channel second = register("app", "127.0.0.2", 18002);
        Channel otherApp = register("other", "127.0.0.3", 18003);
        String clientId = ChannelManager.getContextFromIdentified(first).getClientId();
        int firstCount = 0;
        int secondCount = 0;
        for (int i = 0; i < 101; i++) {
            Channel selected = ChannelManager.getChannel(RESOURCE, clientId, false, BranchType.TCC);
            assertTrue(selected == first || selected == second);
            if (selected == first) {
                firstCount++;
            } else {
                secondCount++;
            }
        }
        assertTrue(Math.abs(firstCount - secondCount) <= 1);
        Set<Channel> selected = new HashSet<>();
        for (int i = 0; i < 3; i++) {
            selected.add(ChannelManager.getChannel(RESOURCE, clientId, true, BranchType.AT));
        }
        assertEquals(new HashSet<>(channels), selected);
        assertTrue(selected.contains(otherApp));
    }

    @Test
    void inactiveChannelsAreRemovedAndEmptyResourcesAreSkipped() throws Exception {
        Channel inactive = register("app", "127.0.0.1", 18001);
        Channel active = register("app", "127.0.0.1", 18002);
        RpcContext inactiveContext = ChannelManager.getContextFromIdentified(inactive);
        String clientId = inactiveContext.getClientId();
        when(inactive.isActive()).thenReturn(false);
        assertSame(active, ChannelManager.getChannel(RESOURCE, clientId, false, BranchType.AT));
        assertFalse(inactiveContext.getClientRMHolderMap().get(RESOURCE).containsValue(inactiveContext));
        when(active.isActive()).thenReturn(false);
        assertNull(ChannelManager.getChannel(RESOURCE, clientId, true, BranchType.AT));
        assertFalse(ChannelManager.getRmChannels(BranchType.AT).containsKey(RESOURCE));
        assertNull(ChannelManager.getChannel("missing-resource", clientId, true, BranchType.AT));
    }

    @Test
    void excludedModesAndDisabledConfigurationKeepOriginalChannel() throws Exception {
        Channel first = register("app", "127.0.0.1", 18001);
        register("app", "127.0.0.2", 18002);
        String clientId = ChannelManager.getContextFromIdentified(first).getClientId();
        for (int i = 0; i < 5; i++) {
            assertSame(first, ChannelManager.getChannel(RESOURCE, clientId, true, BranchType.XA));
            assertSame(first, ChannelManager.getChannel(RESOURCE, clientId, true, BranchType.SAGA));
            assertSame(first, ChannelManager.getChannel(RESOURCE, clientId, true));
        }
        for (String value : new String[] {null, " ", "invalid-strategy"}) {
            when(configuration.getConfig(ServerLoadBalanceFactory.SERVER_LB_AT_TYPE))
                    .thenReturn(value);
            assertSame(first, ChannelManager.getChannel(RESOURCE, clientId, true, BranchType.AT));
        }
    }

    @Test
    void missingApplicationRequiresExplicitCrossApplicationPermission() throws Exception {
        Channel channel = register("other", "127.0.0.1", 18001);
        assertNull(ChannelManager.getChannel(RESOURCE, "app:127.0.0.2:18002", false, BranchType.TCC));
        assertSame(channel, ChannelManager.getChannel(RESOURCE, "app:127.0.0.2:18002", true, BranchType.AT));
    }

    @Test
    void representativeSelectionBalancesAtButPreservesSagaAndLegacyBehavior() throws Exception {
        register("app", "127.0.0.1", 18001);
        register("app", "127.0.0.2", 18002);
        Channel legacy = ChannelManager.getRmChannels().get(RESOURCE);
        assertSame(legacy, ChannelManager.getRmChannels(BranchType.SAGA).get(RESOURCE));
        Channel first = ChannelManager.getRmChannels(BranchType.AT).get(RESOURCE);
        Channel second = ChannelManager.getRmChannels(BranchType.AT).get(RESOURCE);
        assertNotSame(first, second);
    }

    @Test
    void leastActiveUsesConnectionLoad() throws Exception {
        Channel busy = register("app", "127.0.0.1", 18001);
        Channel idle = register("app", "127.0.0.2", 18002);
        RpcContext context = ChannelManager.getContextFromIdentified(busy);
        context.incrementActiveCount();
        when(configuration.getConfig(ServerLoadBalanceFactory.SERVER_LB_AT_TYPE))
                .thenReturn("LeastActiveLoadBalance");
        assertSame(idle, ChannelManager.getChannel(RESOURCE, context.getClientId(), false, BranchType.AT));
        context.decrementActiveCount();
    }
}
