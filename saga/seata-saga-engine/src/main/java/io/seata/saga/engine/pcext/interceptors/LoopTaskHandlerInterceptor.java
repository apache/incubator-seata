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
package io.seata.saga.engine.pcext.interceptors;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import io.seata.common.loader.LoadLevel;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.StringUtils;
import io.seata.saga.engine.StateMachineConfig;
import io.seata.saga.engine.exception.EngineExecutionException;
import io.seata.saga.engine.pcext.InterceptableStateHandler;
import io.seata.saga.engine.pcext.StateHandlerInterceptor;
import io.seata.saga.engine.pcext.StateInstruction;
import io.seata.saga.engine.pcext.handlers.ServiceTaskStateHandler;
import io.seata.saga.engine.pcext.handlers.SubStateMachineHandler;
import io.seata.saga.engine.pcext.utils.CompensationHolder;
import io.seata.saga.engine.pcext.utils.EngineUtils;
import io.seata.saga.engine.pcext.utils.LoopContextHolder;
import io.seata.saga.engine.pcext.utils.LoopTaskUtils;
import io.seata.saga.proctrl.HierarchicalProcessContext;
import io.seata.saga.proctrl.ProcessContext;
import io.seata.saga.proctrl.impl.ProcessContextImpl;
import io.seata.saga.statelang.domain.DomainConstants;
import io.seata.saga.statelang.domain.ExecutionStatus;
import io.seata.saga.statelang.domain.State;
import io.seata.saga.statelang.domain.StateInstance;
import io.seata.saga.statelang.domain.StateMachineInstance;
import io.seata.saga.statelang.domain.TaskState.Loop;
import io.seata.saga.statelang.domain.impl.AbstractTaskState;

/**
 * State Interceptor For ServiceTask, SubStateMachine, ScriptTask With Loop Attribute
 *
 * @author anselleeyy
 */
@LoadLevel(name = "LoopTask", order = 90)
public class LoopTaskHandlerInterceptor implements StateHandlerInterceptor {

    @Override
    public boolean match(Class<? extends InterceptableStateHandler> clazz) {
        return clazz != null &&
            (ServiceTaskStateHandler.class.isAssignableFrom(clazz)
                || SubStateMachineHandler.class.isAssignableFrom(clazz)
                || ScriptTaskHandlerInterceptor.class.isAssignableFrom(clazz));
    }

    @Override
    public void preProcess(ProcessContext context) throws EngineExecutionException {

        if (context.hasVariable(DomainConstants.VAR_NAME_IS_LOOP_STATE)) {
            StateInstruction instruction = context.getInstruction(StateInstruction.class);
            AbstractTaskState currentState = (AbstractTaskState)instruction.getState(context);

            int loopCounter = (int)context.getVariable(DomainConstants.LOOP_COUNTER);
            Loop loop;

            State compensationTriggerState = (State)((HierarchicalProcessContext)context).getVariableLocally(
                DomainConstants.VAR_NAME_CURRENT_COMPEN_TRIGGER_STATE);

            // get loop config
            if (null != compensationTriggerState) {
                // compensate condition should get stateToBeCompensated 's config
                CompensationHolder compensationHolder = CompensationHolder.getCurrent(context, true);
                StateInstance stateToBeCompensated = compensationHolder.getStatesNeedCompensation().get(currentState.getName());
                AbstractTaskState compensateState = (AbstractTaskState)stateToBeCompensated.getStateMachineInstance()
                    .getStateMachine().getState(EngineUtils.getOriginStateName(stateToBeCompensated));
                loop = compensateState.getLoop();
                loopCounter = LoopTaskUtils.reloadLoopCounter(stateToBeCompensated.getName());
            } else {
                loop = currentState.getLoop();
            }

            // forward with subStateMachine should check each loop state's status
            if (DomainConstants.OPERATION_NAME_FORWARD.equals(context.getVariable(DomainConstants.VAR_NAME_OPERATION_NAME))) {
                StateMachineInstance stateMachineInstance = (StateMachineInstance)context.getVariable(
                    DomainConstants.VAR_NAME_STATEMACHINE_INST);
                StateMachineConfig stateMachineConfig = (StateMachineConfig)context.getVariable(
                    DomainConstants.VAR_NAME_STATEMACHINE_CONFIG);

                StateInstance lastRetriedStateInstance = LoopTaskUtils.reloadLastRetriedStateInstance(
                    stateMachineInstance, LoopTaskUtils.generateLoopStateName(context, currentState.getName()));

                if (null != lastRetriedStateInstance && DomainConstants.STATE_TYPE_SUB_STATE_MACHINE.equals(
                    lastRetriedStateInstance.getType())) {

                    boolean isForForward = !ExecutionStatus.SU.equals(lastRetriedStateInstance.getCompensationStatus());

                    if (isForForward) {
                        while (StringUtils.isNotBlank(lastRetriedStateInstance.getStateIdRetriedFor())) {
                            lastRetriedStateInstance = stateMachineConfig.getStateLogStore().getStateInstance(
                                lastRetriedStateInstance.getStateIdRetriedFor(),
                                lastRetriedStateInstance.getMachineInstanceId());
                        }
                        List<StateMachineInstance> subInst = stateMachineConfig.getStateLogStore()
                            .queryStateMachineInstanceByParentId(
                                EngineUtils.generateParentId(lastRetriedStateInstance));
                        if (CollectionUtils.isNotEmpty(subInst)) {
                            if (ExecutionStatus.SU.equals(subInst.get(0).getCompensationStatus())) {
                                isForForward = false;
                            }
                        }
                    }

                    ((HierarchicalProcessContext)context).setVariableLocally(
                        DomainConstants.VAR_NAME_IS_FOR_SUB_STATMACHINE_FORWARD, isForForward);
                }
            }

            Collection collection = LoopContextHolder.getCurrent(context, true).getCollection();
            Map<String, Object> contextVariables = (Map<String, Object>)context.getVariable(
                DomainConstants.VAR_NAME_STATEMACHINE_CONTEXT);
            Map<String, Object> copyContextVariables = new ConcurrentHashMap<>(contextVariables.size() + 2);
            nullSafeCopy(contextVariables, copyContextVariables);
            copyContextVariables.put(loop.getElementIndexName(), loopCounter);
            copyContextVariables.put(loop.getElementVariableName(), iterator(collection, loopCounter));
            ((HierarchicalProcessContext)context).setVariableLocally(DomainConstants.VAR_NAME_STATEMACHINE_CONTEXT, copyContextVariables);
        }
    }

