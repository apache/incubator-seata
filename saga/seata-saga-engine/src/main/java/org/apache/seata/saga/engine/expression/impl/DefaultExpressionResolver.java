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
package org.apache.seata.saga.engine.expression.impl;

import org.apache.seata.saga.engine.expression.Expression;
import org.apache.seata.saga.engine.expression.ExpressionFactory;
import org.apache.seata.saga.engine.expression.ExpressionFactoryManager;
import org.apache.seata.saga.engine.expression.ExpressionResolver;

/**
 * Default {@link ExpressionResolver} implementation
 *
 */
public class DefaultExpressionResolver implements ExpressionResolver {

    protected static class ExpressionStruct {
        int typeStart;

        int typeEnd;

        int end;

        String type;

        String content;
    }

    private ExpressionFactoryManager expressionFactoryManager;

    @Override
    public Expression getExpression(String expressionStr) {
        ExpressionStruct struct = parseExpressionStruct(expressionStr);

        ExpressionFactory expressionFactory = expressionFactoryManager.getExpressionFactory(struct.type);
        if (expressionFactory == null) {
            throw new IllegalArgumentException("Cannot get ExpressionFactory by Type[" + struct + "]");
        }
        return expressionFactory.createExpression(struct.content);
    }

    protected ExpressionStruct parseExpressionStruct(String expressionStr) {
        ExpressionStruct struct = new ExpressionStruct();
        struct.typeStart = expressionStr.indexOf("$");
        int dot = expressionStr.indexOf(".", struct.typeStart);
        int leftBracket = expressionStr.indexOf("{", struct.typeStart);


        boolean isOldEvaluatorStyle = false;
        if (struct.typeStart == 0) {
            if (leftBracket < 0 && dot < 0) {
                throw new IllegalArgumentException(String.format("Expression [%s] type is not closed", expressionStr));
            }
            // Backward compatible for structure: $expressionType{expressionContent}
            if (leftBracket > 0 && (leftBracket < dot || dot < 0)) {
                struct.typeEnd = leftBracket;
                isOldEvaluatorStyle = true;
            }
            if (dot > 0 && (dot < leftBracket || leftBracket < 0)) {
                struct.typeEnd = dot;
            }
        }

        if (struct.typeStart == 0 && leftBracket != -1 && leftBracket < dot) {
            // Backward compatible for structure: $expressionType{expressionContent}
            struct.typeEnd = expressionStr.indexOf("{", struct.typeStart);
            isOldEvaluatorStyle = true;
        }

        // No $ indicator denotes default type
        if (struct.typeStart != 0) {
            struct.typeStart = struct.typeEnd = -1;
            struct.type = null;
        } else {
            struct.type = expressionStr.substring(struct.typeStart + 1, struct.typeEnd);
        }

        if (isOldEvaluatorStyle) {
            struct.end = expressionStr.indexOf("}", struct.typeEnd);
        } else {
            struct.end = expressionStr.length();
        }

        struct.content = expressionStr.substring(struct.typeEnd + 1, struct.end);

        return struct;
    }

    @Override
    public ExpressionFactoryManager getExpressionFactoryManager() {
        return expressionFactoryManager;
    }

    @Override
    public void setExpressionFactoryManager(ExpressionFactoryManager expressionFactoryManager) {
        this.expressionFactoryManager = expressionFactoryManager;
    }
}
