/*
 * Copyright 2024-2024 the original author or authors.
 */

package io.modelcontextprotocol.spec;

import java.util.Map;

/**
 * Interface for validating structured content against a JSON schema. This interface
 * defines a method to validate structured content based on the provided output schema.
 *
 * @author Christian Tzolov
 */
public interface JsonSchemaValidator {

    /**
     * Represents the result of a validation operation.
     *
     * validation was successful, otherwise null.
     */
    public static final class ValidationResponse {

        private final boolean valid;
        private final String errorMessage;
        private final String jsonStructuredOutput;

        public ValidationResponse(boolean valid, String errorMessage, String jsonStructuredOutput) {
            this.valid = valid;
            this.errorMessage = errorMessage;
            this.jsonStructuredOutput = jsonStructuredOutput;
        }

        public static ValidationResponse asValid(String jsonStructuredOutput) {
            return new ValidationResponse(true, null, jsonStructuredOutput);
        }

        public static ValidationResponse asInvalid(String message) {
            return new ValidationResponse(false, message, null);
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public String getJsonStructuredOutput() {
            return jsonStructuredOutput;
        }
    }

    /**
     * Validates the structured content against the provided JSON schema.
     * @param schema The JSON schema to validate against.
     * @param structuredContent The structured content to validate.
     * @return A ValidationResponse indicating whether the validation was successful or
     * not.
     */
    ValidationResponse validate(Map<String, Object> schema, Map<String, Object> structuredContent);
}
