/*
* Copyright 2025 - 2025 the original author or authors.
*/

package io.modelcontextprotocol.util;

/**
 * @author Christian Tzolov
 */
public class DeafaultMcpUriTemplateManagerFactory implements McpUriTemplateManagerFactory {

	/**
	 * Creates a new instance of {@link McpUriTemplateManager} with the specified URI
	 * template.
	 * @param uriTemplate The URI template to be used for variable extraction
	 * @return A new instance of {@link McpUriTemplateManager}
	 * @throws IllegalArgumentException if the URI template is null or empty
	 */
	@Override
	public McpUriTemplateManager create(String uriTemplate) {
		return new DefaultMcpUriTemplateManager(uriTemplate);
	}

}
