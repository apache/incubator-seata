/*
 * Copyright 2024-2025 the original author or authors.
 */

package io.modelcontextprotocol.spec;

import com.fasterxml.jackson.core.type.TypeReference;
import io.modelcontextprotocol.util.Assert;
import reactor.core.publisher.Mono;

/**
 * A {@link McpLoggableSession} which represents a missing stream that would allow the
 * server to communicate with the client. Specifically, it can be used when a Streamable
 * HTTP client has not opened a listening SSE stream to accept messages for interactions
 * unrelated with concurrently running client-initiated requests.
 *
 * @author Dariusz Jędrzejczyk
 */
public class MissingMcpTransportSession implements McpLoggableSession {

	private final String sessionId;

	private volatile McpSchema.LoggingLevel minLoggingLevel = McpSchema.LoggingLevel.INFO;

	/**
	 * Create an instance with the Session ID specified.
	 * @param sessionId session ID
	 */
	public MissingMcpTransportSession(String sessionId) {
		this.sessionId = sessionId;
	}

	@Override
	public <T> Mono<T> sendRequest(String method, Object requestParams, TypeReference<T> typeRef) {
		return Mono.error(new IllegalStateException("Stream unavailable for session " + this.sessionId));
	}

	@Override
	public Mono<Void> sendNotification(String method, Object params) {
		return Mono.error(new IllegalStateException("Stream unavailable for session " + this.sessionId));
	}

	@Override
	public Mono<Void> closeGracefully() {
		return Mono.empty();
	}

	@Override
	public void close() {
	}

	@Override
	public void setMinLoggingLevel(McpSchema.LoggingLevel minLoggingLevel) {
		Assert.notNull(minLoggingLevel, "minLoggingLevel must not be null");
		this.minLoggingLevel = minLoggingLevel;
	}

	@Override
	public boolean isNotificationForLevelAllowed(McpSchema.LoggingLevel loggingLevel) {
		return loggingLevel.level() >= this.minLoggingLevel.level();
	}

}
