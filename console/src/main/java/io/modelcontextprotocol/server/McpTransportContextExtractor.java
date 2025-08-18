/*
 * Copyright 2024-2025 the original author or authors.
 */

package io.modelcontextprotocol.server;

/**
 * The contract for extracting metadata from a generic transport request of type
 * {@link T}.
 *
 * @param <T> transport-specific representation of the request which allows extracting
 * metadata for use in the MCP features implementations.
 * @author Dariusz Jędrzejczyk
 */
public interface McpTransportContextExtractor<T> {

	/**
	 * Given an empty context, provides the means to fill it with transport-specific
	 * metadata extracted from the request.
	 * @param request the generic representation for the request in the context of a
	 * specific transport implementation
	 * @param transportContext the mutable context which can be filled in with metadata
	 * @return the context filled in with metadata. It can be the same instance as
	 * provided or a new one.
	 */
	McpTransportContext extract(T request, McpTransportContext transportContext);

}
