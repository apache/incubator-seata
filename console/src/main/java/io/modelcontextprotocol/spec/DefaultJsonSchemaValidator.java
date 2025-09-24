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
 * Copyright (c) 2025 the original author or authors.
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

package io.modelcontextprotocol.spec;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import io.modelcontextprotocol.util.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Christian Tzolov
 */
public class DefaultJsonSchemaValidator implements JsonSchemaValidator {

    private static final Logger logger = LoggerFactory.getLogger(DefaultJsonSchemaValidator.class);

    private final ObjectMapper objectMapper;

    private final JsonSchemaFactory schemaFactory;

    private final ConcurrentHashMap<String, JsonSchema> schemaCache;

    public DefaultJsonSchemaValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        this.schemaCache = new ConcurrentHashMap<>();
    }

    @Override
    public ValidationResponse validate(Map<String, Object> schema, Map<String, Object> structuredContent) {

        Assert.notNull(schema, "Schema must not be null");
        Assert.notNull(structuredContent, "Structured content must not be null");

        try {

            JsonNode jsonStructuredOutput = this.objectMapper.valueToTree(structuredContent);

            Set<ValidationMessage> validationResult =
                    this.getOrCreateJsonSchema(schema).validate(jsonStructuredOutput);

            if (!validationResult.isEmpty()) {
                return ValidationResponse.asInvalid(
                        "Validation failed: structuredContent does not match tool outputSchema. "
                                + "Validation errors: " + validationResult);
            }

            return ValidationResponse.asValid(jsonStructuredOutput.toString());

        } catch (JsonProcessingException e) {
            logger.error("Failed to validate CallToolResult: Error parsing schema: {}", e);
            return ValidationResponse.asInvalid("Error parsing tool JSON Schema: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Failed to validate CallToolResult: Unexpected error: {}", e);
            return ValidationResponse.asInvalid("Unexpected validation error: " + e.getMessage());
        }
    }

    private JsonSchema getOrCreateJsonSchema(Map<String, Object> schema) throws JsonProcessingException {
        String cacheKey = this.generateCacheKey(schema);

        JsonSchema cachedSchema = this.schemaCache.get(cacheKey);
        if (cachedSchema != null) {
            return cachedSchema;
        }

        JsonSchema newSchema = this.createJsonSchema(schema);

        JsonSchema existingSchema = this.schemaCache.putIfAbsent(cacheKey, newSchema);
        return existingSchema != null ? existingSchema : newSchema;
    }

    private JsonSchema createJsonSchema(Map<String, Object> schema) throws JsonProcessingException {
        JsonNode schemaNode = this.objectMapper.valueToTree(schema);

        if (schemaNode == null) {
            throw new JsonProcessingException("Failed to convert schema to JsonNode") {};
        }

        if (schemaNode.isObject()) {
            ObjectNode objectSchemaNode = (ObjectNode) schemaNode;
            if (!objectSchemaNode.has("additionalProperties")) {
                objectSchemaNode = objectSchemaNode.deepCopy();
                objectSchemaNode.put("additionalProperties", false);
                schemaNode = objectSchemaNode;
            }
        }

        return this.schemaFactory.getSchema(schemaNode);
    }

    protected String generateCacheKey(Map<String, Object> schema) {
        if (schema.containsKey("$id")) {
            // Use the (optional) "$id" field as the cache key if present
            return "" + schema.get("$id");
        }
        return String.valueOf(schema.hashCode());
    }
}
