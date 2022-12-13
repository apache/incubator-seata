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

import java.util.ArrayList;
import java.util.Collection;
import java.util.EmptyStackException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import io.seata.common.exception.FrameworkErrorCode;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.NumberUtils;
import io.seata.common.util.StringUtils;
import io.seata.saga.engine.StateMachineConfig;
import io.seata.saga.engine.evaluation.EvaluatorFactoryManager;
import io.seata.saga.engine.evaluation.expression.ExpressionEvaluator;
import io.seata.saga.engine.exception.ForwardInvalidException;
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
     * @param context the process context
     * @param currentState the task state
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
     * match if state has loop property
     *
     * @param state the state
     * @return the boolean
     */
    public static boolean matchLoop(State state) {
        return state != null && (DomainConstants.STATE_TYPE_SERVICE_TASK.equals(state.getType())
            || DomainConstants.STATE_TYPE_SCRIPT_TASK.equals(state.getType())
            || DomainConstants.STATE_TYPE_SUB_STATE_MACHINE.equals(state.getType()));
    }

    /**
     * create loop counter context
     *
     * @param context the process context
     */
    public static void createLoopCounterContext(ProcessContext context) {
        LoopContextHolder contextHolder = LoopContextHolder.getCurrent(context, true);
        Collection collection = contextHolder.getCollection();
        contextHolder.getNrOfInstances().set(collection.size());

        for (int i = collection.size() - 1; i >= 0; i--) {
            contextHolder.getLoopCounterStack().push(i);
        }
    }


    /**
     * reload loop counter context while forward
     *
     * @param context the process context
     * @param forwardStateName the forward state name
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
        LinkedList<Integer> failEndList = new LinkedList<>();
        for (StateInstance stateInstance : forwardStateList) {
            if (!stateInstance.isIgnoreStatus()) {
                if (ExecutionStatus.SU.equals(stateInstance.getStatus())) {
                    executedNumber += 1;
                } else {
                    stateInstance.setIgnoreStatus(true);
                    failEndList.addFirst(reloadLoopCounter(stateInstance.getName()));
                }
                list.remove(Integer.valueOf(reloadLoopCounter(stateInstance.getName())));
            }
        }

        loopContextHolder.getLoopCounterStack().addAll(list);
        loopContextHolder.getForwardCounterStack().addAll(failEndList);
        loopContextHolder.getNrOfInstances().set(collection.size());
        loopContextHolder.getNrOfCompletedInstances().set(executedNumber);
    }

    /**
     * create context for async publish
     *
     * @param context the process context
     * @param loopCounter acquire new counter if is -1, else means a specific loop-counter
     * @return the process context
     */
    public static ProcessContext createLoopEventContext(ProcessContext context, int loopCounter) {
        ProcessContextImpl copyContext = new ProcessContextImpl();
        copyContext.setParent(context);
        copyContext.setVariableLocally(DomainConstants.LOOP_COUNTER, loopCounter >= 0 ? loopCounter : acquireNextLoopCounter(context));
        copyContext.setInstruction(copyInstruction(context.getInstruction(StateInstruction.class)));
        return copyContext;
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

    public static StateInstance findOutLastRetriedStateInstance(StateMachineInstance stateMachineInstance,
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
     * @param context the process context
     * @return the boolean
     */
    public static boolean isCompletionConditionSatisfied(ProcessContext context) {

        StateInstruction instruction = context.getInstruction(StateInstruction.class);
        AbstractTaskState currentState = (AbstractTaskState)instruction.getState(context);
        LoopContextHolder currentLoopContext = LoopContextHolder.getCurrent(context, true);

        if (currentLoopContext.isCompletionConditionSatisfied()) {
            return true;
        }

        int nrOfInstances = currentLoopContext.getNrOfInstances().get();
        int nrOfActiveInstances = currentLoopContext.getNrOfActiveInstances().get();
        int nrOfCompletedInstances = currentLoopContext.getNrOfCompletedInstances().get();

        if (!currentLoopContext.isCompletionConditionSatisfied()) {
            synchronized (currentLoopContext) {
                if (!currentLoopContext.isCompletionConditionSatisfied()) {
                    Map<String, Object> stateMachineContext = (Map<String, Object>)context.getVariable(
                        DomainConstants.VAR_NAME_STATEMACHINE_CONTEXT);
                    // multi-instance variables should be double/float while evaluate
                    stateMachineContext.put(DomainConstants.NUMBER_OF_INSTANCES, (double)nrOfInstances);
                    stateMachineContext.put(DomainConstants.NUMBER_OF_ACTIVE_INSTANCES, (double)nrOfActiveInstances);
                    stateMachineContext.put(DomainConstants.NUMBER_OF_COMPLETED_INSTANCES,
                        (double)nrOfCompletedInstances);

                    if (nrOfCompletedInstances >= nrOfInstances || getEvaluator(context,
                        currentState.getLoop().getCompletionCondition()).evaluate(stateMachineContext)) {
                        currentLoopContext.setCompletionConditionSatisfied(true);
                    }
                }
            }
        }

        return currentLoopContext.isCompletionConditionSatisfied();
    }

    public static int acquireNextLoopCounter(ProcessContext context) {
        try {
            return LoopContextHolder.getCurrent(context, true).getLoopCounterStack().pop();
        } catch (EmptyStackException e) {
            return -1;
        }
    }

    /**
     * generate loop state name like stateName-fork-1
     *
     * @param stateName the state name
     * @param context the process context
     * @return the loop state name
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
     * @param stateName the state name
     * @return the reloaded loop counter
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

    /**
     * put loop out params to parent context
     *
     * @param context the process context
     */
    public static void putContextToParent(ProcessContext context, List<ProcessContext> subContextList, State state) {

        Map<String, Object> contextVariables = (Map<String, Object>)context.getVariable(
            DomainConstants.VAR_NAME_STATEMACHINE_CONTEXT);
        if (CollectionUtils.isNotEmpty(subContextList)) {

            StateMachineConfig stateMachineConfig = (StateMachineConfig)context.getVariable(
                DomainConstants.VAR_NAME_STATEMACHINE_CONFIG);

            List<Map<String, Object>> subContextVariables = new ArrayList<>();
            for (ProcessContext subProcessContext : subContextList) {
                StateInstance stateInstance = (StateInstance)subProcessContext.getVariable(DomainConstants.VAR_NAME_STATE_INST);

                Map<String, Object> outputVariablesToContext = ParameterUtils.createOutputParams(
                    stateMachineConfig.getExpressionFactoryManager(), (AbstractTaskState)state, stateInstance.getOutputParams());
                subContextVariables.add(outputVariablesToContext);
            }

            contextVariables.put(DomainConstants.LOOP_RESULT, subContextVariables);
        }

    }

    /**
     * forward with subStateMachine should check each loop state's status
     *
     * @param context the process context
     * @return the boolean
     */
    public static boolean isForSubStateMachineForward(ProcessContext context) {

        StateMachineInstance stateMachineInstance = (StateMachineInstance)context.getVariable(
            DomainConstants.VAR_NAME_STATEMACHINE_INST);
        StateInstruction instruction = context.getInstruction(StateInstruction.class);
        StateMachineConfig stateMachineConfig = (StateMachineConfig)context.getVariable(
            DomainConstants.VAR_NAME_STATEMACHINE_CONFIG);

        StateInstance lastRetriedStateInstance = LoopTaskUtils.findOutLastRetriedStateInstance(
            stateMachineInstance, LoopTaskUtils.generateLoopStateName(context, instruction.getStateName()));

        if (null != lastRetriedStateInstance && DomainConstants.STATE_TYPE_SUB_STATE_MACHINE.equals(
            lastRetriedStateInstance.getType()) && !ExecutionStatus.SU.equals(
            lastRetriedStateInstance.getCompensationStatus())) {

            while (StringUtils.isNotBlank(lastRetriedStateInstance.getStateIdRetriedFor())) {
                lastRetriedStateInstance = stateMachineConfig.getStateLogStore().getStateInstance(
                    lastRetriedStateInstance.getStateIdRetriedFor(), lastRetriedStateInstance.getMachineInstanceId());
            }

            List<StateMachineInstance> subInst = stateMachineConfig.getStateLogStore()
                .queryStateMachineInstanceByParentId(EngineUtils.generateParentId(lastRetriedStateInstance));
            if (CollectionUtils.isNotEmpty(subInst)) {
                if (ExecutionStatus.SU.equals(subInst.get(0).getCompensationStatus())) {
                    return false;
                }
            }

            if (ExecutionStatus.UN.equals(subInst.get(0).getCompensationStatus())) {
                throw new ForwardInvalidException(
                    "Last forward execution state instance is SubStateMachine and compensation status is "
                        + "[UN], Operation[forward] denied, stateInstanceId:"
                        + lastRetriedStateInstance.getId(), FrameworkErrorCode.OperationDenied);
            }

            return true;
        }
        return false;
    }

    /**
     * decide current exception route for loop publish over
     *
     * @param subContextList the sub context list
     * @param stateMachine the state machine
     * @return route if current exception route not null
     */
    public static String decideCurrentExceptionRoute(List<ProcessContext> subContextList, StateMachine stateMachine) {

        String route = null;
        if (CollectionUtils.isNotEmpty(subContextList)) {

            for (ProcessContext processContext : subContextList) {
                String next = (String)processContext.getVariable(DomainConstants.VAR_NAME_CURRENT_EXCEPTION_ROUTE);
                if (StringUtils.isNotBlank(next)) {

                    // compensate must be execute
                    State state = stateMachine.getState(next);
                    if (DomainConstants.STATE_TYPE_COMPENSATION_TRIGGER.equals(state.getType())) {
                        route = next;
                        break;
                    } else if (null == route) {
                        route = next;
                    }
                }
            }
        }
        return route;
    }

    /**
     * get loop completion condition evaluator
     *
     * @param context the process context
     * @param completionCondition the completion condition
     * @return the expression evaluator
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