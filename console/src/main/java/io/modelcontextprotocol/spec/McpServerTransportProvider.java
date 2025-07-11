package io.modelcontextprotocol.spec;

import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * The core building block providing the server-side MCP transport. Implement this
 * interface to bridge between a particular server-side technology and the MCP server
 * transport layer.
 *
 * <p>
 * The lifecycle of the provider dictates that it be created first, upon application
 * startup, and then passed into either
 * {@link io.modelcontextprotocol.server.McpServer#sync(McpServerTransportProvider)} or
 * {@link io.modelcontextprotocol.server.McpServer#async(McpServerTransportProvider)}. As
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
public interface McpServerTransportProvider {

	/**
	 * Sets the session factory that will be used to create sessions for new clients. An
	 * implementation of the MCP server MUST call this method before any MCP interactions
	 * take place.
	 * @param sessionFactory the session factory to be used for initiating client sessions
	 */
	void setSessionFactory(McpServerSession.Factory sessionFactory);

	/**
	 * Sends a notification to all connected clients.
	 * @param method the name of the notification method to be called on the clients
	 * @param params a map of parameters to be sent with the notification
	 * @return a Mono that completes when the notification has been broadcast
	 * @see McpSession#sendNotification(String, Map)
	 */
	Mono<Void> notifyClients(String method, Map<String, Object> params);

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

}
