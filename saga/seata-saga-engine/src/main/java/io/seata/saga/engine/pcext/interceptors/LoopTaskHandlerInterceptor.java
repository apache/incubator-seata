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
import java.util.concurrent.Semaphore;

import io.seata.common.loader.LoadLevel;
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
import io.seata.saga.statelang.domain.DomainConstants;
import io.seata.saga.statelang.domain.ExecutionStatus;
import io.seata.saga.statelang.domain.StateInstance;
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

            int loopCounter;
            Loop loop;

            // get loop config
            if (context.hasVariable(DomainConstants.VAR_NAME_CURRENT_COMPEN_TRIGGER_STATE)) {
                // compensate condition should get stateToBeCompensated 's config
                CompensationHolder compensationHolder = CompensationHolder.getCurrent(context, true);
                StateInstance stateToBeCompensated = compensationHolder.getStatesNeedCompensation().get(currentState.getName());
                AbstractTaskState compensateState = (AbstractTaskState)stateToBeCompensated.getStateMachineInstance()
                    .getStateMachine().getState(EngineUtils.getOriginStateName(stateToBeCompensated));
                loop = compensateState.getLoop();
                loopCounter = LoopTaskUtils.reloadLoopCounter(stateToBeCompensated.getName());
            } else {
                loop = currentState.getLoop();
                loopCounter = (int)context.getVariable(DomainConstants.LOOP_COUNTER);
            }

            Collection collection = LoopContextHolder.getCurrent(context, true).getCollection();
            Map<String, Object> contextVariables = (Map<String, Object>)context.getVariable(
                DomainConstants.VAR_NAME_STATEMACHINE_CONTEXT);
            Map<String, Object> copyContextVariables = new ConcurrentHashMap<>(contextVariables);
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
                }
            }

            Exception exp = (Exception)((HierarchicalProcessContext)context).getVariableLocally(DomainConstants.VAR_NAME_CURRENT_EXCEPTION);
            if (exp == null) {
                exp = e;
            }

            if (null != e) {
                if (context.hasVariable(DomainConstants.LOOP_SEMAPHORE)) {
                    Semaphore semaphore = (Semaphore)context.getVariable(DomainConstants.LOOP_SEMAPHORE);
                    semaphore.release();
                }
            }

            if (null != exp) {
                LoopContextHolder.getCurrent(context, true).setFailEnd(true);
            } else {
                LoopContextHolder.getCurrent(context, true).getNrOfCompletedInstances().incrementAndGet();
            }
            LoopContextHolder.getCurrent(context, true).getNrOfActiveInstances().decrementAndGet();

        }
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
