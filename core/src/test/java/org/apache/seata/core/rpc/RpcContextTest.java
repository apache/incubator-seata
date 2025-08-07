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
package org.apache.seata.core.rpc;

import io.netty.channel.Channel;
import org.apache.seata.core.rpc.netty.NettyPoolKey;
import org.apache.seata.core.rpc.netty.NettyPoolKey.TransactionRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RpcContext Test
 *
 *
 *
 */
public class RpcContextTest {
    /** RpcContext constructor parameter set as constant **/
    private static RpcContext rpcContext;
    /** Version value **/
    private static final String VERSION = "a";
    /** TransactionServiceGroup value **/
    private static final String TSG = "a";
    /** ID value for every method needing an Id **/
    private static final String ID = "1";
    /** ResourceValue value **/
    private static final String RV = "abc";
    /** ResourceSet value **/
    private static final String RS = "b";

    /**
     * RpcContext Constructor
     */
    @BeforeEach
    public void setup() {
        rpcContext = new RpcContext();
    }

    /**
     * Test set ApplicationId to value = "1" Test get ApplicationId
     */
    @Test
    public void testApplicationIdValue() {
        rpcContext.setApplicationId(ID);
        assertEquals(ID, rpcContext.getApplicationId());
    }

    /**
     * Test set Version to value = "a" Test get Version
     */
    @Test
    public void testVersionValue() {
        rpcContext.setVersion(VERSION);
        assertEquals(VERSION, rpcContext.getVersion());
    }

    /**
     * Test set ClientId to value = "1" Test get ClientId
     */
    @Test
    public void testClientIdValue() {
        rpcContext.setClientId(ID);
        assertEquals(ID, rpcContext.getClientId());
    }

    @Test
    void testSetAndGetTransactionServiceGroup() {
        String serviceGroup = "testGroup";
        rpcContext.setTransactionServiceGroup(serviceGroup);
        assertEquals(serviceGroup, rpcContext.getTransactionServiceGroup(), "Transaction service group should match");
    }

    /**
     * Test set Channel to null Test get Channel
     */
    @Test
    public void testChannelNull() {
        rpcContext.setChannel(null);
        assertNull(rpcContext.getChannel());
    }

    /**
     * Test set TransactionServiceGroup to value = "1" Test get
     * TransactionServiceGroup
     */
    @Test
    public void testTransactionServiceGroupValue() {
        rpcContext.setTransactionServiceGroup(TSG);
        assertEquals(TSG, rpcContext.getTransactionServiceGroup());
    }

    @Test
    void testSetAndGetChannel() {
        Channel mockChannel = Mockito.mock(Channel.class);
        rpcContext.setChannel(mockChannel);
        assertSame(mockChannel, rpcContext.getChannel(), "Channel should match");
    }

    @Test
    void testSetAndGetClientRole() {
        NettyPoolKey.TransactionRole role = NettyPoolKey.TransactionRole.TMROLE;
        rpcContext.setClientRole(role);
        assertEquals(role, rpcContext.getClientRole(), "Client role should match");
    }

    @Test
    void testAddResource() {
        String resource = "db1";
        rpcContext.addResource(resource);
        Set<String> resources = rpcContext.getResourceSets();
        assertNotNull(resources, "Resource set should not be null");
        assertTrue(resources.contains(resource), "Resource should be added");
    }

    /**
     * Test setClientRole to null Test getApplication Id
     */
    @Test
    public void testClientRoleNull() {
        rpcContext.setClientRole(null);
        assertNull(rpcContext.getClientRole());
    }

    /**
     * Test set ResourceSets to null Test get ResourceSets
     */
    @Test
    public void testResourceSetsNull() {
        rpcContext.setResourceSets(null);
        assertNull(rpcContext.getResourceSets());
    }

    /**
     * Test add resourceSet = null with addResource Test get ResourceSets
     */
    @Test
    public void testAddResourceNull() {
        HashSet<String> resourceSet = new HashSet<String>();
        rpcContext.setResourceSets(resourceSet);
        rpcContext.addResource(null);
        assertEquals(0, rpcContext.getResourceSets().size());
    }

    /**
     * Test add null parameter to ResourceSets with addResources Test get
     * ResourceSets
     */
    @Test
    public void testAddResourcesNull() {
        rpcContext.addResources(null);
        rpcContext.setResourceSets(null);
        assertNull(rpcContext.getResourceSets());
    }

