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

import org.apache.seata.common.store.SessionMode;
import org.apache.seata.core.rpc.RemotingServer;
import org.apache.seata.server.BaseSpringBootTest;
import org.apache.seata.server.cluster.listener.ClusterChangeEvent;
import org.apache.seata.server.session.SessionHolder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;

/**
 * The type Raft coordinator test.
 */
public class RaftCoordinatorTest extends BaseSpringBootTest {

    private static RaftCoordinator raftCoordinator;
    private static RemotingServer remotingServer;

    private static final String TEST_GROUP = "test_group";
    private static final String ANOTHER_GROUP = "another_group";

    @BeforeAll
    public static void setup(ApplicationContext context) throws Exception {
        SessionHolder.init(SessionMode.FILE);
        remotingServer = new DefaultCoordinatorTest.MockServerMessageSender();
        raftCoordinator = new RaftCoordinator(remotingServer);
    }

    @AfterAll
    public static void cleanup() {
        SessionHolder.destroy();
    }

    @AfterEach
    public void clearGroupPrevent() {
        RaftCoordinator.GROUP_PREVENT.clear();
    }

    @Test
    public void constructorSuccessTest() {
        RaftCoordinator coordinator = new RaftCoordinator(remotingServer);
        Assertions.assertNotNull(coordinator);
    }

    @Test
    public void setPreventRaftModeTest() {
        try (MockedStatic<org.apache.seata.server.store.StoreConfig> mockedStoreConfig =
                Mockito.mockStatic(org.apache.seata.server.store.StoreConfig.class)) {

            mockedStoreConfig
                    .when(org.apache.seata.server.store.StoreConfig::getSessionMode)
                    .thenReturn(SessionMode.RAFT);

            RaftCoordinator.setPrevent(TEST_GROUP, true);
            Assertions.assertTrue(RaftCoordinator.GROUP_PREVENT.get(TEST_GROUP));

            RaftCoordinator.setPrevent(TEST_GROUP, false);
            Assertions.assertFalse(RaftCoordinator.GROUP_PREVENT.get(TEST_GROUP));
        }
    }

    @Test
    public void setPreventNonRaftModeTest() {
        try (MockedStatic<org.apache.seata.server.store.StoreConfig> mockedStoreConfig =
                Mockito.mockStatic(org.apache.seata.server.store.StoreConfig.class)) {

            mockedStoreConfig
                    .when(org.apache.seata.server.store.StoreConfig::getSessionMode)
                    .thenReturn(SessionMode.FILE);

            RaftCoordinator.setPrevent(TEST_GROUP, true);
            Assertions.assertNull(RaftCoordinator.GROUP_PREVENT.get(TEST_GROUP));
        }
    }

    @Test
    public void onApplicationEventLeaderChangeTest() {
        try (MockedStatic<org.apache.seata.server.store.StoreConfig> mockedStoreConfig =
                Mockito.mockStatic(org.apache.seata.server.store.StoreConfig.class)) {

            mockedStoreConfig
                    .when(org.apache.seata.server.store.StoreConfig::getSessionMode)
                    .thenReturn(SessionMode.RAFT);

            ClusterChangeEvent event = new ClusterChangeEvent(this, TEST_GROUP, 1L, true);
            raftCoordinator.onApplicationEvent(event);

            Assertions.assertTrue(RaftCoordinator.GROUP_PREVENT.get(TEST_GROUP));
        }
    }

    @Test
    public void onApplicationEventFollowerChangeTest() {
        try (MockedStatic<org.apache.seata.server.store.StoreConfig> mockedStoreConfig =
                Mockito.mockStatic(org.apache.seata.server.store.StoreConfig.class)) {

            mockedStoreConfig
                    .when(org.apache.seata.server.store.StoreConfig::getSessionMode)
                    .thenReturn(SessionMode.RAFT);

            ClusterChangeEvent event = new ClusterChangeEvent(this, TEST_GROUP, 1L, false);
            raftCoordinator.onApplicationEvent(event);

            Assertions.assertFalse(RaftCoordinator.GROUP_PREVENT.get(TEST_GROUP));
        }
    }

    @Test
    public void isPassMultipleGroupsTest() {
        try (MockedStatic<org.apache.seata.server.store.StoreConfig> mockedStoreConfig =
                Mockito.mockStatic(org.apache.seata.server.store.StoreConfig.class)) {

            mockedStoreConfig
                    .when(org.apache.seata.server.store.StoreConfig::getSessionMode)
                    .thenReturn(SessionMode.RAFT);

            RaftCoordinator.setPrevent(TEST_GROUP, true);
            RaftCoordinator.setPrevent(ANOTHER_GROUP, false);

            Assertions.assertTrue(RaftCoordinator.GROUP_PREVENT.get(TEST_GROUP));
            Assertions.assertFalse(RaftCoordinator.GROUP_PREVENT.get(ANOTHER_GROUP));
        }
    }
}
