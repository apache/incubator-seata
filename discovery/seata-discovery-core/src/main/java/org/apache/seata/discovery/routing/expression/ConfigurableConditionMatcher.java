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
package org.apache.seata.discovery.routing.expression;

import org.apache.seata.common.metadata.ServiceInstance;
import org.apache.seata.discovery.routing.RoutingContext;

import java.util.Map;

/**
 * Supports loading various filtering conditions from configuration
 */
public class ConfigurableConditionMatcher implements ConditionMatcher {

    private final String condition;
    private final String key;
    private final String operator;
    private final String value;

    public ConfigurableConditionMatcher(String condition) {
        this.condition = condition.trim();

        // Split by spaces, must have three parts: key, operator, value
        String[] parts = this.condition.split("\\s+");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid condition format: " + condition);
        }

        this.key = parts[0].trim();
        this.operator = parts[1].trim();
        this.value = parts[2].trim();
    }

    @Override
    public boolean match(ServiceInstance server, RoutingContext ctx) {
        Map<String, Object> metadata = server.getMetadata();

        // If metadata is null, don't pass through
        if (metadata == null) {
            return false;
        }

        // If the attribute doesn't exist, don't pass through
        Object actualValue = metadata.get(key);
        if (actualValue == null) {
            return false;
        }

        return compareValues(actualValue.toString(), operator, value);
    }

    private boolean compareValues(String actual, String operator, String expected) {
        // == and = and != support both string and numeric comparison
        if ("==".equals(operator) || "=".equals(operator) || "!=".equals(operator)) {
            // Try numeric comparison
            try {
                double actualNum = Double.parseDouble(actual);
                double expectedNum = Double.parseDouble(expected);

                if ("==".equals(operator) || "=".equals(operator)) {
                    return Double.compare(actualNum, expectedNum) == 0;
                } else {
                    return Double.compare(actualNum, expectedNum) != 0;
                }
            } catch (NumberFormatException e) {
                // If cannot convert to numeric, perform string comparison
                if ("==".equals(operator) || "=".equals(operator)) {
                    return actual.equals(expected);
                } else {
                    return !actual.equals(expected);
                }
            }
        }

        // > < >= <= only support numeric comparison
        try {
            double actualNum = Double.parseDouble(actual);
            double expectedNum = Double.parseDouble(expected);

            switch (operator) {
                case ">":
                    return actualNum > expectedNum;
                case ">=":
                    return actualNum >= expectedNum;
                case "<":
                    return actualNum < expectedNum;
                case "<=":
                    return actualNum <= expectedNum;
                default:
                    return false; // Invalid operator
            }
        } catch (NumberFormatException e) {
            // If cannot convert to numeric, return false
            return false;
        }
    }

    @Override
    public String toString() {
        return "ConfigurableConditionMatcher{" + "condition='"
                + condition + '\'' + ", key='"
                + key + '\'' + ", operator='"
                + operator + '\'' + ", value='"
                + value + '\'' + '}';
    }
}
