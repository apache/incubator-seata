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
package org.apache.seata.mcp.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpAsyncServer;
import org.apache.seata.mcp.entity.pojo.MCPProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for McpServerManager
 */
@ExtendWith(MockitoExtension.class)
class MCPServerManagerTest {

    @Mock
    private ObjectMapper objectMapper;

    private MCPServerManager mcpServerManager;
    private MCPProperties mcpProperties;

    @BeforeEach
    void setUp() {
        mcpProperties = createMockMCPProperties();
        mcpServerManager = new MCPServerManager(mcpProperties, objectMapper);
    }

    @Test
    void testConstructorWithSseType() {
        MCPProperties sseProperties = createSseMCPProperties();
        MCPServerManager sseManager = new MCPServerManager(sseProperties, objectMapper);

        assertNotNull(sseManager);
        assertEquals(sseProperties, sseManager.getConfig());
        assertNotNull(sseManager.getStateLock());
        assertNotNull(sseManager.getPoolLock());
        assertNotNull(sseManager.getRunning());
        assertFalse(sseManager.getRunning().get());
    }

    @Test
    void testConstructorWithStreamableType() {
        MCPProperties streamableProperties = createStreamableMCPProperties();
        MCPServerManager streamableManager = new MCPServerManager(streamableProperties, objectMapper);

        assertNotNull(streamableManager);
        assertEquals(streamableProperties, streamableManager.getConfig());
        assertFalse(streamableManager.getRunning().get());
    }

    @Test
    void testStartStopWithSseType() {
        MCPProperties sseProperties = createSseMCPProperties();
        MCPServerManager sseManager = new MCPServerManager(sseProperties, objectMapper);

        assertFalse(sseManager.isRunning());
        sseManager.start();
        assertTrue(sseManager.isRunning());
        assertNotNull(sseManager.getServerInstance());

        sseManager.stop();
        assertFalse(sseManager.isRunning());
    }

    @Test
    void testGetRouterFunction() {
        RouterFunction<ServerResponse> routerFunction = mcpServerManager.getRouterFunction();
        assertNotNull(routerFunction);
    }

    @Test
    void testSseRouterFunctionActiveFlagBranches() throws Exception {
        // Inactive branch: expect 503 handler returned
        MCPProperties sseProperties = createSseMCPProperties();
        MCPServerManager sseManager = new MCPServerManager(sseProperties, objectMapper);
        RouterFunction<ServerResponse> routerFunctionInactive = sseManager.getRouterFunction();
        ServerRequest inactiveReq = createMvcServerRequest("GET", "/any");
        assertTrue(routerFunctionInactive.route(inactiveReq).isPresent());
        ServerResponse resp = routerFunctionInactive.route(inactiveReq).get().handle(inactiveReq);
        assertEquals(503, resp.statusCode().value());

        // Active branch: after start() active=true, delegate to super router
        sseManager.start();
        RouterFunction<ServerResponse> routerFunctionActive = sseManager.getRouterFunction();
        // Match GET to sseEndpoint
        ServerRequest activeReq = createMvcServerRequest(
                "GET", sseProperties.getSseServerProperties().getSseEndpoint());
        // Route may or may not be present depending on full handler composition; ensure no NPE
        routerFunctionActive.route(activeReq);

        // Pause -> inactive again
        sseManager.pause();
        RouterFunction<ServerResponse> routerFunctionPaused = sseManager.getRouterFunction();
        ServerRequest pausedReq = createMvcServerRequest("GET", "/any");
        assertTrue(routerFunctionPaused.route(pausedReq).isPresent());
        ServerResponse pausedResp = routerFunctionPaused.route(pausedReq).get().handle(pausedReq);
        assertEquals(503, pausedResp.statusCode().value());
    }

    @Test
    void testStreamableRouterFunctionActiveFlagBranches() throws Exception {
        MCPProperties streamableProperties = createStreamableMCPProperties();
        MCPServerManager streamableManager = new MCPServerManager(streamableProperties, objectMapper);

        // Inactive branch
        RouterFunction<ServerResponse> routerFunctionInactive = streamableManager.getRouterFunction();
        ServerRequest inactiveReq = createMvcServerRequest("GET", "/any");
        assertTrue(routerFunctionInactive.route(inactiveReq).isPresent());
        ServerResponse resp = routerFunctionInactive.route(inactiveReq).get().handle(inactiveReq);
        assertEquals(503, resp.statusCode().value());

        // Active branch
        streamableManager.start();
        RouterFunction<ServerResponse> routerFunctionActive = streamableManager.getRouterFunction();
        // Match GET to mcp endpoint (listening stream)
        ServerRequest activeReq = createMvcServerRequest(
                "GET", streamableProperties.getStreamableProperties().getMcpEndPoint());
        routerFunctionActive.route(activeReq);

        // Deactivate via pause
        streamableManager.pause();
        RouterFunction<ServerResponse> routerFunctionPaused = streamableManager.getRouterFunction();
        ServerRequest pausedReq = createMvcServerRequest("GET", "/any");
        assertTrue(routerFunctionPaused.route(pausedReq).isPresent());
        ServerResponse pausedResp = routerFunctionPaused.route(pausedReq).get().handle(pausedReq);
        assertEquals(503, pausedResp.statusCode().value());
    }

    private ServerRequest createMvcServerRequest(String method, String path) {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest(method, path);
        List<HttpMessageConverter<?>> converters = Collections.emptyList();
        return ServerRequest.create(servletRequest, converters);
    }

    @Test
    void testStartLifecycle() {
        assertFalse(mcpServerManager.isRunning());

        mcpServerManager.start();

        assertTrue(mcpServerManager.isRunning());
        assertNotNull(mcpServerManager.getServerInstance());
    }

