/*
 * Copyright 2025-2025 the original author or authors.
 */

package io.modelcontextprotocol.util;

import java.util.List;
import java.util.Map;

/**
 * Interface for working with URI templates.
 * <p>
 * This interface provides methods for extracting variables from URI templates and
 * matching them against actual URIs.
 *
 * @author Christian Tzolov
 */
public interface McpUriTemplateManager {

	/**
	 * Extract URI variable names from this URI template.
	 * @return A list of variable names extracted from the template
	 * @throws IllegalArgumentException if duplicate variable names are found
	 */
	List<String> getVariableNames();

	/**
	 * Extract URI variable values from the actual request URI.
	 * <p>
	 * This method converts the URI template into a regex pattern, then uses that pattern
	 * to extract variable values from the request URI.
	 * @param uri The actual URI from the request
	 * @return A map of variable names to their values
	 * @throws IllegalArgumentException if the URI template is invalid or the request URI
	 * doesn't match the template pattern
	 */
	Map<String, String> extractVariableValues(String uri);

	/**
	 * Indicate whether the given URI matches this template.
	 * @param uri the URI to match to
	 * @return {@code true} if it matches; {@code false} otherwise
	 */
	boolean matches(String uri);

	/**
	 * Check if the given URI is a URI template.
	 * @return Returns true if the URI contains variables in the format {variableName}
	 */
	public boolean isUriTemplate(String uri);

}
