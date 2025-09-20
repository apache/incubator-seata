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
