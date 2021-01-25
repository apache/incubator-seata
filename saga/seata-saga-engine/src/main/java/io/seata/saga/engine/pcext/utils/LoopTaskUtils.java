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

import java.util.Collection;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import io.seata.common.util.NumberUtils;
import io.seata.common.util.StringUtils;
import io.seata.saga.engine.StateMachineConfig;
import io.seata.saga.engine.evaluation.EvaluatorFactoryManager;
import io.seata.saga.engine.evaluation.expression.ExpressionEvaluator;
import io.seata.saga.engine.pcext.StateInstruction;
import io.seata.saga.proctrl.ProcessContext;
import io.seata.saga.proctrl.impl.ProcessContextImpl;
import io.seata.saga.statelang.domain.DomainConstants;
import io.seata.saga.statelang.domain.ExecutionStatus;
import io.seata.saga.statelang.domain.State;
import io.seata.saga.statelang.domain.StateInstance;
import io.seata.saga.statelang.domain.StateMachine;
import io.seata.saga.statelang.domain.StateMachineInstance;
import io.seata.saga.statelang.domain.TaskState.Loop;
import io.seata.saga.statelang.domain.impl.AbstractTaskState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loop Task Util
 *
 * @author anselleeyy
 */
public class LoopTaskUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoopTaskUtils.class);

    private static final String DEFAULT_COMPLETION_CONDITION = "[nrOfInstances] == [nrOfCompletedInstances]";
    public static final String LOOP_STATE_NAME_PATTERN = "-loop-";

    private static final Map<String, ExpressionEvaluator> EXPRESSION_EVALUATOR_MAP = new ConcurrentHashMap<>();

    /**
     * get Loop Config from State
     *
     * @param context
     * @param currentState
     * @return currentState loop config if satisfied, else {@literal null}
     */
    public static Loop getLoopConfig(ProcessContext context, State currentState) {
        if (matchLoop(currentState)) {
            AbstractTaskState taskState = (AbstractTaskState)currentState;

            StateMachineInstance stateMachineInstance = (StateMachineInstance)context.getVariable(
                DomainConstants.VAR_NAME_STATEMACHINE_INST);
            StateMachineConfig stateMachineConfig = (StateMachineConfig)context.getVariable(
                DomainConstants.VAR_NAME_STATEMACHINE_CONFIG);

            if (null != taskState.getLoop()) {
                Loop loop = taskState.getLoop();
                String collectionName = loop.getCollection();
                if (StringUtils.isNotBlank(collectionName)) {
                    Object expression = ParameterUtils.createValueExpression(
                        stateMachineConfig.getExpressionFactoryManager(), collectionName);
                    Object collection = ParameterUtils.getValue(expression, stateMachineInstance.getContext(), null);
                    if (collection instanceof Collection && ((Collection)collection).size() > 0) {
                        LoopContextHolder.getCurrent(context, true).setCollection((Collection)collection);
                        return loop;
                    }
                }
                LOGGER.warn("State [{}] loop collection param [{}] invalid", currentState.getName(), collectionName);
            }
        }
        return null;
    }

    /**
     * create loop context for async publisher
     *
     * @param context
     */
    public static void createLoopContext(ProcessContext context) {
        LoopContextHolder contextHolder = LoopContextHolder.getCurrent(context, true);
        Collection collection = contextHolder.getCollection();
        contextHolder.getNrOfInstances().set(collection.size());

        for (int i = collection.size() - 1; i >= 0; i--) {
            contextHolder.getLoopIndexStack().push(i);
        }
    }

    public static ProcessContext createLoopEventContext(ProcessContext context) {
        ProcessContextImpl copyContext = new ProcessContextImpl();
        copyContext.setParent(context);
        copyContext.setVariableLocally(DomainConstants.VAR_NAME_IS_LOOP_STATE, true);
        copyContext.setVariableLocally(DomainConstants.LOOP_COUNTER, acquireNextLoopCounter(context));
        copyContext.setInstruction(copyInstruction(context.getInstruction(StateInstruction.class)));
        return copyContext;
    }

    public static ProcessContext createCompensateLoopEventContext(ProcessContext context, StateInstance stateToBeCompensated) {
        Stack<StateInstance> stateStackToBeCompensated = CompensationHolder.getCurrent(context, true)
            .getStateStackNeedCompensation();
        int loopCounter = reloadLoopCounter(stateToBeCompensated.getName());
        ProcessContextImpl copyContext = new ProcessContextImpl();
        CompensationHolder.getCurrent(copyContext, true);
        copyContext.setParent(context);
        copyContext.setVariableLocally(DomainConstants.VAR_NAME_IS_LOOP_STATE, true);
        copyContext.setVariableLocally(DomainConstants.LOOP_COUNTER, loopCounter);
        copyContext.setVariableLocally(DomainConstants.VAR_NAME_CURRENT_COMPEN_TRIGGER_STATE,
            context.getVariable(DomainConstants.VAR_NAME_CURRENT_COMPEN_TRIGGER_STATE));
        copyContext.setInstruction(copyInstruction(context.getInstruction(StateInstruction.class)));
        StateInstruction instruction = copyContext.getInstruction(StateInstruction.class);
        CompensationHolder.getCurrent(copyContext, true).addToBeCompensatedState(instruction.getStateName(),
            stateToBeCompensated);
        CompensationHolder.getCurrent(copyContext, true).setStateStackNeedCompensation(stateStackToBeCompensated);
        return copyContext;
    }

    /**
     * reload loop context while forward
     *
     * @param context
     * @param forwardStateName
     */
    public static void reloadLoopContext(ProcessContext context, String forwardStateName) {

        StateMachineInstance stateMachineInstance = (StateMachineInstance)context.getVariable(
            DomainConstants.VAR_NAME_STATEMACHINE_INST);

        List<StateInstance> actList = stateMachineInstance.getStateList();
        List<StateInstance> forwardStateList = actList.stream().filter(
            e -> forwardStateName.equals(EngineUtils.getOriginStateName(e))).collect(Collectors.toList());

        LoopContextHolder loopContextHolder = LoopContextHolder.getCurrent(context, true);
        Collection collection = loopContextHolder.getCollection();

        LinkedList<Integer> list = new LinkedList<>();
        for (int i = 0; i < collection.size(); i++) {
            list.addFirst(i);
        }

        int executedNumber = 0;
        for (StateInstance stateInstance : forwardStateList) {
            if (ExecutionStatus.SU.equals(stateInstance.getStatus())) {
                list.remove(Integer.valueOf(reloadLoopCounter(stateInstance.getName())));
                executedNumber += 1;
            }
        }

        loopContextHolder.getLoopIndexStack().addAll(list);
        loopContextHolder.getNrOfInstances().set(collection.size());
        loopContextHolder.getNrOfCompletedInstances().set(executedNumber);
    }

    /**
     * create loop context for compensate
     *
     * @param context
     * @param stateToBeCompensated
     */
    public static void createCompensateContext(ProcessContext context, StateInstance stateToBeCompensated) {

        StateMachine stateMachine = (StateMachine)context.getVariable(DomainConstants.VAR_NAME_STATEMACHINE);
        State state = stateMachine.getState(EngineUtils.getOriginStateName(stateToBeCompensated));

        LoopContextHolder loopContextHolder = LoopContextHolder.getCurrent(context, true);

        Stack<StateInstance> stateStackToBeCompensated = CompensationHolder.getCurrent(context, true)
            .getStateStackNeedCompensation();
        int sameStateNeedToBeCompensatedSize = 1;
        for (StateInstance stateInstance : stateStackToBeCompensated) {
            if (state.getName().equals(EngineUtils.getOriginStateName(stateInstance))) {
                sameStateNeedToBeCompensatedSize += 1;
            }
        }
        loopContextHolder.getNrOfInstances().set(sameStateNeedToBeCompensatedSize);
        stateStackToBeCompensated.push(stateToBeCompensated);

    }

    /**
     * match if state has loop property
     *
     * @param state
     * @return
     */
    public static boolean matchLoop(State state) {
        return state != null && (DomainConstants.STATE_TYPE_SERVICE_TASK.equals(state.getType())
            || DomainConstants.STATE_TYPE_SCRIPT_TASK.equals(state.getType())
            || DomainConstants.STATE_TYPE_SUB_STATE_MACHINE.equals(state.getType()));
    }

    public static StateInstance findOutLastNeedForwardStateInstance(ProcessContext context) {
        StateMachineInstance stateMachineInstance = (StateMachineInstance)context.getVariable(
            DomainConstants.VAR_NAME_STATEMACHINE_INST);
        StateInstance lastForwardState = (StateInstance)context.getVariable(DomainConstants.VAR_NAME_STATE_INST);

        List<StateInstance> actList = stateMachineInstance.getStateList();
        for (int i = actList.size() - 1; i >= 0; i--) {
            StateInstance stateInstance = actList.get(i);
            if (EngineUtils.getOriginStateName(stateInstance).equals(EngineUtils.getOriginStateName(lastForwardState))
                && !ExecutionStatus.SU.equals(stateInstance.getStatus())) {
                return stateInstance;
            }
        }
        return lastForwardState;
    }

    public static StateInstance reloadLastRetriedStateInstance(StateMachineInstance stateMachineInstance,
                                                               String stateName) {
        List<StateInstance> actList = stateMachineInstance.getStateList();
        for (int i = actList.size() - 1; i >= 0; i--) {
            StateInstance stateInstance = actList.get(i);
            if (stateInstance.getName().equals(stateName)) {
                return stateInstance;
            }
        }
        return null;
    }

    /**
     * check if satisfied completion condition
     *
     * @param context
     * @return
     */
    public static boolean isCompletionConditionSatisfied(ProcessContext context) {

        if (!LoopContextHolder.getCurrent(context, true).getLoopExpContext().isEmpty()) {
            return true;
        }

        StateInstruction instruction = context.getInstruction(StateInstruction.class);
        AbstractTaskState currentState = (AbstractTaskState)instruction.getState(context);

        Map<String, Object> elContext = new HashMap<>(3);

        int nrOfInstances = LoopContextHolder.getCurrent(context, true).getNrOfInstances().get();
        int nrOfActiveInstances = LoopContextHolder.getCurrent(context, true).getNrOfActiveInstances().get();
        int nrOfCompletedInstances = LoopContextHolder.getCurrent(context, true).getNrOfCompletedInstances().get();

        State compensationTriggerState = (State)context.getVariable(DomainConstants.VAR_NAME_CURRENT_COMPEN_TRIGGER_STATE);
        if (null != compensationTriggerState) {
            return nrOfInstances <= 0;
        }

        elContext.put(DomainConstants.NUMBER_OF_INSTANCES, nrOfInstances);
        elContext.put(DomainConstants.NUMBER_OF_ACTIVE_INSTANCES, (double)nrOfActiveInstances);
        elContext.put(DomainConstants.NUMBER_OF_COMPLETED_INSTANCES, (double)nrOfCompletedInstances);

        return nrOfCompletedInstances >= nrOfInstances || getEvaluator(context,
            currentState.getLoop().getCompletionCondition()).evaluate(elContext);
    }

    public static int acquireNextLoopCounter(ProcessContext context) {
        try {
            return LoopContextHolder.getCurrent(context, true).getLoopIndexStack().pop();
        } catch (EmptyStackException e) {
            return -1;
        }
    }

    /**
     * generate loop state name like stateName-fork-1
     *
     * @param stateName
     * @param context
     * @return
     */
    public static String generateLoopStateName(ProcessContext context, String stateName) {
        if (StringUtils.isNotBlank(stateName)) {
            int loopCounter = (int)context.getVariable(DomainConstants.LOOP_COUNTER);
            return stateName + LOOP_STATE_NAME_PATTERN + loopCounter;
        }
        return stateName;
    }

    /**
     * reload context loop counter from stateInstName
     *
     * @param stateName
     * @return
     * @see #generateLoopStateName(ProcessContext, String)
     */
    public static int reloadLoopCounter(String stateName) {
        if (StringUtils.isNotBlank(stateName)) {
            int end = stateName.lastIndexOf(LOOP_STATE_NAME_PATTERN);
            if (end > -1) {
                String loopCounter = stateName.substring(end + LOOP_STATE_NAME_PATTERN.length());
                return NumberUtils.toInt(loopCounter, -1);
            }
        }
        return -1;
    }

    public static boolean needCompensate(ProcessContext context) {
        return !context.getVariable(DomainConstants.VAR_NAME_OPERATION_NAME).equals(DomainConstants.OPERATION_NAME_COMPENSATE)
                && LoopContextHolder.getCurrent(context, true).isNeedCompensate();
    }

    /**
     * get loop completion condition evaluator
     *
     * @param context
     * @param completionCondition
     * @return
     */
    private static ExpressionEvaluator getEvaluator(ProcessContext context, String completionCondition) {
        if (StringUtils.isBlank(completionCondition)) {
            completionCondition = DEFAULT_COMPLETION_CONDITION;
        }
        if (!EXPRESSION_EVALUATOR_MAP.containsKey(completionCondition)) {
            StateMachineConfig stateMachineConfig = (StateMachineConfig)context.getVariable(
                DomainConstants.VAR_NAME_STATEMACHINE_CONFIG);
            ExpressionEvaluator expressionEvaluator = (ExpressionEvaluator)stateMachineConfig
                .getEvaluatorFactoryManager().getEvaluatorFactory(EvaluatorFactoryManager.EVALUATOR_TYPE_DEFAULT)
                .createEvaluator(completionCondition);
            expressionEvaluator.setRootObjectName(null);
            EXPRESSION_EVALUATOR_MAP.put(completionCondition, expressionEvaluator);
        }
        return EXPRESSION_EVALUATOR_MAP.get(completionCondition);
    }

    private static StateInstruction copyInstruction(StateInstruction instruction) {
        StateInstruction targetInstruction = new StateInstruction();
        targetInstruction.setStateMachineName(instruction.getStateMachineName());
        targetInstruction.setTenantId(instruction.getTenantId());
        targetInstruction.setStateName(instruction.getStateName());
        targetInstruction.setTemporaryState(instruction.getTemporaryState());
        return targetInstruction;
    }

}