/*
 * Copyright 2024-2025 the original author or authors.
 */

package io.modelcontextprotocol.spec;

/**
 * An {@link McpSession} which is capable of processing logging notifications and keeping
 * track of a min logging level.
 *
 * @author Dariusz Jędrzejczyk
 */
public interface McpLoggableSession extends McpSession {

	/**
	 * Set the minimum logging level for the client. Messages below this level will be
	 * filtered out.
	 * @param minLoggingLevel The minimum logging level
	 */
	void setMinLoggingLevel(McpSchema.LoggingLevel minLoggingLevel);

	/**
	 * Allows checking whether a particular logging level is allowed.
	 * @param loggingLevel the level to check
	 * @return whether the logging at the specified level is permitted.
	 */
	boolean isNotificationForLevelAllowed(McpSchema.LoggingLevel loggingLevel);

}