    /**
     * Test add a short resourceSet(["abc"]) with addResources Test get ResourceSets
     */
    @Test
    public void testAddResourcesResourceValue() {
        HashSet<String> resourceSet = new HashSet<String>();
        resourceSet.add(RV);
        rpcContext.addResources(resourceSet);
        assertEquals(resourceSet, rpcContext.getResourceSets());
    }

    /**
     * Test add resource and resource sets to ResourceSets with addResourceSets Test
     * getResourceSets
     */
    @Test
    public void testAddResourcesResourceSetValue() {
        HashSet<String> resourceSets = new HashSet<String>();
        resourceSets.add(RS);
        HashSet<String> resourceSet = new HashSet<String>();
        resourceSet.add(RV);
        rpcContext.addResources(resourceSet);
        rpcContext.setResourceSets(resourceSets);
        rpcContext.addResources(resourceSet);
        assertEquals(resourceSets, rpcContext.getResourceSets());
    }

    /**
     * Test toString having all the parameters initialized to null
     */
    @Test
    public void testToString() {
        rpcContext.setApplicationId(null);
        rpcContext.setTransactionServiceGroup(null);
        rpcContext.setClientId(null);
        rpcContext.setChannel(null);
        rpcContext.setResourceSets(null);
        assertEquals(
                "RpcContext{" + "applicationId='" + rpcContext.getApplicationId() + '\'' + ", transactionServiceGroup='"
                        + rpcContext.getTransactionServiceGroup() + '\'' + ", clientId='" + rpcContext.getClientId()
                        + '\''
                        + ", channel=" + rpcContext.getChannel() + ", resourceSets=" + rpcContext.getResourceSets()
                        + '}',
                rpcContext.toString());
    }

    @Test
    void testHoldInIdentifiedChannels() {
        ConcurrentMap<Channel, RpcContext> clientIDHolderMap = new ConcurrentHashMap<>();
        Channel mockChannel = Mockito.mock(Channel.class);
        rpcContext.setChannel(mockChannel);

        rpcContext.holdInIdentifiedChannels(clientIDHolderMap);
        assertSame(rpcContext, clientIDHolderMap.get(mockChannel), "RpcContext should be held in the map");
    }

    @Test
    void testHoldInClientChannels() {
        ConcurrentMap<Integer, RpcContext> clientTMHolderMap = new ConcurrentHashMap<>();
        Channel mockChannel = Mockito.mock(Channel.class);
        rpcContext.setChannel(mockChannel);
        Mockito.when(mockChannel.remoteAddress()).thenReturn(new InetSocketAddress(8080));

        rpcContext.holdInClientChannels(clientTMHolderMap);
        Integer clientPort = 8080; // Assuming port is extracted from remote address
        assertSame(rpcContext, clientTMHolderMap.get(clientPort), "RpcContext should be held in the map");
    }

    @Test
    void testHoldInResourceManagerChannels() {
        String resourceId = "db1";
        Integer clientPort = 8080;

        rpcContext.holdInResourceManagerChannels(resourceId, clientPort);
        ConcurrentMap<String, ConcurrentMap<Integer, RpcContext>> clientRMHolderMap = rpcContext.getClientRMHolderMap();
        assertNotNull(clientRMHolderMap, "Client RM holder map should not be null");

        ConcurrentMap<Integer, RpcContext> portMap = clientRMHolderMap.get(resourceId);
        assertNotNull(portMap, "Port map should not be null");
        assertSame(rpcContext, portMap.get(clientPort), "RpcContext should be held in the map");
    }

    @Test
    void testRelease() {
        Channel mockChannel = Mockito.mock(Channel.class);
        rpcContext.setChannel(mockChannel);
        Mockito.when(mockChannel.remoteAddress()).thenReturn(new InetSocketAddress(8080));

        // Setup data
        rpcContext.setClientRole(TransactionRole.RMROLE);
        ConcurrentMap<Integer, RpcContext> clientTMHolderMap = new ConcurrentHashMap<>();
        rpcContext.holdInClientChannels(clientTMHolderMap);

        rpcContext.release();
        assertNull(rpcContext.getClientRMHolderMap(), "Client RM holder map should be cleared");
        assertNull(rpcContext.getResourceSets(), "Resource sets should be cleared");
    }
}
