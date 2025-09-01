package org.apache.seata.mcp.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpAsyncServer;
import org.apache.seata.mcp.entity.pojo.MCPProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

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
    void testGetRouterFunction() {
        RouterFunction<ServerResponse> routerFunction = mcpServerManager.getRouterFunction();
        assertNotNull(routerFunction);
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
        properties.setResourceSupport(true);
        properties.setResourceTemplates(false);
        properties.setPromptSupport(true);

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
        properties.setResourceSupport(true);
        properties.setResourceTemplates(true);
        properties.setPromptSupport(true);

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
        properties.setResourceSupport(false);
        properties.setResourceTemplates(false);
        properties.setPromptSupport(false);

        MCPProperties.StreamableProperties streamableProps = new MCPProperties.StreamableProperties();
        streamableProps.setMcpEndPoint("/stream");
        streamableProps.setHeartBeatSecondDuration(60L);
        properties.setStreamableProperties(streamableProps);

        return properties;
    }
}
