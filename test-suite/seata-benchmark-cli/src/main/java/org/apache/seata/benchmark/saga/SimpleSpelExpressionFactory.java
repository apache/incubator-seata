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
package org.apache.seata.benchmark.saga;

import org.apache.seata.saga.engine.expression.Expression;
import org.apache.seata.saga.engine.expression.ExpressionFactory;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

/**
 * Simple Spring EL expression factory for benchmark testing.
 * Does not require Spring ApplicationContext.
 */
public class SimpleSpelExpressionFactory implements ExpressionFactory {

    private final ExpressionParser parser = new SpelExpressionParser();

    @Override
    public Expression createExpression(String expression) {
        org.springframework.expression.Expression spelExpression = parser.parseExpression(expression);
        return new SimpleSpelExpression(spelExpression);
    }

    /**
     * Simple SpEL expression wrapper
     */
    private static class SimpleSpelExpression implements Expression {

        private final org.springframework.expression.Expression spelExpression;

        public SimpleSpelExpression(org.springframework.expression.Expression spelExpression) {
            this.spelExpression = spelExpression;
        }

        @Override
        public Object getValue(Object elContext) {
            // Create evaluation context with MapAccessor for property access on Maps
            StandardEvaluationContext evalContext = new StandardEvaluationContext(elContext);
            evalContext.addPropertyAccessor(
                    new org.springframework.expression.spel.support.ReflectivePropertyAccessor());
            evalContext.addPropertyAccessor(new org.springframework.context.expression.MapAccessor());
            return spelExpression.getValue(evalContext);
        }

        @Override
        public void setValue(Object value, Object elContext) {
            StandardEvaluationContext evalContext = new StandardEvaluationContext(elContext);
            evalContext.addPropertyAccessor(
                    new org.springframework.expression.spel.support.ReflectivePropertyAccessor());
            evalContext.addPropertyAccessor(new org.springframework.context.expression.MapAccessor());
            spelExpression.setValue(evalContext, value);
        }

        @Override
        public String getExpressionString() {
            return spelExpression.getExpressionString();
        }
    }
}
