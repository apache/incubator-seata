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
package io.seata.saga.engine.pcext.handlers;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.seata.saga.engine.StateMachineConfig;
import io.seata.saga.engine.evaluation.Evaluator;
import io.seata.saga.engine.evaluation.EvaluatorFactory;
import io.seata.saga.engine.evaluation.EvaluatorFactoryManager;
import io.seata.saga.engine.exception.EngineExecutionException;
import io.seata.saga.engine.pcext.StateHandler;
import io.seata.saga.engine.pcext.StateInstruction;
import io.seata.saga.proctrl.ProcessContext;
import io.seata.saga.statelang.domain.ChoiceState;
import io.seata.saga.statelang.domain.DomainConstants;
import io.seata.saga.statelang.domain.impl.ChoiceStateImpl;

/**
 * ChoiceState Handler
 *
 * @author lorne.cl
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
                        for (ChoiceState.Choice choice : choices) {
                            Evaluator evaluator = getEvaluatorFactory(context).createEvaluator(choice.getExpression());
                            choiceEvaluators.put(evaluator, choice.getNext());
                        }
                    }
                    choiceState.setChoiceEvaluators(choiceEvaluators);
                }
            }
        }

        for (Object choiceEvaluatorObj : choiceEvaluators.keySet()) {
            Evaluator evaluator = (Evaluator)choiceEvaluatorObj;
            if (evaluator.evaluate(context.getVariables())) {
                context.setVariable(DomainConstants.VAR_NAME_CURRENT_CHOICE, choiceEvaluators.get(evaluator));
                return;
            }
        }

        context.setVariable(DomainConstants.VAR_NAME_CURRENT_CHOICE, choiceState.getDefault());
    }

    public EvaluatorFactory getEvaluatorFactory(ProcessContext context) {
        StateMachineConfig stateMachineConfig = (StateMachineConfig)context.getVariable(
            DomainConstants.VAR_NAME_STATEMACHINE_CONFIG);
        return stateMachineConfig.getEvaluatorFactoryManager().getEvaluatorFactory(
            EvaluatorFactoryManager.EVALUATOR_TYPE_DEFAULT);
    }
}