    @Test
    void testStopLifecycle() {
        mcpServerManager.start();
        assertTrue(mcpServerManager.isRunning());

        mcpServerManager.stop();

        assertFalse(mcpServerManager.isRunning());
    }

    @Test
    void testStartWhenAlreadyRunning() {
        mcpServerManager.start();
        assertTrue(mcpServerManager.isRunning());
        McpAsyncServer firstInstance = mcpServerManager.getServerInstance();

        mcpServerManager.start();

        assertTrue(mcpServerManager.isRunning());
        assertEquals(firstInstance, mcpServerManager.getServerInstance());
    }

    @Test
    void testStopWhenNotRunning() {
        assertFalse(mcpServerManager.isRunning());

        assertDoesNotThrow(() -> mcpServerManager.stop());

        assertFalse(mcpServerManager.isRunning());
    }

    @Test
    void testPauseWhenRunning() {
        mcpServerManager.start();
        assertTrue(mcpServerManager.isRunning());

        mcpServerManager.pause();

        assertFalse(mcpServerManager.isRunning());
    }

    @Test
    void testPauseWhenNotRunning() {
        assertFalse(mcpServerManager.isRunning());

        mcpServerManager.pause();

        assertFalse(mcpServerManager.isRunning());
    }

    @Test
    void testResumeAfterPause() {
        mcpServerManager.start();
        mcpServerManager.pause();
        assertFalse(mcpServerManager.isRunning());

        mcpServerManager.resume();

        assertTrue(mcpServerManager.isRunning());
    }

    @Test
    void testResumeWhenAlreadyRunning() {
        mcpServerManager.start();
        assertTrue(mcpServerManager.isRunning());

        mcpServerManager.resume();

        assertTrue(mcpServerManager.isRunning());
    }

    @Test
    void testConcurrentStartStop() throws InterruptedException {
        final int threadCount = 10;
        final CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            new Thread(() -> {
                        try {
                            if (threadId % 2 == 0) {
                                mcpServerManager.start();
                            } else {
                                mcpServerManager.stop();
                            }
                        } finally {
                            latch.countDown();
                        }
                    })
                    .start();
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS));

        boolean finalState = mcpServerManager.isRunning();
        assertEquals(finalState, mcpServerManager.getRunning().get());
    }

    @Test
    void testConcurrentPauseResume() throws InterruptedException {
        mcpServerManager.start();

        final int threadCount = 10;
        final CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            new Thread(() -> {
                        try {
                            if (threadId % 2 == 0) {
                                mcpServerManager.pause();
                            } else {
                                mcpServerManager.resume();
                            }
                        } finally {
                            latch.countDown();
                        }
                    })
                    .start();
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS));

        boolean finalState = mcpServerManager.isRunning();
        assertEquals(finalState, mcpServerManager.getRunning().get());
    }

    @Test
    void testToString() {
        String toStringResult = mcpServerManager.toString();

        assertNotNull(toStringResult);
        assertTrue(toStringResult.contains("McpServerManager"));
        assertTrue(toStringResult.contains("stateLock"));
        assertTrue(toStringResult.contains("config"));
    }

    @Test
    void testGettersAndSetters() {
        assertNotNull(mcpServerManager.getStateLock());
        assertNotNull(mcpServerManager.getPoolLock());
        assertNotNull(mcpServerManager.getRunning());
        assertEquals(mcpProperties, mcpServerManager.getConfig());

        assertNull(mcpServerManager.getServerInstance());

        mcpServerManager.start();
        assertNotNull(mcpServerManager.getServerInstance());
    }

    @Test
    void testLifecycleSequence() {
        // Test the full lifecycle sequence: start -> pause -> resume -> stop

        assertFalse(mcpServerManager.isRunning());

        mcpServerManager.start();
        assertTrue(mcpServerManager.isRunning());

        mcpServerManager.pause();
        assertFalse(mcpServerManager.isRunning());

        mcpServerManager.resume();
        assertTrue(mcpServerManager.isRunning());

        mcpServerManager.stop();
        assertFalse(mcpServerManager.isRunning());
    }

    private MCPProperties createMockMCPProperties() {
        MCPProperties properties = new MCPProperties();
        properties.setMcpType("streamable");
        properties.setServerName("TestServer");
        properties.setServerVersion("1.0.0");

        MCPProperties.StreamableProperties streamableProps = new MCPProperties.StreamableProperties();
        streamableProps.setMcpEndPoint("/mcp");
        streamableProps.setHeartBeatSecondDuration(30L);
        properties.setStreamableProperties(streamableProps);

        return properties;
    }

    private MCPProperties createSseMCPProperties() {
        MCPProperties properties = new MCPProperties();
        properties.setMcpType(MCPProperties.SSE_TYPE);
        properties.setServerName("TestSseServer");
        properties.setServerVersion("1.0.0");
        MCPProperties.SseServerProperties sseProps = new MCPProperties.SseServerProperties();
        sseProps.setMessageEndpoint("/message");
        sseProps.setSseEndpoint("/sse");
        properties.setSseServerProperties(sseProps);

        return properties;
    }

    private MCPProperties createStreamableMCPProperties() {
        MCPProperties properties = new MCPProperties();
        properties.setMcpType("streamable");
        properties.setServerName("TestStreamableServer");
        properties.setServerVersion("2.0.0");

        MCPProperties.StreamableProperties streamableProps = new MCPProperties.StreamableProperties();
        streamableProps.setMcpEndPoint("/stream");
        streamableProps.setHeartBeatSecondDuration(60L);
        properties.setStreamableProperties(streamableProps);

        return properties;
    }
}
