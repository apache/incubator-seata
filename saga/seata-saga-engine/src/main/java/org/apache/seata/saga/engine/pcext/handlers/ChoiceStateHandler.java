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
package org.apache.seata.saga.engine.pcext.handlers;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.seata.common.exception.FrameworkErrorCode;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.saga.engine.StateMachineConfig;
import org.apache.seata.saga.engine.exception.EngineExecutionException;
import org.apache.seata.saga.engine.expression.Expression;
import org.apache.seata.saga.engine.expression.ExpressionResolver;
import org.apache.seata.saga.engine.pcext.StateHandler;
import org.apache.seata.saga.engine.pcext.StateInstruction;
import org.apache.seata.saga.engine.pcext.utils.EngineUtils;
import org.apache.seata.saga.engine.utils.ExceptionUtils;
import org.apache.seata.saga.proctrl.ProcessContext;
import org.apache.seata.saga.statelang.domain.ChoiceState;
import org.apache.seata.saga.statelang.domain.DomainConstants;
import org.apache.seata.saga.statelang.domain.StateMachineInstance;
import org.apache.seata.saga.statelang.domain.impl.ChoiceStateImpl;

/**
 * ChoiceState Handler
 *
 */
public class ChoiceStateHandler implements StateHandler {

    @Override
    public void process(ProcessContext context) throws EngineExecutionException {

        StateInstruction instruction = context.getInstruction(StateInstruction.class);
        ChoiceStateImpl choiceState = (ChoiceStateImpl)instruction.getState(context);

        Map<Object, String> choiceEvaluators = choiceState.getChoiceEvaluators();
        if (choiceEvaluators == null) {
            synchronized (choiceState) {
                choiceEvaluators = choiceState.getChoiceEvaluators();
                if (choiceEvaluators == null) {

                    List<ChoiceState.Choice> choices = choiceState.getChoices();
                    if (choices == null) {
                        choiceEvaluators = new LinkedHashMap<>(0);
                    } else {
                        choiceEvaluators = new LinkedHashMap<>(choices.size());
                        ExpressionResolver resolver = ((StateMachineConfig) context.getVariable(
                                DomainConstants.VAR_NAME_STATEMACHINE_CONFIG)).getExpressionResolver();
                        for (ChoiceState.Choice choice : choices) {
                            Expression evaluator = resolver.getExpression(choice.getExpression());
                            choiceEvaluators.put(evaluator, choice.getNext());
                        }
                    }
                    choiceState.setChoiceEvaluators(choiceEvaluators);
                }
            }
        }

        Expression expression;
        for (Map.Entry<Object, String> entry : choiceEvaluators.entrySet()) {
            expression = (Expression) entry.getKey();
            if (Boolean.TRUE.equals(expression.getValue(context.getVariable(
                    DomainConstants.VAR_NAME_STATEMACHINE_CONTEXT)))) {
                context.setVariable(DomainConstants.VAR_NAME_CURRENT_CHOICE, entry.getValue());
                return;
            }
        }

        if (StringUtils.isEmpty(choiceState.getDefault())) {

            StateMachineInstance stateMachineInstance = (StateMachineInstance)context.getVariable(
                    DomainConstants.VAR_NAME_STATEMACHINE_INST);

            EngineExecutionException exception = ExceptionUtils.createEngineExecutionException(FrameworkErrorCode.StateMachineNoChoiceMatched, "No choice matched, maybe it is a bug. Choice state name: " + choiceState.getName(), stateMachineInstance, null);

            EngineUtils.failStateMachine(context, exception);

            throw exception;
        }

        context.setVariable(DomainConstants.VAR_NAME_CURRENT_CHOICE, choiceState.getDefault());
    }
}
