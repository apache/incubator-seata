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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
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
    public static final String LOOP_STATE_NAME_PATTERN = "-fork-";

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
                    Object expression = ParameterUtils.createValueExpression(stateMachineConfig.getExpressionFactoryManager(), collectionName);
                    Object collection = ParameterUtils.getValue(expression, stateMachineInstance.getContext(), null);
                    if (collection instanceof Collection && ((Collection)collection).size() > 0) {
                        context.setVariable(DomainConstants.LOOP_COLLECTION, collection);
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
     * @param loop
     */
    public static void createLoopContext(ProcessContext context, Loop loop) {

        LoopContextHolder loopContextHolder = LoopContextHolder.getCurrent(context, true);

        Collection collection = (Collection)context.getVariable(DomainConstants.LOOP_COLLECTION);
        loopContextHolder.getNrOfInstances().set(collection.size());
        loopContextHolder.setCollection(collection);

        Map<Integer, AtomicBoolean> loopContext = loopContextHolder.getLoopCounterContext();
        for (int i = 0; i < collection.size(); i++) {
            loopContext.put(i, new AtomicBoolean(false));
        }

        int asyncPublisherNumber = getMaxMultiInstanceNumber(loop.getParallel(), collection.size()) - 1;
        List<ProcessContext> asyncProcessContextList = new ArrayList<>(asyncPublisherNumber);
        buildAsyncProcessContext(context, asyncPublisherNumber, asyncProcessContextList, false);
        loopContextHolder.getNrOfActiveInstances().set(asyncPublisherNumber + 1);

        context.setVariable(DomainConstants.LOOP_PROCESS_CONTEXT, asyncProcessContextList);
        context.setVariable(DomainConstants.LOOP_COUNTER, acquireNextLoopCounter(context));
    }

    public static void reloadLoopContext(ProcessContext context, StateInstance forwardState, Loop loop) {

        StateMachineInstance stateMachineInstance = (StateMachineInstance)context.getVariable(
            DomainConstants.VAR_NAME_STATEMACHINE_INST);

        List<StateInstance> actList = stateMachineInstance.getStateList();
        String originStateName = EngineUtils.getOriginStateName(forwardState);
        List<StateInstance> forwardStateList = actList.stream()
            .filter(e -> originStateName.equals(EngineUtils.getOriginStateName(e)))
            .collect(Collectors.toList());

        LoopContextHolder loopContextHolder = LoopContextHolder.getCurrent(context, true);
        Collection collection = (Collection)context.getVariable(DomainConstants.LOOP_COLLECTION);
        loopContextHolder.setCollection(collection);

        Map<Integer, AtomicBoolean> loopContext = loopContextHolder.getLoopCounterContext();
        for (int i = 0; i < collection.size(); i++) {
            loopContext.put(i, new AtomicBoolean(false));
        }
        loopContext.get(reloadLoopCounter(forwardState.getName())).set(true);

        int executedNumber = 0;
        for (StateInstance stateInstance : forwardStateList) {
            if (ExecutionStatus.SU.equals(stateInstance.getStatus())) {
                int executedLoopCounter = reloadLoopCounter(stateInstance.getName());
                loopContext.get(executedLoopCounter).getAndSet(true);
                executedNumber += 1;
            }
        }

        loopContextHolder.getNrOfInstances().set(collection.size());
        loopContextHolder.getNrOfCompletedInstances().set(executedNumber);

        int asyncPublisherNumber = LoopTaskUtils.getMaxMultiInstanceNumber(loop.getParallel(),
            collection.size() - executedNumber) - 1;
        List<ProcessContext> asyncProcessContextList = new ArrayList<>(asyncPublisherNumber);
        buildAsyncProcessContext(context, asyncPublisherNumber, asyncProcessContextList, false);
        loopContextHolder.getNrOfActiveInstances().set(asyncPublisherNumber + 1);

        context.setVariable(DomainConstants.LOOP_PROCESS_CONTEXT, asyncProcessContextList);
        context.setVariable(DomainConstants.LOOP_COUNTER, reloadLoopCounter(forwardState.getName()));
    }

    public static void createCompensateContext(ProcessContext context, StateInstance stateToBeCompensated) {

        if (Boolean.TRUE.equals(context.getVariable(DomainConstants.VAR_NAME_IS_LOOP_STATE))) {
            context.setVariable(DomainConstants.LOOP_COUNTER, reloadLoopCounter(stateToBeCompensated.getName()));
            return;
        }

        StateMachine stateMachine = (StateMachine)context.getVariable(DomainConstants.VAR_NAME_STATEMACHINE);
        State state = stateMachine.getState(EngineUtils.getOriginStateName(stateToBeCompensated));
        Loop loop = getLoopConfig(context, state);

        if (loop == null) {
            return;
        }

        LoopContextHolder loopContextHolder = LoopContextHolder.getCurrent(context, true);
        loopContextHolder.setCollection((Collection)context.getVariable(DomainConstants.LOOP_COLLECTION));

        Stack<StateInstance> stateStackToBeCompensated = CompensationHolder.getCurrent(context, true)
            .getStateStackNeedCompensation();
        int sameStateNeedToBeCompensatedSize = 1;
        for (StateInstance stateInstance : stateStackToBeCompensated) {
            if (state.getName().equals(EngineUtils.getOriginStateName(stateInstance))) {
                sameStateNeedToBeCompensatedSize += 1;
            }
        }

        int asyncPublisherNumber = getMaxMultiInstanceNumber(loop.getParallel(), sameStateNeedToBeCompensatedSize) - 1;
        loopContextHolder.getNrOfInstances().set(sameStateNeedToBeCompensatedSize);
        loopContextHolder.getNrOfActiveInstances().set(asyncPublisherNumber + 1);

        List<ProcessContext> asyncProcessContextList = new ArrayList<>(asyncPublisherNumber);

        buildAsyncProcessContext(context, asyncPublisherNumber, asyncProcessContextList, true);

        for (int i = 0; i < asyncPublisherNumber; i++) {
            StateInstance stateToBeCompensatedTemp = stateStackToBeCompensated.pop();
            int loopCounter = reloadLoopCounter(stateToBeCompensatedTemp.getName());
            ProcessContext tempContext = asyncProcessContextList.get(i);

            tempContext.setVariable(DomainConstants.LOOP_COUNTER, loopCounter);
            CompensationHolder.clearCurrent(tempContext);
            StateInstruction instruction = tempContext.getInstruction(StateInstruction.class);
            CompensationHolder.getCurrent(tempContext, true).addToBeCompensatedState(instruction.getStateName(),
                stateToBeCompensatedTemp);
            CompensationHolder.getCurrent(tempContext, true).setStateStackNeedCompensation(stateStackToBeCompensated);
        }

        context.setVariable(DomainConstants.LOOP_PROCESS_CONTEXT, asyncProcessContextList);
        context.setVariable(DomainConstants.LOOP_COUNTER, reloadLoopCounter(stateToBeCompensated.getName()));

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

    public static StateInstance reloadLastRetriedStateInstance(StateMachineInstance stateMachineInstance, String stateName) {
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

        return nrOfCompletedInstances >= nrOfInstances ||
            getEvaluator(context, currentState.getLoop().getCompletionCondition()).evaluate(elContext);
    }

    /**
     * wait current state loop execution finished
     *
     * @param context
     * @throws InterruptedException
     */
    public static void waitForComplete(ProcessContext context) throws InterruptedException {
        if (Boolean.TRUE.equals(context.getVariable(DomainConstants.VAR_NAME_IS_LOOP_ASYNC_EXECUTION))) {
            return;
        }

        LinkedBlockingDeque<Exception> deque = LoopContextHolder.getCurrent(context, true).getLoopExpContext();
        if (null == deque || deque.isEmpty()) {
            boolean isSatisfied = isCompletionConditionSatisfied(context);
            while (!isSatisfied) {
                TimeUnit.MILLISECONDS.sleep(2);
                isSatisfied = isCompletionConditionSatisfied(context);
            }
        }

        while (LoopContextHolder.getCurrent(context, true).getNrOfActiveInstances().get() > 0) {
            TimeUnit.MILLISECONDS.sleep(2);
        }
    }

    public static int acquireNextLoopCounter(ProcessContext context) {
        int loopCounter = -1;
        Map<Integer, AtomicBoolean> loopCounterContext = LoopContextHolder.getCurrent(context, true).getLoopCounterContext();
        if (null == loopCounterContext) {
            return loopCounter;
        }
        for (Integer counter : loopCounterContext.keySet()) {
            if (!loopCounterContext.get(counter).getAndSet(true)) {
                loopCounter = counter;
                break;
            }
        }
        return loopCounter;
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
        return !Boolean.TRUE.equals(context.getVariable(DomainConstants.VAR_NAME_IS_LOOP_ASYNC_EXECUTION))
                && !context.getVariable(DomainConstants.VAR_NAME_OPERATION_NAME).equals(DomainConstants.OPERATION_NAME_COMPENSATE)
                && LoopContextHolder.getCurrent(context, true).isNeedCompensate();
    }

    private static void buildAsyncProcessContext(ProcessContext originContext, int asyncPublisherNumber,
                                                 List<ProcessContext> asyncProcessContextList, boolean isCompensate) {
        for (int i = 0; i < asyncPublisherNumber; i++) {
            int loopCounter = -1;
            if (!isCompensate) {
                loopCounter = acquireNextLoopCounter(originContext);
                if (loopCounter < 0) {
                    break;
                }
            }
            ProcessContext copyContext = new ProcessContextImpl();
            copyContext.setVariables(new ConcurrentHashMap<>(originContext.getVariables()));
            copyContext.setVariable(DomainConstants.VAR_NAME_IS_LOOP_ASYNC_EXECUTION, true);
            copyContext.setVariable(DomainConstants.LOOP_COUNTER, loopCounter);
            copyContext.removeVariable(DomainConstants.VAR_NAME_RETRIED_STATE_INST_ID);
            copyContext.removeVariable(DomainConstants.VAR_NAME_IS_FOR_SUB_STATMACHINE_FORWARD);
            copyContext.setInstruction(copyInstruction(originContext.getInstruction(StateInstruction.class)));
            asyncProcessContextList.add(copyContext);
        }
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
            ExpressionEvaluator expressionEvaluator = (ExpressionEvaluator)stateMachineConfig.getEvaluatorFactoryManager()
                .getEvaluatorFactory(EvaluatorFactoryManager.EVALUATOR_TYPE_DEFAULT).createEvaluator(completionCondition);
            expressionEvaluator.setRootObjectName(null);
            EXPRESSION_EVALUATOR_MAP.put(completionCondition, expressionEvaluator);
        }
        return EXPRESSION_EVALUATOR_MAP.get(completionCondition);
    }

    private static int getMaxMultiInstanceNumber(int parallelNumber, int collectionSize) {
        parallelNumber = Math.min(parallelNumber, Runtime.getRuntime().availableProcessors());
        return Math.min(collectionSize, parallelNumber);
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