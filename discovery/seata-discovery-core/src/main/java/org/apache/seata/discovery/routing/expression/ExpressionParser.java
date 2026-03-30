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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Expression parser
 * Parses routing expressions and generates condition matchers
 *
 * Supported basic syntax:
 * - key == value: exact match (Java style)
 * - key = value: exact match (legacy style)
 * - key >= value: greater than or equal
 * - key <= value: less than or equal
 * - key > value: greater than
 * - key < value: less than
 * - key != value: not equal
 *
 * Supports three modes:
 * 1. Single expression: version >= 2.3
 * 2. OR logic expression: (version >= 2.3) || (env == dev) || (region == cn-bj)
 * 3. AND logic expression: (version >= 2.3) && (env == prod) && (region == cn-bj)
 *
 * Note: Mixed AND/OR logic is not supported, use multiple MetadataRouters for complex logic
 */
public class ExpressionParser {

    private static final Pattern PARENTHESES_PATTERN = Pattern.compile("\\(([^()]+)\\)");

    /**
     * Parse expression
     * @param expression expression string
     * @return list of condition matchers
     * @throws IllegalArgumentException thrown when expression format is incorrect
     */
    public static List<ConditionMatcher> parse(String expression) {
        String trimmedExpression = expression.trim();

        // Validate expression format
        if (!isValidExpression(trimmedExpression)) {
            throw new IllegalArgumentException("Invalid expression format: " + expression);
        }

        // Check if contains logical operators
        if (trimmedExpression.contains("||")) {
            return parseOrExpression(trimmedExpression);
        } else if (trimmedExpression.contains("&&")) {
            return parseAndExpression(trimmedExpression);
        } else {
            return parseSingleExpression(trimmedExpression);
        }
    }

    /**
     * Parse single expression
     * @param expression single expression
     * @return list of condition matchers
     */
    private static List<ConditionMatcher> parseSingleExpression(String expression) {
        List<ConditionMatcher> matchers = new ArrayList<>();

        // Remove outer parentheses only if both sides have parentheses
        String cleanExpression = expression.trim();
        if (cleanExpression.startsWith("(") && cleanExpression.endsWith(")")) {
            cleanExpression =
                    cleanExpression.substring(1, cleanExpression.length() - 1).trim();
        }

        if (!cleanExpression.isEmpty()) {
            matchers.add(new ConfigurableConditionMatcher(cleanExpression));
        }

        return matchers;
    }

    /**
     * Parse OR logic expression
     * Format: (condition1) || (condition2) || (condition3) or condition1 || condition2 || condition3
     * @param expression OR logic expression
     * @return list of condition matchers
     */
    private static List<ConditionMatcher> parseOrExpression(String expression) {
        List<ConditionMatcher> matchers = new ArrayList<>();

        // Split by ||
        String[] parts = expression.split("\\|\\|");

        for (String part : parts) {
            part = part.trim();
            if (!part.isEmpty()) {
                // Try to extract content inside parentheses if they exist
                Matcher matcher = PARENTHESES_PATTERN.matcher(part);
                if (matcher.find()) {
                    String condition = matcher.group(1).trim();
                    if (!condition.isEmpty()) {
                        matchers.add(new ConfigurableConditionMatcher(condition));
                    }
                } else {
                    // No parentheses, treat the whole part as a condition
                    if (isValidSingleExpression(part)) {
                        matchers.add(new ConfigurableConditionMatcher(part));
                    } else {
                        throw new IllegalArgumentException("Invalid OR expression part: " + part);
                    }
                }
            }
        }

        return matchers;
    }

    /**
     * Parse AND logic expression
     * Format: (condition1) && (condition2) && (condition3) or condition1 && condition2 && condition3
     * @param expression AND logic expression
     * @return list of condition matchers
     */
    private static List<ConditionMatcher> parseAndExpression(String expression) {
        List<ConditionMatcher> matchers = new ArrayList<>();

        // Split by '&&'
        String[] parts = expression.split("&&");

        for (String part : parts) {
            part = part.trim();
            if (!part.isEmpty()) {
                // Try to extract content inside parentheses if they exist
                Matcher matcher = PARENTHESES_PATTERN.matcher(part);
                if (matcher.find()) {
                    String condition = matcher.group(1).trim();
                    if (!condition.isEmpty()) {
                        matchers.add(new ConfigurableConditionMatcher(condition));
                    }
                } else {
                    // No parentheses, treat the whole part as a condition
                    if (isValidSingleExpression(part)) {
                        matchers.add(new ConfigurableConditionMatcher(part));
                    } else {
                        throw new IllegalArgumentException("Invalid AND expression part: " + part);
                    }
                }
            }
        }

        return matchers;
    }