    @Override
    public void postProcess(ProcessContext context, Exception e) throws EngineExecutionException {

        if (context.hasVariable(DomainConstants.VAR_NAME_IS_LOOP_STATE)) {

            StateInstance stateInstance = (StateInstance)context.getVariable(DomainConstants.VAR_NAME_STATE_INST);
            if (null != stateInstance && !LoopContextHolder.getCurrent(context, true).isFailEnd()) {
                if (!ExecutionStatus.SU.equals(stateInstance.getStatus())) {
                    LoopContextHolder.getCurrent(context, true).setFailEnd(true);
                    putContextToParent(context);
                }
            }

            boolean compensateOperation = false;

            Stack<Exception> expStack = LoopContextHolder.getCurrent(context, true).getLoopExpContext();
            Exception exp = (Exception)((HierarchicalProcessContext)context).getVariableLocally(DomainConstants.VAR_NAME_CURRENT_EXCEPTION);
            if (exp == null) {
                exp = e;
            }

            if (null != e) {
                if (((HierarchicalProcessContext)context).hasVariableLocal(DomainConstants.LOOP_COUNT_DOWN_LATCH)) {
                    CountDownLatch countDownLatch = (CountDownLatch)context.removeVariable(DomainConstants.LOOP_COUNT_DOWN_LATCH);
                    countDownLatch.countDown();
                }
            }

            if (null != exp) {
                expStack.push(exp);
                String next = (String)((HierarchicalProcessContext)context).getVariableLocally(
                    DomainConstants.VAR_NAME_CURRENT_EXCEPTION_ROUTE);
                if (StringUtils.isNotBlank(next) && next.equals(DomainConstants.STATE_TYPE_COMPENSATION_TRIGGER)) {
                    LoopContextHolder.getCurrent(context, true).setNeedCompensate(true);
                    ((HierarchicalProcessContext)context).removeVariableLocally(DomainConstants.VAR_NAME_CURRENT_EXCEPTION_ROUTE);
                }
            } else {
                LoopContextHolder.getCurrent(context, true).getNrOfCompletedInstances().incrementAndGet();
            }
            LoopContextHolder.getCurrent(context, true).getNrOfActiveInstances().decrementAndGet();

            State compensationTriggerState = (State)((HierarchicalProcessContext)context).getVariableLocally(
                DomainConstants.VAR_NAME_CURRENT_COMPEN_TRIGGER_STATE);
            if (compensationTriggerState != null) {
                compensateOperation = true;
            }

            int loopCounter = LoopTaskUtils.acquireNextLoopCounter(context);
            if (LoopContextHolder.getCurrent(context, true).isFailEnd()
                    || LoopTaskUtils.isCompletionConditionSatisfied(context)
                    || (!compensateOperation && loopCounter < 0)) {
                if (!expStack.isEmpty()) {
                    ((HierarchicalProcessContext)context).setVariableLocally(DomainConstants.VAR_NAME_CURRENT_EXCEPTION,
                        expStack.peek());
                } else if (!LoopContextHolder.getCurrent(context, true).isFailEnd()) {
                    // put one out context to parent for choice decision under normal condition
                    putContextToParent(context);
                }
                context.removeVariable(DomainConstants.VAR_NAME_CURRENT_COMPEN_TRIGGER_STATE);
                context.removeVariable(DomainConstants.VAR_NAME_CURRENT_LOOP_STATE);
            } else {
                LoopContextHolder.getCurrent(context, true).getNrOfActiveInstances().incrementAndGet();
                if (compensateOperation) {
                    LoopContextHolder.getCurrent(context, true).getNrOfInstances().decrementAndGet();
                } else {
                    ((HierarchicalProcessContext)context).setVariableLocally(DomainConstants.LOOP_COUNTER, loopCounter);
                }
            }
        }
    }

    private void nullSafeCopy(Map<String, Object> srcMap, Map<String, Object> destMap) {
        srcMap.forEach((key, value) -> {
            if (value != null) {
                destMap.put(key, value);
            }
        });
    }

    private Object iterator(Collection collection, int loopCounter) {
        Iterator iterator = collection.iterator();
        int index = 0;
        Object value = null;
        while (index <= loopCounter) {
            value = iterator.next();
            index += 1;
        }
        return value;
    }

    private void putContextToParent(ProcessContext context) {
        Map<String, Object> contextVariables = (Map<String, Object>)context.getVariable(
            DomainConstants.VAR_NAME_STATEMACHINE_CONTEXT);
        if (CollectionUtils.isNotEmpty(contextVariables)) {
            Map<String, Object> parentContextVariables = (Map<String, Object>)((ProcessContextImpl)context).getParent()
                .getVariable(DomainConstants.VAR_NAME_STATEMACHINE_CONTEXT);
            parentContextVariables.putAll(contextVariables);
        }
    }

}
