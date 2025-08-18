/*
 * Copyright 2024-2025 the original author or authors.
 */

package io.modelcontextprotocol.server;

import java.util.Collections;

/**
 * Context associated with the transport layer. It allows to add transport-level metadata
 * for use further down the line. Specifically, it can be beneficial to extract HTTP
 * request metadata for use in MCP feature implementations.
 *
 * @author Dariusz Jędrzejczyk
 */
public interface McpTransportContext {

	/**
	 * Key for use in Reactor Context to transport the context to user land.
	 */
	String KEY = "MCP_TRANSPORT_CONTEXT";

	/**
	 * An empty, unmodifiable context.
	 */
	@SuppressWarnings("unchecked")
	McpTransportContext EMPTY = new DefaultMcpTransportContext(Collections.EMPTY_MAP);

	/**
	 * Extract a value from the context.
	 * @param key the key under the data is expected
	 * @return the associated value or {@code null} if missing.
	 */
	Object get(String key);

	/**
	 * Inserts a value for a given key.
	 * @param key a String representing the key
	 * @param value the value to store
	 */
	void put(String key, Object value);

	/**
	 * Copies the contents of the context to allow further modifications without affecting
	 * the initial object.
	 * @return a new instance with the underlying storage copied.
	 */
	McpTransportContext copy();

}
