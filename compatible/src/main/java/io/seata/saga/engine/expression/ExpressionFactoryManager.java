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
package io.seata.saga.engine.expression;

import java.util.AbstractMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Expression factory manager
 */
@Deprecated
public class ExpressionFactoryManager {

    private final org.apache.seata.saga.engine.expression.ExpressionFactoryManager actual;

    public static final String DEFAULT_EXPRESSION_TYPE = "Default";

    private ExpressionFactoryManager(org.apache.seata.saga.engine.expression.ExpressionFactoryManager actual) {
        this.actual = actual;
    }

    public ExpressionFactoryManager() {
        actual = new org.apache.seata.saga.engine.expression.ExpressionFactoryManager();
    }


    public ExpressionFactory getExpressionFactory(String expressionType) {
        org.apache.seata.saga.engine.expression.ExpressionFactory expressionFactory = actual.getExpressionFactory(expressionType);
        return expressionFactory::createExpression;
    }

    public void setExpressionFactoryMap(Map<String, ExpressionFactory> expressionFactoryMap) {
        Map<String, org.apache.seata.saga.engine.expression.ExpressionFactory> actualExpressionFactoryMap = expressionFactoryMap.entrySet().stream()
                .map(e -> new AbstractMap.SimpleEntry<String, org.apache.seata.saga.engine.expression.ExpressionFactory>(e.getKey(), e.getValue()))
                .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));
        actual.setExpressionFactoryMap(actualExpressionFactoryMap);
    }

    public void putExpressionFactory(String type, ExpressionFactory factory) {
        actual.putExpressionFactory(type, factory);
    }

    public org.apache.seata.saga.engine.expression.ExpressionFactoryManager unwrap() {
        return actual;
    }

    public static ExpressionFactoryManager wrap(org.apache.seata.saga.engine.expression.ExpressionFactoryManager actual) {
        return new ExpressionFactoryManager(actual);
    }
}
