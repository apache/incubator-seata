/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * ------------------------------------------------------------------------
 * This file contains code originally from the [Model Context Protocol Java SDK],
 * which is licensed under the MIT License.
 *
 * The original MIT license text is reproduced below:
 * ------------------------------------------------------------------------
 */

/*
 * MIT License
 * Copyright (c) [Year] the original author or authors.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.modelcontextprotocol.util;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Default implementation of the UriTemplateUtils interface.
 * <p>
 * This class provides methods for extracting variables from URI templates and matching
 * them against actual URIs.
 *
 * @author Christian Tzolov
 */
public class DefaultMcpUriTemplateManager implements McpUriTemplateManager {

    /**
     * Pattern to match URI variables in the format {variableName}.
     */
    private static final Pattern URI_VARIABLE_PATTERN = Pattern.compile("\\{([^/]+?)\\}");

    private final String uriTemplate;

    /**
     * Constructor for DefaultMcpUriTemplateManager.
     * @param uriTemplate The URI template to be used for variable extraction
     */
    public DefaultMcpUriTemplateManager(String uriTemplate) {
        if (uriTemplate == null || uriTemplate.isEmpty()) {
            throw new IllegalArgumentException("URI template must not be null or empty");
        }
        this.uriTemplate = uriTemplate;
    }

    /**
     * Extract URI variable names from a URI template.
     * {variableName}
     * @return A list of variable names extracted from the template
     * @throws IllegalArgumentException if duplicate variable names are found
     */
    @Override
    public List<String> getVariableNames() {
        if (uriTemplate == null || uriTemplate.isEmpty()) {
            return Collections.singletonList("");
        }

        List<String> variables = new ArrayList<>();
        Matcher matcher = URI_VARIABLE_PATTERN.matcher(this.uriTemplate);

        while (matcher.find()) {
            String variableName = matcher.group(1);
            if (variables.contains(variableName)) {
                throw new IllegalArgumentException("Duplicate URI variable name in template: " + variableName);
            }
            variables.add(variableName);
        }

        return variables;
    }

    /**
     * Extract URI variable values from the actual request URI.
     * <p>
     * This method converts the URI template into a regex pattern, then uses that pattern
     * to extract variable values from the request URI.
     * @param requestUri The actual URI from the request
     * @return A map of variable names to their values
     * @throws IllegalArgumentException if the URI template is invalid or the request URI
     * doesn't match the template pattern
     */
    @Override
    public Map<String, String> extractVariableValues(String requestUri) {
        Map<String, String> variableValues = new HashMap<>();
        List<String> uriVariables = this.getVariableNames();

        if (requestUri == null || uriVariables.isEmpty()) {
            return variableValues;
        }

        try {
            // Create a regex pattern by replacing each {variableName} with a capturing
            // group
            StringBuilder patternBuilder = new StringBuilder("^");

            // Find all variable placeholders and their positions
            Matcher variableMatcher = URI_VARIABLE_PATTERN.matcher(uriTemplate);
            int lastEnd = 0;

            while (variableMatcher.find()) {
                // Add the text between the last variable and this one, escaped for regex
                String textBefore = uriTemplate.substring(lastEnd, variableMatcher.start());
                patternBuilder.append(Pattern.quote(textBefore));

                // Add a capturing group for the variable
                patternBuilder.append("([^/]+)");

                lastEnd = variableMatcher.end();
            }

            // Add any remaining text after the last variable
            if (lastEnd < uriTemplate.length()) {
                patternBuilder.append(Pattern.quote(uriTemplate.substring(lastEnd)));
            }

            patternBuilder.append("$");

            // Compile the pattern and match against the request URI
            Pattern pattern = Pattern.compile(patternBuilder.toString());
            Matcher matcher = pattern.matcher(requestUri);

            if (matcher.find() && matcher.groupCount() == uriVariables.size()) {
                for (int i = 0; i < uriVariables.size(); i++) {
                    String value = matcher.group(i + 1);
                    if (value == null || value.isEmpty()) {
                        throw new IllegalArgumentException(
                                "Empty value for URI variable '" + uriVariables.get(i) + "' in URI: " + requestUri);
                    }
                    variableValues.put(uriVariables.get(i), value);
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Error parsing URI template: " + uriTemplate + " for URI: " + requestUri, e);
        }

        return variableValues;
    }

    /**
     * Check if a URI matches the uriTemplate with variables.
     * @param uri The URI to check
     * @return true if the URI matches the pattern, false otherwise
     */
    @Override
    public boolean matches(String uri) {
        // If the uriTemplate doesn't contain variables, do a direct comparison
        if (!this.isUriTemplate(this.uriTemplate)) {
            return uri.equals(this.uriTemplate);
        }

        // Convert the pattern to a regex
        String regex = this.uriTemplate.replaceAll("\\{[^/]+?\\}", "([^/]+?)");
        regex = regex.replace("/", "\\/");

        // Check if the URI matches the regex
        return Pattern.compile(regex).matcher(uri).matches();
    }

    @Override
    public boolean isUriTemplate(String uri) {
        return URI_VARIABLE_PATTERN.matcher(uri).find();
    }
}
