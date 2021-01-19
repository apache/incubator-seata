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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;

import io.seata.common.loader.LoadLevel;
import io.seata.common.util.StringUtils;
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
import io.seata.saga.proctrl.ProcessContext;
import io.seata.saga.statelang.domain.DomainConstants;
import io.seata.saga.statelang.domain.ExecutionStatus;
import io.seata.saga.statelang.domain.State;
import io.seata.saga.statelang.domain.StateInstance;
import io.seata.saga.statelang.domain.StateMachineInstance;
import io.seata.saga.statelang.domain.TaskState.Loop;
import io.seata.saga.statelang.domain.impl.AbstractTaskState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author anselleeyy
 */
@LoadLevel(name = "LoopTask", order = 90)
public class LoopTaskHandlerInterceptor implements StateHandlerInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoopTaskHandlerInterceptor.class);

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

            State compensationTriggerState = (State)context.getVariable(
                DomainConstants.VAR_NAME_CURRENT_COMPEN_TRIGGER_STATE);
            if (compensationTriggerState != null) {
                CompensationHolder compensationHolder = CompensationHolder.getCurrent(context, true);
                StateInstance stateToBeCompensated = compensationHolder.getStatesNeedCompensation().get(currentState.getName());
                if (loopCounter < 0) {
                    return;
                }
                AbstractTaskState compensateState = (AbstractTaskState)stateToBeCompensated.getStateMachineInstance()
                    .getStateMachine().getState(EngineUtils.getOriginStateName(stateToBeCompensated));
                loop = compensateState.getLoop();
            } else {
                loop = currentState.getLoop();
                if (DomainConstants.OPERATION_NAME_FORWARD.equals(context.getVariable(DomainConstants.VAR_NAME_OPERATION_NAME))) {
                    StateMachineInstance stateMachineInstance = (StateMachineInstance)context.getVariable(
                        DomainConstants.VAR_NAME_STATEMACHINE_INST);
                    StateInstance lastRetriedStateInstance = LoopTaskUtils.reloadLastRetriedStateInstance(
                        stateMachineInstance, LoopTaskUtils.generateLoopStateName(context, currentState.getName()));
                    if (null != lastRetriedStateInstance && DomainConstants.STATE_TYPE_SUB_STATE_MACHINE.equals(
                        lastRetriedStateInstance.getType()) && !ExecutionStatus.SU.equals(lastRetriedStateInstance.getCompensationStatus())) {
                        context.setVariable(DomainConstants.VAR_NAME_IS_FOR_SUB_STATMACHINE_FORWARD, true);
                    } else {
                        context.setVariable(DomainConstants.VAR_NAME_IS_FOR_SUB_STATMACHINE_FORWARD, false);
                    }
                }
            }

            Collection collection = LoopContextHolder.getCurrent(context, true).getCollection();

            Map<String, Object> contextVariables = (Map<String, Object>)context.getVariable(
                DomainConstants.VAR_NAME_STATEMACHINE_CONTEXT);
            Map<String, Object> copyContextVariables = new ConcurrentHashMap<>(contextVariables.size() + 2);
            nullSafeCopy(contextVariables, copyContextVariables);
            copyContextVariables.put(loop.getElementIndexName(), loopCounter);
            copyContextVariables.put(loop.getElementVariableName(), iterator(collection, loopCounter));
            context.setVariable(DomainConstants.VAR_NAME_STATEMACHINE_CONTEXT, copyContextVariables);
        }
    }

    @Override
    public void postProcess(ProcessContext context, Exception e) throws EngineExecutionException {

        if (context.hasVariable(DomainConstants.VAR_NAME_IS_LOOP_STATE)) {

            boolean compensateOperation = false;

            LinkedBlockingDeque<Exception> deque = LoopContextHolder.getCurrent(context, true).getLoopExpContext();
            Exception exp = (Exception)context.getVariable(DomainConstants.VAR_NAME_CURRENT_EXCEPTION);

            if (null != exp) {
                deque.push(exp);
                String next = (String)context.getVariable(DomainConstants.VAR_NAME_CURRENT_EXCEPTION_ROUTE);
                if (StringUtils.isNotBlank(next) && next.equals(DomainConstants.STATE_TYPE_COMPENSATION_TRIGGER)) {
                    LoopContextHolder.getCurrent(context, true).setNeedCompensate(true);
                    context.removeVariable(DomainConstants.VAR_NAME_CURRENT_EXCEPTION_ROUTE);
                }
            } else {
                LoopContextHolder.getCurrent(context, true).getNrOfCompletedInstances().incrementAndGet();
            }

            State compensationTriggerState = (State)context.getVariable(DomainConstants.VAR_NAME_CURRENT_COMPEN_TRIGGER_STATE);
            if (compensationTriggerState != null) {
                compensateOperation = true;
            }
            LoopContextHolder.getCurrent(context, true).getNrOfActiveInstances().decrementAndGet();

            int loopCounter = LoopTaskUtils.acquireNextLoopCounter(context);
            if (!deque.isEmpty() || LoopTaskUtils.isCompletionConditionSatisfied(context) || (!compensateOperation && loopCounter < 0)) {
                try {
                    LoopTaskUtils.waitForComplete(context);
                    if (!deque.isEmpty()) {
                        context.setVariable(DomainConstants.VAR_NAME_CURRENT_EXCEPTION, deque.peek());
                        if (LoopTaskUtils.needCompensate(context)) {
                            context.setVariable(DomainConstants.VAR_NAME_CURRENT_EXCEPTION_ROUTE, DomainConstants.STATE_TYPE_COMPENSATION_TRIGGER);
                        } else {
                            context.removeVariable(DomainConstants.VAR_NAME_IS_LOOP_STATE);
                            EngineUtils.failStateMachine(context, deque.peek());
                        }
                    }
                } catch (InterruptedException exception) {
                    LOGGER.error("State: [{}] wait loop complete is interrupted, message: [{}]",
                        context.getInstruction(StateInstruction.class).getStateName(), exception.getMessage());
                    throw new EngineExecutionException(exception);
                } finally {
                    LoopContextHolder.clearCurrent(context);
                    if (Boolean.TRUE.equals(context.getVariable(DomainConstants.VAR_NAME_IS_LOOP_ASYNC_EXECUTION))) {
                        context.removeVariable(DomainConstants.VAR_NAME_CURRENT_COMPEN_TRIGGER_STATE);
                    }
                }
            } else {
                LoopContextHolder.getCurrent(context, true).getNrOfActiveInstances().incrementAndGet();
                if (!compensateOperation) {
                    context.setVariable(DomainConstants.LOOP_COUNTER, loopCounter);
                } else {
                    LoopContextHolder.getCurrent(context, true).getNrOfInstances().decrementAndGet();
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

}
