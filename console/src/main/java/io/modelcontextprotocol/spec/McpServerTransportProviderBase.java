/*
 * Copyright 2024-2025 the original author or authors.
 */

package io.modelcontextprotocol.spec;

import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

/**
 * The core building block providing the server-side MCP transport. Implement this
 * interface to bridge between a particular server-side technology and the MCP server
 * transport layer.
 *
 * <p>
 * The lifecycle of the provider dictates that it be created first, upon application
 * startup, and then passed into either
 * a result of the MCP server creation, the provider will be notified of a
 * {@link McpServerSession.Factory} which will be used to handle a 1:1 communication
 * between a newly connected client and the server. The provider's responsibility is to
 * create instances of {@link McpServerTransport} that the session will utilise during the
 * session lifetime.
 *
 * <p>
 * Finally, the {@link McpServerTransport}s can be closed in bulk when {@link #close()} or
 * {@link #closeGracefully()} are called as part of the normal application shutdown event.
 * Individual {@link McpServerTransport}s can also be closed on a per-session basis, where
 * the {@link McpServerSession#close()} or {@link McpServerSession#closeGracefully()}
 * closes the provided transport.
 *
 * @author Dariusz Jędrzejczyk
 */
public interface McpServerTransportProviderBase {

    /**
     * Sends a notification to all connected clients.
     * @param method the name of the notification method to be called on the clients
     * @param params parameters to be sent with the notification
     * @return a Mono that completes when the notification has been broadcast
     */
    Mono<Void> notifyClients(String method, Object params);

    /**
     * Immediately closes all the transports with connected clients and releases any
     * associated resources.
     */
    default void close() {
        this.closeGracefully().subscribe();
    }

    /**
     * Gracefully closes all the transports with connected clients and releases any
     * associated resources asynchronously.
     * @return a {@link Mono<Void>} that completes when the connections have been closed.
     */
    Mono<Void> closeGracefully();

    /**
     * Returns the protocol version supported by this transport provider.
     * @return the protocol version as a string
     */
    default List<String> protocolVersions() {
        return Collections.singletonList(ProtocolVersions.MCP_2024_11_05);
    }
}
