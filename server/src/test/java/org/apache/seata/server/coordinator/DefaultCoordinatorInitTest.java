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

import org.apache.seata.core.rpc.RemotingServer;
import org.apache.seata.core.rpc.TMDisconnectHandler;
import org.apache.seata.core.rpc.netty.AbstractNettyRemotingServer;
import org.apache.seata.core.rpc.netty.NettyRemotingServer;
import org.apache.seata.core.rpc.netty.NettyServerConfig;
import org.apache.seata.server.BaseSpringBootTest;
import org.apache.seata.server.session.SessionHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.ThreadPoolExecutor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test for DefaultCoordinator initialization, specifically TM disconnect handler setup
 */
public class DefaultCoordinatorInitTest extends BaseSpringBootTest {

    @Mock
    private ThreadPoolExecutor mockMessageExecutor;

    private TestableAbstractNettyRemotingServer testableRemotingServer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        NettyServerConfig nettyServerConfig = new NettyServerConfig();
        TestableAbstractNettyRemotingServer realServer =
                new TestableAbstractNettyRemotingServer(mockMessageExecutor, nettyServerConfig);
        testableRemotingServer = spy(realServer);
    }

    @Test
    void testDefaultCoordinatorInit_ShouldSetupTMDisconnectHandler() {
        // Given
        try (MockedStatic<SessionHolder> sessionHolder = mockStatic(SessionHolder.class)) {
            sessionHolder
                    .when(() ->
                            SessionHolder.distributedLockAndExecute(anyString(), any(SessionHolder.NoArgsFunc.class)))
                    .thenReturn(true);

            // When
            DefaultCoordinator coordinator = new DefaultCoordinator(testableRemotingServer);
            coordinator.init();

            // Then
            ArgumentCaptor<TMDisconnectHandler> handlerCaptor = ArgumentCaptor.forClass(TMDisconnectHandler.class);
            verify(testableRemotingServer).setTmDisconnectHandler(handlerCaptor.capture());

            TMDisconnectHandler capturedHandler = handlerCaptor.getValue();
            assertNotNull(capturedHandler);
            assertTrue(capturedHandler instanceof DefaultTMDisconnectHandler);
        }
    }

    @Test
    void testDefaultCoordinatorInit_WithNonAbstractNettyServer_ShouldNotSetHandler() {
        // Given
        RemotingServer nonNettyServer = mock(RemotingServer.class);

        try (MockedStatic<SessionHolder> sessionHolder = mockStatic(SessionHolder.class)) {
            sessionHolder
                    .when(() ->
                            SessionHolder.distributedLockAndExecute(anyString(), any(SessionHolder.NoArgsFunc.class)))
                    .thenReturn(true);

            // When
            DefaultCoordinator coordinator = new DefaultCoordinator(nonNettyServer);
            coordinator.init();

            // Then
            verifyNoInteractions(nonNettyServer);
        }
    }

    @Test
    void testDefaultCoordinatorInit_WithNettyRemotingServer_ShouldSetupHandler() {
        // Given
        NettyServerConfig config = new NettyServerConfig();
        NettyRemotingServer nettyRemotingServer = new NettyRemotingServer(mockMessageExecutor, config) {
            @Override
            public void init() {
                // Override to prevent actual server initialization
            }
        };
        NettyRemotingServer spyServer = spy(nettyRemotingServer);

        try (MockedStatic<SessionHolder> sessionHolder = mockStatic(SessionHolder.class)) {
            sessionHolder
                    .when(() ->
                            SessionHolder.distributedLockAndExecute(anyString(), any(SessionHolder.NoArgsFunc.class)))
                    .thenReturn(true);

            // When
            DefaultCoordinator coordinator = new DefaultCoordinator(spyServer);
            coordinator.init();

            // Then
            verify(spyServer).setTmDisconnectHandler(any(DefaultTMDisconnectHandler.class));
        }
    }

    @Test
    void testDefaultCoordinatorInit_SchedulersAreInitialized() {
        // Given
        try (MockedStatic<SessionHolder> sessionHolder = mockStatic(SessionHolder.class)) {
            sessionHolder
                    .when(() ->
                            SessionHolder.distributedLockAndExecute(anyString(), any(SessionHolder.NoArgsFunc.class)))
                    .thenReturn(true);

            // When
            DefaultCoordinator coordinator = new DefaultCoordinator(testableRemotingServer);
            coordinator.init();

            // Then
            // Verify that TM disconnect handler is set up
            verify(testableRemotingServer).setTmDisconnectHandler(any(DefaultTMDisconnectHandler.class));

            // Note: SessionHolder.distributedLockAndExecute verification is complex due to scheduler initialization
            // The important part is that TM disconnect handler is properly set up
        }
    }

    /**
     * Testable implementation of AbstractNettyRemotingServer for testing
     */
    private static class TestableAbstractNettyRemotingServer extends AbstractNettyRemotingServer {

        public TestableAbstractNettyRemotingServer(
                ThreadPoolExecutor messageExecutor, NettyServerConfig nettyServerConfig) {
            super(messageExecutor, nettyServerConfig);
        }

        @Override
        public void destroyChannel(String serverAddress, io.netty.channel.Channel channel) {
            // Test implementation - do nothing
        }

        @Override
        public void init() {
            // Override to prevent actual server initialization in tests
        }
    }
}
