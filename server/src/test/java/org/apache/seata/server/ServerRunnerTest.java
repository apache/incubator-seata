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
package org.apache.seata.server;

import java.lang.reflect.Field;

import org.apache.seata.core.rpc.Disposable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.boot.web.server.WebServer;
import org.springframework.core.Ordered;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ServerRunner Test
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ServerRunner Test")
class ServerRunnerTest {

    private ServerRunner serverRunner;

    @Mock
    private Server mockServer;

    @Mock
    private WebServerInitializedEvent mockWebServerInitializedEvent;

    @Mock
    private WebServer mockWebServer;

    @BeforeEach
    void setUp() throws Exception {
        serverRunner = new ServerRunner();
        // Use reflection to set private field
        Field field = ServerRunner.class.getDeclaredField("seataServer");
        field.setAccessible(true);
        field.set(serverRunner, mockServer);
    }

    @Test
    @DisplayName("test run success")
    void testRunSuccess() throws Exception {
        String[] args = new String[] {"arg1", "arg2"};

        doNothing().when(mockServer).start(args);

        serverRunner.run(args);

        assertTrue(serverRunner.started());
        verify(mockServer).start(args);
    }

    @Test
    @DisplayName("test destroy with disposables")
    void testDestroyWithDisposables() throws Exception {
        Disposable disposable1 = mock(Disposable.class);
        Disposable disposable2 = mock(Disposable.class);

        ServerRunner.addDisposable(disposable1);
        ServerRunner.addDisposable(disposable2);

        serverRunner.destroy();

        verify(disposable1).destroy();
        verify(disposable2).destroy();
    }

    @Test
    @DisplayName("test destroy without disposables")
    void testDestroyWithoutDisposables() throws Exception {
        serverRunner.destroy();
        // Should not throw exception
    }

    @Test
    @DisplayName("test onApplicationEvent with WebServerInitializedEvent")
    void testOnApplicationEventWithWebServerInitializedEvent() {
        when(mockWebServerInitializedEvent.getWebServer()).thenReturn(mockWebServer);
        when(mockWebServer.getPort()).thenReturn(8080);

        serverRunner.onApplicationEvent(mockWebServerInitializedEvent);

        // Port should be captured
        verify(mockWebServer).getPort();
    }

    @Test
    @DisplayName("test onApplicationEvent with other event")
    void testOnApplicationEventWithOtherEvent() {
        org.springframework.context.event.ContextRefreshedEvent otherEvent = mock(
            org.springframework.context.event.ContextRefreshedEvent.class);

        serverRunner.onApplicationEvent(otherEvent);

        // Should not throw exception
    }

    @Test
    @DisplayName("test getOrder")
    void testGetOrder() {
        int order = serverRunner.getOrder();
        assertEquals(Ordered.LOWEST_PRECEDENCE, order);
    }

    @Test
    @DisplayName("test started initial state")
    void testStartedInitialState() {
        ServerRunner newRunner = new ServerRunner();
        assertFalse(newRunner.started());
    }

    @Test
    @DisplayName("test add disposable")
    void testAddDisposable() throws Exception {
        Disposable disposable = mock(Disposable.class);

        ServerRunner.addDisposable(disposable);
        serverRunner.destroy();

        verify(disposable).destroy();
    }
}

