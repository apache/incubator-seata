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

package org.apache.seata.mcp.core.protocol;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.apache.seata.mcp.core.common.RuntimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Standard implementation of JSON schema validator.
 */
public class StandardJsonSchemaValidator implements SchemaValidator {

    private static final Logger logger = LoggerFactory.getLogger(StandardJsonSchemaValidator.class);

    private final ObjectMapper mapper;
    private final JsonSchemaFactory factory;
    private final ConcurrentHashMap<String, JsonSchema> cache = new ConcurrentHashMap<>();

    public StandardJsonSchemaValidator(ObjectMapper mapper) {
        this.mapper = mapper;
        this.factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
    }

    @Override
    public ValidationResponse validate(Map<String, Object> schema, Map<String, Object> content) {
        RuntimeUtils.notNull(schema, "Schema required");
        RuntimeUtils.notNull(content, "Content required");

        try {
            JsonNode node = mapper.valueToTree(content);
            Set<ValidationMessage> errors = getSchema(schema).validate(node);

            if (!errors.isEmpty()) {
                return ValidationResponse.asInvalid("Validation failed: " + errors);
            }

            return ValidationResponse.asValid(node.toString());
        } catch (JsonProcessingException e) {
            logger.error("Parse error: {}", e.getMessage());
            return ValidationResponse.asInvalid("Parse error: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Validation error: {}", e.getMessage());
            return ValidationResponse.asInvalid("Validation error: " + e.getMessage());
        }
    }

    private JsonSchema getSchema(Map<String, Object> schema) throws JsonProcessingException {
        String key = schema.containsKey("$id") ? String.valueOf(schema.get("$id")) : String.valueOf(schema.hashCode());

        JsonSchema cached = cache.get(key);
        if (cached != null) {
            return cached;
        }

        JsonSchema newSchema = createSchema(schema);
        JsonSchema existing = cache.putIfAbsent(key, newSchema);
        return existing != null ? existing : newSchema;
    }

    private JsonSchema createSchema(Map<String, Object> schema) throws JsonProcessingException {
        JsonNode node = mapper.valueToTree(schema);

        if (node.isObject()) {
            ObjectNode obj = (ObjectNode) node;
            if (!obj.has("additionalProperties")) {
                obj = obj.deepCopy();
                obj.put("additionalProperties", false);
                node = obj;
            }
        }

        return factory.getSchema(node);
    }
}
