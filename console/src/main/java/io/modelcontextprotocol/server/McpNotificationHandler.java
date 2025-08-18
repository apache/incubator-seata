package io.modelcontextprotocol.server;

import reactor.core.publisher.Mono;

/**
 * A handler for client-initiated notifications.
 */
public interface McpNotificationHandler {

	/**
	 * Handles a notification from the client.
	 * @param exchange the exchange associated with the client that allows calling back to
	 * the connected client or inspecting its capabilities.
	 * @param params the parameters of the notification.
	 * @return a Mono that completes once the notification is handled.
	 */
	Mono<Void> handle(McpAsyncServerExchange exchange, Object params);

}
