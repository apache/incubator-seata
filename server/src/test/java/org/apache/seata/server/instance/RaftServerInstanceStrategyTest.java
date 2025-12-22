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
package org.apache.seata.server.instance;

import com.alipay.sofa.jraft.entity.PeerId;
import org.apache.seata.common.XID;
import org.apache.seata.common.holder.ObjectHolder;
import org.apache.seata.common.metadata.ClusterRole;
import org.apache.seata.common.metadata.Instance;
import org.apache.seata.common.metadata.Node;
import org.apache.seata.common.store.SessionMode;
import org.apache.seata.server.BaseSpringBootTest;
import org.apache.seata.server.cluster.listener.ClusterChangeEvent;
import org.apache.seata.server.cluster.raft.RaftServer;
import org.apache.seata.server.cluster.raft.RaftServerManager;
import org.apache.seata.server.cluster.raft.RaftStateMachine;
import org.apache.seata.server.session.SessionHolder;
import org.apache.seata.server.store.StoreConfig;
import org.apache.seata.server.store.VGroupMappingStoreManager;
import org.apache.seata.spring.boot.autoconfigure.properties.registry.RegistryNamingServerProperties;
import org.apache.seata.spring.boot.autoconfigure.properties.server.raft.ServerRaftProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.apache.seata.common.Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RaftServerInstanceStrategyTest extends BaseSpringBootTest {

    private RaftServerInstanceStrategy strategy;

    private ServerRaftProperties raftProperties;

    private RegistryNamingServerProperties namingProps;

    private ServerProperties serverProperties;

    @BeforeEach
    void setUp() {
        strategy = new RaftServerInstanceStrategy();
        raftProperties = new ServerRaftProperties();
        namingProps = new RegistryNamingServerProperties();
        serverProperties = new ServerProperties();
        // set minimal required fields
        raftProperties.setGroup("groupA");
        namingProps.setNamespace("ns");
        namingProps.setCluster("clusterA");
        serverProperties.setPort(8088);
        strategy.raftProperties = raftProperties;
        strategy.registryNamingServerProperties = namingProps;
        strategy.serverProperties = serverProperties;
    }

    @AfterEach
    void tearDown() {
        resetInstance();
        // Do not put null into ObjectHolder to avoid ConcurrentHashMap NPE
    }

    @Test
    void serverInstanceInit_shouldPopulateInstanceFromRaft() {
        ConfigurableEnvironment environment = buildEnvironmentWithMeta();
        ObjectHolder.INSTANCE.setObject(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT, environment);

        RaftStateMachine stateMachine = mock(RaftStateMachine.class);
        when(stateMachine.getCurrentTerm()).thenReturn(new AtomicLong(5L));
        when(stateMachine.isLeader()).thenReturn(true);

        PeerId peerId = new PeerId("127.0.0.1", 9090);
        RaftServer raftServer = mock(RaftServer.class);
        when(raftServer.getRaftStateMachine()).thenReturn(stateMachine);
        when(raftServer.getServerId()).thenReturn(peerId);

        try (MockedStatic<RaftServerManager> raftServerManagerMock = Mockito.mockStatic(RaftServerManager.class);
                MockedStatic<StoreConfig> storeConfigMock = Mockito.mockStatic(StoreConfig.class);
                MockedStatic<XID> xidMock = Mockito.mockStatic(XID.class)) {
            raftServerManagerMock
                    .when(() -> RaftServerManager.getRaftServer("groupA"))
                    .thenReturn(raftServer);
            storeConfigMock.when(StoreConfig::getSessionMode).thenReturn(SessionMode.RAFT);
            xidMock.when(XID::getIpAddress).thenReturn("10.0.0.1");

            Instance instance = strategy.serverInstanceInit();

            assertEquals("ns", instance.getNamespace());
            assertEquals("clusterA", instance.getClusterName());
            assertEquals("groupA", instance.getUnit());
            assertEquals(5L, instance.getTerm());
            assertEquals(ClusterRole.LEADER, instance.getRole());
            Node.Endpoint control = instance.getControl();
            assertNotNull(control);
            assertEquals("10.0.0.1", control.getHost());
            assertEquals(8088, control.getPort());
            Node.Endpoint internal = instance.getInternal();
            assertNotNull(internal);
            assertEquals("127.0.0.1", internal.getHost());
            assertEquals(9090, internal.getPort());
            assertEquals("RAFT", instance.getMetadata().get("cluster-type"));
        }
    }

    @Test
    void onChangeEvent_shouldUpdateTermRoleAndNotify() {
        resetInstance();
        Instance instance = Instance.getInstance();
        instance.setRole(ClusterRole.FOLLOWER);
        instance.setTerm(1L);

        ClusterChangeEvent event = new ClusterChangeEvent(this, "groupA", 12L, true);

        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            VGroupMappingStoreManager mappingManager = mock(VGroupMappingStoreManager.class);
            sessionHolderMock.when(SessionHolder::getRootVGroupMappingManager).thenReturn(mappingManager);

            strategy.onChangeEvent(event);

            assertEquals(12L, instance.getTerm());
            assertEquals(ClusterRole.LEADER, instance.getRole());
            verify(mappingManager, times(1)).notifyMapping();
        }
    }

    @Test
    void typeAndOrderShouldReturnExpectedValues() {
        assertEquals(SeataInstanceStrategy.Type.RAFT, strategy.type());
        assertEquals(Integer.MAX_VALUE - 1, strategy.getOrder());
    }

    private ConfigurableEnvironment buildEnvironmentWithMeta() {
        Map<String, Object> map = new HashMap<>();
        map.put("meta.demo", "v");
        StandardEnvironment environment = new StandardEnvironment();
        MutablePropertySources sources = environment.getPropertySources();
        sources.addFirst(new MapPropertySource("testMeta", map));
        return environment;
    }

    private void resetInstance() {
        Instance instance = Instance.getInstance();
        instance.setNamespace(null);
        instance.setClusterName(null);
        instance.setUnit(null);
        instance.setControl(null);
        instance.setTransaction(null);
        instance.setInternal(null);
        instance.setHealthy(true);
        instance.setWeight(1.0);
        instance.setTerm(0L);
        instance.setTimestamp(0L);
        instance.setMetadata(new HashMap<>());
        instance.setRole(ClusterRole.MEMBER);
        instance.setVersion(null);
    }
}
