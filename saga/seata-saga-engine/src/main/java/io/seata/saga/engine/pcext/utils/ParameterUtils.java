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
package io.seata.saga.engine.pcext.utils;

import io.seata.common.util.CollectionUtils;
import io.seata.saga.engine.expression.Expression;
import io.seata.saga.engine.expression.ExpressionFactory;
import io.seata.saga.engine.expression.ExpressionFactoryManager;
import io.seata.saga.engine.expression.seq.SequenceExpression;
import io.seata.saga.statelang.domain.StateInstance;
import io.seata.saga.statelang.domain.impl.AbstractTaskState;
import io.seata.saga.statelang.domain.impl.StateInstanceImpl;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * ParameterUtils
 *
 * @author lorne.cl
 */
public class ParameterUtils {

    public static List<Object> createInputParams(ExpressionFactoryManager expressionFactoryManager,
                                                 StateInstanceImpl stateInstance,
                                                 AbstractTaskState serviceTaskState, Object variablesFrom) {
        List<Object> inputAssignments = serviceTaskState.getInput();
        if (CollectionUtils.isEmpty(inputAssignments)) {
            return new ArrayList<>(0);
        }

        List<Object> inputExpressions = serviceTaskState.getInputExpressions();
        if (inputExpressions == null) {
            synchronized (serviceTaskState) {
                inputExpressions = serviceTaskState.getInputExpressions();
                if (inputExpressions == null) {
                    inputExpressions = new ArrayList<>(inputAssignments.size());
                    for (Object inputAssignment : inputAssignments) {
                        inputExpressions.add(createValueExpression(expressionFactoryManager, inputAssignment));
                    }
                }
                serviceTaskState.setInputExpressions(inputExpressions);
            }
        }
        List<Object> inputValues = new ArrayList<>(inputExpressions.size());
        for (Object valueExpression : inputExpressions) {
            Object value = getValue(valueExpression, variablesFrom, stateInstance);
            inputValues.add(value);
        }

        return inputValues;
    }

    public static Map<String, Object> createOutputParams(ExpressionFactoryManager expressionFactoryManager,
                                                         AbstractTaskState serviceTaskState, Object variablesFrom) {
        Map<String, Object> outputAssignments = serviceTaskState.getOutput();
        if (CollectionUtils.isEmpty(outputAssignments)) {
            return new LinkedHashMap<>(0);
        }

        Map<String, Object> outputExpressions = serviceTaskState.getOutputExpressions();
        if (outputExpressions == null) {
            synchronized (serviceTaskState) {
                outputExpressions = serviceTaskState.getOutputExpressions();
                if (outputExpressions == null) {
                    outputExpressions = new LinkedHashMap<>(outputAssignments.size());
                    for (Map.Entry<String, Object> entry : outputAssignments.entrySet()) {
                        outputExpressions.put(entry.getKey(),
                                createValueExpression(expressionFactoryManager, entry.getValue()));
                    }
                }
                serviceTaskState.setOutputExpressions(outputExpressions);
            }
        }
        Map<String, Object> outputValues = new LinkedHashMap<>(outputExpressions.size());
        for (String paramName : outputExpressions.keySet()) {
            outputValues.put(paramName, getValue(outputExpressions.get(paramName), variablesFrom, null));
        }
        return outputValues;
    }

    public static Object getValue(Object valueExpression, Object variablesFrom, StateInstance stateInstance) {
        if (valueExpression instanceof Expression) {
            Object value = ((Expression)valueExpression).getValue(variablesFrom);
            if (value != null && stateInstance != null && StringUtils.isEmpty(stateInstance.getBusinessKey())
                    && valueExpression instanceof SequenceExpression) {
                stateInstance.setBusinessKey(String.valueOf(value));
            }
            return value;
        } else if (valueExpression instanceof Map) {
            Map<String, Object> mapValueExpression = (Map<String, Object>)valueExpression;
            Map<String, Object> mapValue = new LinkedHashMap<>();
            mapValueExpression.forEach((key, value) -> {
                value = getValue(value, variablesFrom, stateInstance);
                if (value != null) {
                    mapValue.put(key, value);
                }
            });
            return mapValue;
        } else if (valueExpression instanceof List) {
            List<Object> listValueExpression = (List<Object>)valueExpression;
            List<Object> listValue = new ArrayList<>(listValueExpression.size());
            for (Object aValueExpression : listValueExpression) {
                listValue.add(getValue(aValueExpression, variablesFrom, stateInstance));
            }
            return listValue;
        } else {
            return valueExpression;
        }
    }

    public static Object createValueExpression(ExpressionFactoryManager expressionFactoryManager,
                                                Object paramAssignment) {

        Object valueExpression;

        if (paramAssignment instanceof Expression) {
            valueExpression = paramAssignment;
        } else if (paramAssignment instanceof Map) {
            Map<String, Object> paramMapAssignment = (Map<String, Object>)paramAssignment;
            Map<String, Object> paramMap = new LinkedHashMap<>(paramMapAssignment.size());
            paramMapAssignment.forEach((paramName, valueAssignment) -> {
                paramMap.put(paramName, createValueExpression(expressionFactoryManager, valueAssignment));
            });
            valueExpression = paramMap;
        } else if (paramAssignment instanceof List) {
            List<Object> paramListAssignment = (List<Object>)paramAssignment;
            List<Object> paramList = new ArrayList<>(paramListAssignment.size());
            for (Object aParamAssignment : paramListAssignment) {
                paramList.add(createValueExpression(expressionFactoryManager, aParamAssignment));
            }
            valueExpression = paramList;
        } else if (paramAssignment instanceof String && ((String)paramAssignment).startsWith("$")) {

            String expressionStr = (String)paramAssignment;
            int expTypeStart = expressionStr.indexOf("$");
            int expTypeEnd = expressionStr.indexOf(".", expTypeStart);

            String expressionType = null;
            if (expTypeStart >= 0 && expTypeEnd > expTypeStart) {
                expressionType = expressionStr.substring(expTypeStart + 1, expTypeEnd);
            }

            int expEnd = expressionStr.length();
            String expressionContent = null;
            if (expTypeEnd > 0 && expEnd > expTypeEnd) {
                expressionContent = expressionStr.substring(expTypeEnd + 1, expEnd);
            }

            ExpressionFactory expressionFactory = expressionFactoryManager.getExpressionFactory(expressionType);
            if (expressionFactory == null) {
                throw new IllegalArgumentException("Cannot get ExpressionFactory by Type[" + expressionType + "]");
            }
            valueExpression = expressionFactory.createExpression(expressionContent);
        } else {
            valueExpression = paramAssignment;
        }
        return valueExpression;
    }
}