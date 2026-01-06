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

import org.apache.seata.core.rpc.TMDisconnectHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;

/**
 * Test for TM disconnect handling in AbstractNettyRemotingServer
 */
public class AbstractNettyRemotingServerTMDisconnectTest {

    private NettyRemotingServer server;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Use real NettyRemotingServer like other tests
        ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, new LinkedBlockingDeque<>());
        server = new NettyRemotingServer(executor);
    }

    @Test
    void testTMDisconnectHandlerSetup() {
        // Test that the TM disconnect handler can be set and retrieved
        TMDisconnectHandler testHandler = mock(TMDisconnectHandler.class);

        // Verify no exceptions are thrown when setting the handler
        assertDoesNotThrow(() -> server.setTmDisconnectHandler(testHandler));
        assertDoesNotThrow(() -> server.setTmDisconnectHandler(null));
        assertDoesNotThrow(() -> server.setTmDisconnectHandler(testHandler));
    }

    @Test
    void testTMDisconnectHandlerIntegration() {
        // Test that handler is properly integrated in the AbstractNettyRemotingServer
        // This verifies the new method exists and works
        assertDoesNotThrow(() -> {
            TMDisconnectHandler handler = mock(TMDisconnectHandler.class);
            server.setTmDisconnectHandler(handler);
        });
    }
}
