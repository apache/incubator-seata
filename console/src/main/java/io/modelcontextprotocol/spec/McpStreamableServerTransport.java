/*
 * Copyright 2024-2025 the original author or authors.
 */

package io.modelcontextprotocol.spec;

import reactor.core.publisher.Mono;

/**
 * Streamable HTTP server transport representing an individual SSE stream.
 *
 * @author Dariusz Jędrzejczyk
 */
public interface McpStreamableServerTransport extends McpServerTransport {

	/**
	 * Send a message to the client with a message ID for use in the SSE event payload
	 * @param message the JSON-RPC payload
	 * @param messageId message id for SSE events
	 * @return Mono which completes when done
	 */
	Mono<Void> sendMessage(McpSchema.JSONRPCMessage message, String messageId);

}
