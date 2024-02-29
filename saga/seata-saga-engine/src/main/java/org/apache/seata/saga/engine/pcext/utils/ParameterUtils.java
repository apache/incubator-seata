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
package org.apache.seata.saga.engine.pcext.utils;

import org.apache.seata.common.util.CollectionUtils;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.saga.engine.expression.Expression;
import org.apache.seata.saga.engine.expression.ExpressionResolver;
import org.apache.seata.saga.engine.expression.seq.SequenceExpression;
import org.apache.seata.saga.statelang.domain.StateInstance;
import org.apache.seata.saga.statelang.domain.impl.AbstractTaskState;
import org.apache.seata.saga.statelang.domain.impl.StateInstanceImpl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * ParameterUtils
 *
 */
public class ParameterUtils {

    public static List<Object> createInputParams(ExpressionResolver expressionResolver,
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
                        inputExpressions.add(createValueExpression(expressionResolver, inputAssignment));
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

    public static Map<String, Object> createOutputParams(ExpressionResolver expressionResolver,
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
                                createValueExpression(expressionResolver, entry.getValue()));
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

    public static Object createValueExpression(ExpressionResolver expressionResolver,
                                               Object paramAssignment) {

        Object valueExpression;

        if (paramAssignment instanceof Expression) {
            valueExpression = paramAssignment;
        } else if (paramAssignment instanceof Map) {
            Map<String, Object> paramMapAssignment = (Map<String, Object>)paramAssignment;
            Map<String, Object> paramMap = new LinkedHashMap<>(paramMapAssignment.size());
            paramMapAssignment.forEach((paramName, valueAssignment) -> {
                paramMap.put(paramName, createValueExpression(expressionResolver, valueAssignment));
            });
            valueExpression = paramMap;
        } else if (paramAssignment instanceof List) {
            List<Object> paramListAssignment = (List<Object>)paramAssignment;
            List<Object> paramList = new ArrayList<>(paramListAssignment.size());
            for (Object aParamAssignment : paramListAssignment) {
                paramList.add(createValueExpression(expressionResolver, aParamAssignment));
            }
            valueExpression = paramList;
        } else if (paramAssignment instanceof String && ((String)paramAssignment).startsWith("$")) {
            valueExpression = expressionResolver.getExpression((String) paramAssignment);
        } else {
            valueExpression = paramAssignment;
        }
        return valueExpression;
    }
}
