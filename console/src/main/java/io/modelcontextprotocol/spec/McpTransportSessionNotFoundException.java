/*
 * Copyright 2024-2025 the original author or authors.
 */

package io.modelcontextprotocol.spec;

/**
 * Exception that signifies that the server does not recognize the connecting client via
 * the presented transport session identifier.
 *
 * @author Dariusz Jędrzejczyk
 */
public class McpTransportSessionNotFoundException extends RuntimeException {

	/**
	 * Construct an instance with a known {@link Exception cause}.
	 * @param sessionId transport session identifier
	 * @param cause the cause that was identified as a session not found error
	 */
	public McpTransportSessionNotFoundException(String sessionId, Exception cause) {
		super("Session " + sessionId + " not found on the server", cause);
	}

	/**
	 * Construct an instance with the session identifier but without a {@link Exception
	 * cause}.
	 * @param sessionId transport session identifier
	 */
	public McpTransportSessionNotFoundException(String sessionId) {
		super("Session " + sessionId + " not found on the server");
	}

}
