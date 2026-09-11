/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.saga.engine.evaluation.expression;

import java.util.Map;

import io.seata.common.exception.FrameworkErrorCode;
import io.seata.saga.engine.evaluation.Evaluator;
import io.seata.saga.engine.exception.EngineExecutionException;
import io.seata.saga.engine.expression.Expression;
import io.seata.saga.statelang.domain.DomainConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 * Expression evaluator
 *
 * @author lorne.cl
 */
public class ExpressionEvaluator implements Evaluator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExpressionEvaluator.class);

    private Expression expression;

    /**
     * If it is empty, use variables as the root variable, otherwise take rootObjectName as the root.
     */
    private String rootObjectName = DomainConstants.VAR_NAME_STATEMACHINE_CONTEXT;

    @Override
    public boolean evaluate(Map<String, Object> variables) {

        Object rootObject;
        if (StringUtils.hasText(this.rootObjectName)) {
            rootObject = variables.get(this.rootObjectName);
        } else {
            rootObject = variables;
        }

        Object result;
        try {
            result = expression.getValue(rootObject);
        } catch (Exception e) {
            result = Boolean.FALSE;
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("Expression [{}] execute failed, and it will return false by default. variables:{}",
                    expression.getExpressionString(), variables, e);
            }
        }

        if (result == null) {
            throw new EngineExecutionException("Evaluation returns null", FrameworkErrorCode.EvaluationReturnsNull);
        }
        if (!(result instanceof Boolean)) {
            throw new EngineExecutionException(
                "Evaluation returns non-Boolean: " + result + " (" + result.getClass().getName() + ")",
                FrameworkErrorCode.EvaluationReturnsNonBoolean);
        }
        return (Boolean)result;
    }

    public Expression getExpression() {
        return expression;
    }

    public void setExpression(Expression expression) {
        this.expression = expression;
    }

    public String getRootObjectName() {
        return rootObjectName;
    }

    public void setRootObjectName(String rootObjectName) {
        this.rootObjectName = rootObjectName;
    }
}