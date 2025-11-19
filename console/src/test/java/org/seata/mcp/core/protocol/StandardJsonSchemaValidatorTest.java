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
package org.seata.mcp.core.protocol;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.seata.mcp.core.protocol.SchemaValidator;
import org.apache.seata.mcp.core.protocol.StandardJsonSchemaValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for StandardJsonSchemaValidator and SchemaValidator
 */
class StandardJsonSchemaValidatorTest {

    private ObjectMapper objectMapper;
    private StandardJsonSchemaValidator validator;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        validator = new StandardJsonSchemaValidator(objectMapper);
    }

    @Test
    void testValidateSuccess() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> nameProp = new HashMap<>();
        nameProp.put("type", "string");
        properties.put("name", nameProp);
        schema.put("properties", properties);

        Map<String, Object> content = new HashMap<>();
        content.put("name", "test");

        SchemaValidator.ValidationResponse response = validator.validate(schema, content);
        assertTrue(response.isValid());
        assertNull(response.getErrorMessage());
        assertNotNull(response.getJsonStructuredOutput());
    }

    @Test
    void testValidateFailure() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> nameProp = new HashMap<>();
        nameProp.put("type", "string");
        properties.put("name", nameProp);
        schema.put("properties", properties);
        schema.put("required", java.util.Arrays.asList("name"));

        Map<String, Object> content = new HashMap<>();
        content.put("name", 123);

        SchemaValidator.ValidationResponse response = validator.validate(schema, content);
        assertFalse(response.isValid());
        assertNotNull(response.getErrorMessage());
        assertNull(response.getJsonStructuredOutput());
    }

    @Test
    void testValidateWithRequiredField() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> nameProp = new HashMap<>();
        nameProp.put("type", "string");
        properties.put("name", nameProp);
        schema.put("properties", properties);
        schema.put("required", java.util.Arrays.asList("name"));

        Map<String, Object> content = new HashMap<>();
        content.put("name", "test");

        SchemaValidator.ValidationResponse response = validator.validate(schema, content);
        assertTrue(response.isValid());
    }

    @Test
    void testValidateWithMissingRequiredField() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> nameProp = new HashMap<>();
        nameProp.put("type", "string");
        properties.put("name", nameProp);
        schema.put("properties", properties);
        schema.put("required", java.util.Arrays.asList("name"));

        Map<String, Object> content = new HashMap<>();

        SchemaValidator.ValidationResponse response = validator.validate(schema, content);
        assertFalse(response.isValid());
        assertNotNull(response.getErrorMessage());
    }

    @Test
    void testValidateWithAdditionalProperties() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        Map<String, Object> properties = new HashMap<>();
        schema.put("properties", properties);
        schema.put("additionalProperties", false);

        Map<String, Object> content = new HashMap<>();
        content.put("name", "test");

        SchemaValidator.ValidationResponse response = validator.validate(schema, content);
        assertFalse(response.isValid());
    }

    @Test
    void testValidateWithAdditionalPropertiesTrue() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        Map<String, Object> properties = new HashMap<>();
        schema.put("properties", properties);
        schema.put("additionalProperties", true);

        Map<String, Object> content = new HashMap<>();
        content.put("name", "test");

        SchemaValidator.ValidationResponse response = validator.validate(schema, content);
        assertTrue(response.isValid());
    }

    @Test
    void testValidateWithIntegerType() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> ageProp = new HashMap<>();
        ageProp.put("type", "integer");
        properties.put("age", ageProp);
        schema.put("properties", properties);

        Map<String, Object> content = new HashMap<>();
        content.put("age", 25);

        SchemaValidator.ValidationResponse response = validator.validate(schema, content);
        assertTrue(response.isValid());
    }

    @Test
    void testValidateWithNumberType() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> priceProp = new HashMap<>();
        priceProp.put("type", "number");
        properties.put("price", priceProp);
        schema.put("properties", properties);

        Map<String, Object> content = new HashMap<>();
        content.put("price", 99.99);

        SchemaValidator.ValidationResponse response = validator.validate(schema, content);
        assertTrue(response.isValid());
    }

    @Test
    void testValidateWithBooleanType() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> activeProp = new HashMap<>();
        activeProp.put("type", "boolean");
        properties.put("active", activeProp);
        schema.put("properties", properties);

        Map<String, Object> content = new HashMap<>();
        content.put("active", true);

        SchemaValidator.ValidationResponse response = validator.validate(schema, content);
        assertTrue(response.isValid());
    }

    @Test
    void testValidateWithArrayType() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> itemsProp = new HashMap<>();
        itemsProp.put("type", "array");
        Map<String, Object> items = new HashMap<>();
        items.put("type", "string");
        itemsProp.put("items", items);
        properties.put("items", itemsProp);
        schema.put("properties", properties);

        Map<String, Object> content = new HashMap<>();
        content.put("items", java.util.Arrays.asList("item1", "item2"));

        SchemaValidator.ValidationResponse response = validator.validate(schema, content);
        assertTrue(response.isValid());
    }

    @Test
    void testValidateWithNestedObject() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> addressProp = new HashMap<>();
        addressProp.put("type", "object");
        Map<String, Object> addressProperties = new HashMap<>();
        Map<String, Object> streetProp = new HashMap<>();
        streetProp.put("type", "string");
        addressProperties.put("street", streetProp);
        addressProp.put("properties", addressProperties);
        properties.put("address", addressProp);
        schema.put("properties", properties);

        Map<String, Object> content = new HashMap<>();
        Map<String, Object> address = new HashMap<>();
        address.put("street", "Main St");
        content.put("address", address);

        SchemaValidator.ValidationResponse response = validator.validate(schema, content);
        assertTrue(response.isValid());
    }

    @Test
    void testValidateWithSchemaId() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("$id", "test-schema");
        schema.put("type", "object");
        Map<String, Object> properties = new HashMap<>();
        schema.put("properties", properties);

        Map<String, Object> content = new HashMap<>();

        SchemaValidator.ValidationResponse response = validator.validate(schema, content);
        assertFalse(response.isValid());
    }

    @Test
    void testValidateNullSchema() {
        Map<String, Object> content = new HashMap<>();
        assertThrows(IllegalArgumentException.class, () -> {
            validator.validate(null, content);
        });
    }

    @Test
    void testValidateNullContent() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        assertThrows(IllegalArgumentException.class, () -> {
            validator.validate(schema, null);
        });
    }

    @Test
    void testValidationResponseAsValid() {
        SchemaValidator.ValidationResponse response = SchemaValidator.ValidationResponse.asValid("{\"result\":\"ok\"}");
        assertTrue(response.isValid());
        assertNull(response.getErrorMessage());
        assertEquals("{\"result\":\"ok\"}", response.getJsonStructuredOutput());
    }

    @Test
    void testValidationResponseAsInvalid() {
        SchemaValidator.ValidationResponse response = SchemaValidator.ValidationResponse.asInvalid("Error message");
        assertFalse(response.isValid());
        assertEquals("Error message", response.getErrorMessage());
        assertNull(response.getJsonStructuredOutput());
    }

    @Test
    void testValidationResponseConstructor() {
        SchemaValidator.ValidationResponse response = new SchemaValidator.ValidationResponse(true, "error", "output");
        assertTrue(response.isValid());
        assertEquals("error", response.getErrorMessage());
        assertEquals("output", response.getJsonStructuredOutput());
    }

    @Test
    void testSchemaCaching() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("$id", "cached-schema");
        schema.put("type", "object");
        Map<String, Object> properties = new HashMap<>();
        schema.put("properties", properties);

        Map<String, Object> content = new HashMap<>();

        SchemaValidator.ValidationResponse response1 = validator.validate(schema, content);
        SchemaValidator.ValidationResponse response2 = validator.validate(schema, content);

        assertFalse(response1.isValid());
        assertFalse(response2.isValid());
    }
}