    /**
     * Validate if expression format is correct
     * @param expression expression string
     * @return whether valid
     */
    public static boolean isValidExpression(String expression) {
        String trimmedExpression = expression.trim();

        // Check if contains logical operators
        if (trimmedExpression.contains("||")) {
            return isValidOrExpression(trimmedExpression);
        } else if (trimmedExpression.contains("&&")) {
            return isValidAndExpression(trimmedExpression);
        } else {
            return isValidSingleExpression(trimmedExpression);
        }
    }

    /**
     * Validate single expression format
     * @param expression single expression
     * @return whether valid
     */
    private static boolean isValidSingleExpression(String expression) {
        // Handle parentheses: only remove if both sides have parentheses
        String cleanExpression = expression.trim();
        if (cleanExpression.startsWith("(") && cleanExpression.endsWith(")")) {
            cleanExpression =
                    cleanExpression.substring(1, cleanExpression.length() - 1).trim();
        }

        if (cleanExpression.isEmpty()) {
            return false;
        }

        // Split by spaces, must have three parts: key, operator, value
        String[] parts = cleanExpression.split("\\s+");
        if (parts.length != 3) {
            return false;
        }

        String key = parts[0].trim();
        String operator = parts[1].trim();
        String value = parts[2].trim();

        // Validate key name: cannot be empty and cannot contain spaces
        if (key.isEmpty() || key.contains(" ")) {
            return false;
        }

        // Validate operator: must be a valid comparison operator
        if (!isValidOperator(operator)) {
            return false;
        }

        // Validate value: cannot be empty
        return !value.isEmpty();
    }

    /**
     * Validate OR expression format
     * @param expression OR expression
     * @return whether valid
     */
    private static boolean isValidOrExpression(String expression) {
        // Split by ||
        String[] parts = expression.split("\\|\\|");

        for (String part : parts) {
            part = part.trim();
            if (!part.isEmpty()) {
                // Check if has parentheses
                if (part.startsWith("(") && part.endsWith(")")) {
                    // Extract content inside parentheses
                    String innerExpression =
                            part.substring(1, part.length() - 1).trim();
                    if (!isValidSingleExpression(innerExpression)) {
                        return false;
                    }
                } else {
                    // No parentheses, validate the part directly
                    if (!isValidSingleExpression(part)) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    /**
     * Validate AND expression format
     * @param expression AND expression
     * @return whether valid
     */
    private static boolean isValidAndExpression(String expression) {
        // Split by '&&'
        String[] parts = expression.split("&&");

        for (String part : parts) {
            part = part.trim();
            if (!part.isEmpty()) {
                // Check if has parentheses
                if (part.startsWith("(") && part.endsWith(")")) {
                    // Extract content inside parentheses
                    String innerExpression =
                            part.substring(1, part.length() - 1).trim();
                    if (!isValidSingleExpression(innerExpression)) {
                        return false;
                    }
                } else {
                    // No parentheses, validate the part directly
                    if (!isValidSingleExpression(part)) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    /**
     * Validate if operator is valid
     * @param operator operator
     * @return whether valid
     */
    private static boolean isValidOperator(String operator) {
        // Support both Java style (==) and legacy style (=)
        return "==".equals(operator)
                || "=".equals(operator)
                || "!=".equals(operator)
                || ">".equals(operator)
                || ">=".equals(operator)
                || "<".equals(operator)
                || "<=".equals(operator);
    }

    /**
     * Check if expression contains OR logic
     * @param expression expression string
     * @return whether contains OR logic
     */
    public static boolean isOrExpression(String expression) {
        return expression != null && expression.contains("||");
    }

    /**
     * Check if expression contains AND logic
     * @param expression expression string
     * @return whether contains AND logic
     */
    public static boolean isAndExpression(String expression) {
        return expression != null && expression.contains("&&");
    }
}
