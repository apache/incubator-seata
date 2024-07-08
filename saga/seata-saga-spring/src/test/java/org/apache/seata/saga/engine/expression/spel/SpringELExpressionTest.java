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
package org.apache.seata.saga.engine.expression.spel;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * SpringELExpressionTest
 */
public class SpringELExpressionTest {
    @Test
    public void testGetValue() {
        ExpressionParser parser = new SpelExpressionParser();
        Expression defaultExpression = parser.parseExpression("'Hello World'.concat('!')");
        String value = (String) new SpringELExpression(defaultExpression).getValue(null);
        Assertions.assertEquals(value, "Hello World!");
    }

    @Test
    void testSetValue() {
        ExpressionParser parser = new SpelExpressionParser();
        String expression = "name";
        SpringELExpressionObject springELExpressionObject = new SpringELExpressionObject();
        Expression defaultExpression = parser.parseExpression(expression);
        SpringELExpression springELExpression = new SpringELExpression(defaultExpression);
        springELExpression.setValue("test", springELExpressionObject);
        Assertions.assertEquals(springELExpressionObject.getName(), "test");
    }

    @Test
    void testGetExpressionString() {
        ExpressionParser parser = new SpelExpressionParser();
        Expression defaultExpression = parser.parseExpression("'Hello World'.concat('!')");
        SpringELExpression springELExpression = new SpringELExpression(defaultExpression);
        Assertions.assertEquals(springELExpression.getExpressionString(), "'Hello World'.concat('!')");
    }
}