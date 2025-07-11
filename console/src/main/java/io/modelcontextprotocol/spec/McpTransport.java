/*
 * Copyright 2024-2024 the original author or authors.
 */

package io.modelcontextprotocol.spec;

import com.fasterxml.jackson.core.type.TypeReference;
import io.modelcontextprotocol.spec.McpSchema.JSONRPCMessage;
import reactor.core.publisher.Mono;

import java.util.function.Function;

/**
 * Defines the asynchronous transport layer for the Model Context Protocol (MCP).
 *
 * <p>
 * The McpTransport interface provides the foundation for implementing custom transport
 * mechanisms in the Model Context Protocol. It handles the bidirectional communication
 * between the client and server components, supporting asynchronous message exchange
 * using JSON-RPC format.
 * </p>
 *
 * <p>
 * Implementations of this interface are responsible for:
 * </p>
 * <ul>
 * <li>Managing the lifecycle of the transport connection</li>
 * <li>Handling incoming messages and errors from the server</li>
 * <li>Sending outbound messages to the server</li>
 * </ul>
 *
 * <p>
 * The transport layer is designed to be protocol-agnostic, allowing for various
 * implementations such as WebSocket, HTTP, or custom protocols.
 * </p>
 *
 * @author Christian Tzolov
 * @author Dariusz Jędrzejczyk
 */
public interface McpTransport {

	/**
	 * Initializes and starts the transport connection.
	 *
	 * <p>
	 * This method should be called before any message exchange can occur. It sets up the
	 * necessary resources and establishes the connection to the server.
	 * </p>
	 * @deprecated This is only relevant for client-side transports and will be removed
	 * from this interface in 0.9.0.
	 */
	@Deprecated
	default Mono<Void> connect(Function<Mono<JSONRPCMessage>, Mono<JSONRPCMessage>> handler) {
		return Mono.empty();
	}

	/**
	 * Closes the transport connection and releases any associated resources.
	 *
	 * <p>
	 * This method ensures proper cleanup of resources when the transport is no longer
	 * needed. It should handle the graceful shutdown of any active connections.
	 * </p>
	 */
	default void close() {
		this.closeGracefully().subscribe();
	}

	/**
	 * Closes the transport connection and releases any associated resources
	 * asynchronously.
	 * @return a {@link Mono<Void>} that completes when the connection has been closed.
	 */
	Mono<Void> closeGracefully();

	/**
	 * Sends a message to the peer asynchronously.
	 *
	 * <p>
	 * This method handles the transmission of messages to the server in an asynchronous
	 * manner. Messages are sent in JSON-RPC format as specified by the MCP protocol.
	 * </p>
	 * @param message the {@link JSONRPCMessage} to be sent to the server
	 * @return a {@link Mono<Void>} that completes when the message has been sent
	 */
	Mono<Void> sendMessage(JSONRPCMessage message);

	/**
	 * Unmarshals the given data into an object of the specified type.
	 * @param <T> the type of the object to unmarshal
	 * @param data the data to unmarshal
	 * @param typeRef the type reference for the object to unmarshal
	 * @return the unmarshalled object
	 */
	<T> T unmarshalFrom(Object data, TypeReference<T> typeRef);

}
