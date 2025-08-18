/*
 * Copyright 2024-2025 the original author or authors.
 */

package io.modelcontextprotocol.spec;

import org.reactivestreams.Publisher;
import reactor.util.function.Tuple2;

import java.util.Optional;

/**
 * A representation of a stream at the transport layer of the MCP protocol. In particular,
 * it is currently used in the Streamable HTTP implementation to potentially be able to
 * resume a broken connection from where it left off by optionally keeping track of
 * attached SSE event ids.
 *
 * @param <CONNECTION> the resource on which the stream is being served and consumed via
 * this mechanism
 * @author Dariusz Jędrzejczyk
 */
public interface McpTransportStream<CONNECTION> {

	/**
	 * The last observed event identifier.
	 * @return if not empty, contains the most recent event that was consumed
	 */
	Optional<String> lastId();

	/**
	 * An internal stream identifier used to distinguish streams while debugging.
	 * @return a {@code long} stream identifier value
	 */
	long streamId();

	/**
	 * Allows keeping track of the transport stream of events (currently an SSE stream
	 * from Streamable HTTP specification) and enable resumability and reconnects in case
	 * of stream errors.
	 * @param eventStream a {@link Publisher} of tuples (pairs) of an optional identifier
	 * associated with a collection of messages
	 * @return a flattened {@link Publisher} of
	 * {@link McpSchema.JSONRPCMessage JSON-RPC messages}
	 * with the identifier stripped away
	 */
	Publisher<McpSchema.JSONRPCMessage> consumeSseStream(
			Publisher<Tuple2<Optional<String>, Iterable<McpSchema.JSONRPCMessage>>> eventStream);

}
