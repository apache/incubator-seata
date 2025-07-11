/*
 * Copyright 2024-2024 the original author or authors.
 */

package io.modelcontextprotocol.spec;

import com.fasterxml.jackson.core.type.TypeReference;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Represents a Model Control Protocol (MCP) session that handles communication between
 * clients and the server. This interface provides methods for sending requests and
 * notifications, as well as managing the session lifecycle.
 *
 * <p>
 * The session operates asynchronously using Project Reactor's {@link Mono} type for
 * non-blocking operations. It supports both request-response patterns and one-way
 * notifications.
 * </p>
 *
 * @author Christian Tzolov
 * @author Dariusz Jędrzejczyk
 */
public interface McpSession {

	/**
	 * Sends a request to the model counterparty and expects a response of type T.
	 *
	 * <p>
	 * This method handles the request-response pattern where a response is expected from
	 * the client or server. The response type is determined by the provided
	 * TypeReference.
	 * </p>
	 * @param <T> the type of the expected response
	 * @param method the name of the method to be called on the counterparty
	 * @param requestParams the parameters to be sent with the request
	 * @param typeRef the TypeReference describing the expected response type
	 * @return a Mono that will emit the response when received
	 */
	<T> Mono<T> sendRequest(String method, Object requestParams, TypeReference<T> typeRef);

	/**
	 * Sends a notification to the model client or server without parameters.
	 *
	 * <p>
	 * This method implements the notification pattern where no response is expected from
	 * the counterparty. It's useful for fire-and-forget scenarios.
	 * </p>
	 * @param method the name of the notification method to be called on the server
	 * @return a Mono that completes when the notification has been sent
	 */
	default Mono<Void> sendNotification(String method) {
		return sendNotification(method, null);
	}

	/**
	 * Sends a notification to the model client or server with parameters.
	 *
	 * <p>
	 * Similar to {@link #sendNotification(String)} but allows sending additional
	 * parameters with the notification.
	 * </p>
	 * @param method the name of the notification method to be sent to the counterparty
	 * @param params a map of parameters to be sent with the notification
	 * @return a Mono that completes when the notification has been sent
	 */
	Mono<Void> sendNotification(String method, Map<String, Object> params);

	/**
	 * Closes the session and releases any associated resources asynchronously.
	 * @return a {@link Mono<Void>} that completes when the session has been closed.
	 */
	Mono<Void> closeGracefully();

	/**
	 * Closes the session and releases any associated resources.
	 */
	void close();

}
