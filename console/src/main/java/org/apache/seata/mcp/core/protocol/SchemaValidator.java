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

import java.util.Map;

/**
 * Interface for validating JSON schemas.
 */
public interface SchemaValidator {

    final class ValidationResponse {

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

    ValidationResponse validate(Map<String, Object> schema, Map<String, Object> structuredContent);
}
