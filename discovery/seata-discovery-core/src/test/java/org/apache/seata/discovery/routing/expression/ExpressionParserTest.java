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

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ExpressionParserTest {

    /**
     * Test single expressions - verify basic parsing functionality
     */
    @Test
    public void testParseWithSingleExpressions() {
        // Test single expression with >= operator
        String versionExpression = "version >= 2.3";
        List<ConditionMatcher> versionMatchers = ExpressionParser.parse(versionExpression);
        assertEquals(1, versionMatchers.size());
        assertFalse(ExpressionParser.isOrExpression(versionExpression));
        assertFalse(ExpressionParser.isAndExpression(versionExpression));

        // Test single expression with == operator
        String envExpression = "env == prod";
        List<ConditionMatcher> envMatchers = ExpressionParser.parse(envExpression);
        assertEquals(1, envMatchers.size());
        assertFalse(ExpressionParser.isOrExpression(envExpression));
        assertFalse(ExpressionParser.isAndExpression(envExpression));
    }

    /**
     * Test complex expressions with parentheses - verify multi-condition parsing
     */
    @Test
    public void testParseWithComplexExpressionsWithParentheses() {
        // Test OR expressions with parentheses (double pipe)
        String orExpression = "(version >= 2.3) || (env == dev) || (region == cn-bj)";
        List<ConditionMatcher> orMatchers = ExpressionParser.parse(orExpression);
        assertEquals(3, orMatchers.size());
        assertTrue(ExpressionParser.isOrExpression(orExpression));
        assertFalse(ExpressionParser.isAndExpression(orExpression));

        // Test AND expressions with parentheses
        String andExpression = "(version >= 2.3) && (env == prod) && (region == cn-bj)";
        List<ConditionMatcher> andMatchers = ExpressionParser.parse(andExpression);
        assertEquals(3, andMatchers.size());
        assertFalse(ExpressionParser.isOrExpression(andExpression));
        assertTrue(ExpressionParser.isAndExpression(andExpression));
    }

    /**
     * Test expressions without parentheses - verify flexible parentheses handling
     */
    @Test
    public void testParseWithExpressionsWithoutParentheses() {
        // Test OR expression without parentheses
        String orExpression = "version >= 2.3 || env == dev || region == cn-bj";
        List<ConditionMatcher> orMatchers = ExpressionParser.parse(orExpression);
        assertEquals(3, orMatchers.size());
        assertTrue(ExpressionParser.isOrExpression(orExpression));

        // Test AND expression without parentheses
        String andExpression = "version >= 2.3 && env == prod && region == cn-bj";
        List<ConditionMatcher> andMatchers = ExpressionParser.parse(andExpression);
        assertEquals(3, andMatchers.size());
        assertTrue(ExpressionParser.isAndExpression(andExpression));
    }

    /**
     * Test expressions with mixed parentheses - verify partial parentheses support
     */
    @Test
    public void testParseWithExpressionsWithMixedParentheses() {
        // Test OR expression with mixed parentheses
        String orExpression = "(version >= 2.3) || env == dev || (region == cn-bj)";
        List<ConditionMatcher> orMatchers = ExpressionParser.parse(orExpression);
        assertEquals(3, orMatchers.size());
        assertTrue(ExpressionParser.isOrExpression(orExpression));

        // Test AND expression with mixed parentheses
        String andExpression = "(version >= 2.3) && env == prod && (region == cn-bj)";
        List<ConditionMatcher> andMatchers = ExpressionParser.parse(andExpression);
        assertEquals(3, andMatchers.size());
        assertTrue(ExpressionParser.isAndExpression(andExpression));
    }

    /**
     * Test single expression with == operator - verify Java style equality
     */
    @Test
    public void testParseSingleExpressionWithDoubleEquals() {
        String expression = "env == prod";
        List<ConditionMatcher> matchers = ExpressionParser.parse(expression);
        assertEquals(1, matchers.size());
        assertFalse(ExpressionParser.isOrExpression(expression));
        assertFalse(ExpressionParser.isAndExpression(expression));
    }

    /**
     * Test OR expression detection - verify OR logic recognition
     */
    @Test
    public void testIsOrExpression() {
        // Test with double pipe ||
        assertTrue(ExpressionParser.isOrExpression("(version >= 2.3) || (env == dev)"));
        assertTrue(ExpressionParser.isOrExpression("version >= 2.3 || env == dev"));
        assertTrue(ExpressionParser.isOrExpression("(version >= 2.3) || (env == dev) || (region == cn-bj)"));

        // Test non-OR expressions
        assertFalse(ExpressionParser.isOrExpression("version >= 2.3"));
        assertFalse(ExpressionParser.isOrExpression("(version >= 2.3)"));
    }

    /**
     * Test AND expression detection - verify AND logic recognition
     */
    @Test
    public void testIsAndExpression() {
        assertTrue(ExpressionParser.isAndExpression("(version >= 2.3) && (env == prod)"));
        assertTrue(ExpressionParser.isAndExpression("version >= 2.3 && env == prod"));
        assertFalse(ExpressionParser.isAndExpression("version >= 2.3"));
        assertFalse(ExpressionParser.isAndExpression("(version >= 2.3)"));
    }

    /**
     * Test valid expressions - verify various valid formats
     */
    @Test
    public void testIsValidExpression() {
        // Single expressions
        assertTrue(ExpressionParser.isValidExpression("version >= 2.3"));
        assertTrue(ExpressionParser.isValidExpression("env == prod"));
        assertTrue(ExpressionParser.isValidExpression("env = prod"));
        assertTrue(ExpressionParser.isValidExpression("version != 1.0"));
        assertTrue(ExpressionParser.isValidExpression("key-1 == value"));
        assertTrue(ExpressionParser.isValidExpression("my_key == value"));

        // OR expressions
        assertTrue(ExpressionParser.isValidExpression("(version >= 2.3) || (env == dev)"));
        assertTrue(ExpressionParser.isValidExpression("version >= 2.3 || env == dev"));
        assertTrue(ExpressionParser.isValidExpression("(version >= 2.3) || env == dev"));
        assertTrue(ExpressionParser.isValidExpression("version >= 2.3 || (env == dev)"));

        // AND expressions
        assertTrue(ExpressionParser.isValidExpression("(version >= 2.3) && (env == prod)"));
        assertTrue(ExpressionParser.isValidExpression("version >= 2.3 && env == prod"));
        assertTrue(ExpressionParser.isValidExpression("(version >= 2.3) && env == prod"));
        assertTrue(ExpressionParser.isValidExpression("version >= 2.3 && (env == prod)"));

        // Invalid expressions
        assertFalse(ExpressionParser.isValidExpression(">= 2.3")); // Missing key name
        assertFalse(ExpressionParser.isValidExpression("version")); // Missing operator and value
        assertFalse(ExpressionParser.isValidExpression("version ==")); // Missing value
        assertFalse(ExpressionParser.isValidExpression("version >== 2.3")); // Invalid operator
        assertFalse(ExpressionParser.isValidExpression("()")); // Empty parentheses
        assertFalse(ExpressionParser.isValidExpression("key 1 == 1")); // Key name contains spaces
        assertFalse(ExpressionParser.isValidExpression("my key == value")); // Key name contains spaces
    }

    /**
     * Test parentheses handling logic - verify flexible parentheses support
     */
    @Test
    public void testParenthesesHandling() {
        // Both sides have parentheses
        assertTrue(ExpressionParser.isValidExpression("(version >= 2.3)"));
        // No parentheses
        assertTrue(ExpressionParser.isValidExpression("version >= 2.3"));
        // Only left parenthesis - treated as part of key-value
        assertTrue(ExpressionParser.isValidExpression("(version >= 2.3"));
        // Only right parenthesis - treated as part of key-value
        assertTrue(ExpressionParser.isValidExpression("version >= 2.3)"));
    }

    /**
     * Test parsing invalid expression throws exception - verify error handling
     */
    @Test
    public void testParseInvalidExpressionThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            ExpressionParser.parse(">= 2.3");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            ExpressionParser.parse("version");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            ExpressionParser.parse("version ==");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            ExpressionParser.parse("version >== 2.3");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            ExpressionParser.parse("()");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            ExpressionParser.parse("key 1 == 1");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            ExpressionParser.parse("my key == value");
        });
    }

    /**
     * Test mixed AND/OR logic - verify it's not supported
     */
    @Test
    public void testMixedAndOrLogicNotSupported() {
        // These should throw exceptions as mixed logic is not supported
        assertThrows(IllegalArgumentException.class, () -> {
            ExpressionParser.parse("(version >= 2.3) || (env == dev) && (region == cn-bj)");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            ExpressionParser.parse("(version >= 2.3) && (env == prod) || (region == cn-bj)");
        });
    }
}
