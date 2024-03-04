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
package org.apache.seata.saga.engine.expression.exception;

import org.apache.seata.common.exception.FrameworkErrorCode;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.saga.engine.exception.EngineExecutionException;
import org.apache.seata.saga.engine.expression.Expression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Exception match evaluator expression
 *
 */
public class ExceptionMatchExpression implements Expression {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionMatchExpression.class);
    private String expressionString;
    private Class<Exception> exceptionClass;

    @Override
    public Object getValue(Object elContext) {
        if (elContext instanceof Exception && StringUtils.hasText(expressionString)) {

            Exception e = (Exception) elContext;

            String exceptionClassName = e.getClass().getName();
            if (exceptionClassName.equals(expressionString)) {
                return true;
            }
            try {
                if (exceptionClass.isAssignableFrom(e.getClass())) {
                    return true;
                }
            } catch (Exception e1) {
                LOGGER.error("Exception Match failed. expression[{}]", expressionString, e1);
            }
        }

        return false;
    }

    @Override
    public void setValue(Object value, Object elContext) {

    }

    @Override
    public String getExpressionString() {
        return expressionString;
    }

    @SuppressWarnings("unchecked")
    public void setExpressionString(String expressionString) {
        this.expressionString = expressionString;
        try {
            this.exceptionClass = (Class<Exception>) Class.forName(expressionString);
        } catch (ClassNotFoundException e) {
            throw new EngineExecutionException(e, expressionString + " is not a Exception Class",
                    FrameworkErrorCode.NotExceptionClass);
        }
    }
}
