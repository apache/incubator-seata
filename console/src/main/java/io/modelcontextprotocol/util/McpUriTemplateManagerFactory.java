/*
* Copyright 2025 - 2025 the original author or authors.
*/

package io.modelcontextprotocol.util;

/**
 * Factory interface for creating instances of {@link McpUriTemplateManager}.
 *
 * @author Christian Tzolov
 */
public interface McpUriTemplateManagerFactory {

	/**
	 * Creates a new instance of {@link McpUriTemplateManager} with the specified URI
	 * template.
	 * @param uriTemplate The URI template to be used for variable extraction
	 * @return A new instance of {@link McpUriTemplateManager}
	 * @throws IllegalArgumentException if the URI template is null or empty
	 */
	McpUriTemplateManager create(String uriTemplate);

}
