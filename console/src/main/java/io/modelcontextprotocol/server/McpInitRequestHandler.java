/*
 * Copyright 2024-2025 the original author or authors.
 */

package io.modelcontextprotocol.server;

import io.modelcontextprotocol.spec.McpSchema;
import reactor.core.publisher.Mono;

/**
 * Request handler for the initialization request.
 */
public interface McpInitRequestHandler {

	/**
	 * Handles the initialization request.
	 * @param initializeRequest the initialization request by the client
	 * @return a Mono that will emit the result of the initialization
	 */
	Mono<McpSchema.InitializeResult> handle(McpSchema.InitializeRequest initializeRequest);

}
