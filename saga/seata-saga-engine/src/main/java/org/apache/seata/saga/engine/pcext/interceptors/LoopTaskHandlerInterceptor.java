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
package org.apache.seata.saga.engine.pcext.interceptors;

import org.apache.seata.common.loader.LoadLevel;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.saga.engine.StateMachineConfig;
import org.apache.seata.saga.engine.exception.EngineExecutionException;
import org.apache.seata.saga.engine.expression.Expression;
import org.apache.seata.saga.engine.expression.ExpressionFactory;
import org.apache.seata.saga.engine.expression.ExpressionFactoryManager;
import org.apache.seata.saga.engine.pcext.InterceptableStateHandler;
import org.apache.seata.saga.engine.pcext.StateHandlerInterceptor;
import org.apache.seata.saga.engine.pcext.StateInstruction;
import org.apache.seata.saga.engine.pcext.handlers.ServiceTaskStateHandler;
import org.apache.seata.saga.engine.pcext.handlers.SubStateMachineHandler;
import org.apache.seata.saga.engine.pcext.utils.CompensationHolder;
import org.apache.seata.saga.engine.pcext.utils.EngineUtils;
import org.apache.seata.saga.engine.pcext.utils.LoopContextHolder;
import org.apache.seata.saga.engine.pcext.utils.LoopTaskUtils;
import org.apache.seata.saga.proctrl.HierarchicalProcessContext;
import org.apache.seata.saga.proctrl.ProcessContext;
import org.apache.seata.saga.statelang.domain.DomainConstants;
import org.apache.seata.saga.statelang.domain.ExecutionStatus;
import org.apache.seata.saga.statelang.domain.StateInstance;
import org.apache.seata.saga.statelang.domain.TaskState.Loop;
import org.apache.seata.saga.statelang.domain.impl.AbstractTaskState;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

/**
 * State Interceptor For ServiceTask, SubStateMachine, ScriptTask With Loop Attribute
 *
 */
@LoadLevel(name = "LoopTask", order = 90)
public class LoopTaskHandlerInterceptor implements StateHandlerInterceptor {

    @Override
    public boolean match(Class<? extends InterceptableStateHandler> clazz) {
        return clazz != null
                && (ServiceTaskStateHandler.class.isAssignableFrom(clazz)
                        || SubStateMachineHandler.class.isAssignableFrom(clazz)
                        || ScriptTaskHandlerInterceptor.class.isAssignableFrom(clazz));
    }

    @Override
    public void preProcess(ProcessContext context) throws EngineExecutionException {

        if (context.hasVariable(DomainConstants.VAR_NAME_IS_LOOP_STATE)) {
            StateInstruction instruction = context.getInstruction(StateInstruction.class);
            AbstractTaskState currentState = (AbstractTaskState) instruction.getState(context);

            int loopCounter;
            Loop loop;
            Collection<?> collection = null;

            @SuppressWarnings("unchecked")
            Map<String, Object> contextVariables =
                    (Map<String, Object>) context.getVariable(DomainConstants.VAR_NAME_STATEMACHINE_CONTEXT);

            if (context.hasVariable(DomainConstants.VAR_NAME_CURRENT_COMPEN_TRIGGER_STATE)) {
                // compensate condition should get stateToBeCompensated 's config
                CompensationHolder compensationHolder = CompensationHolder.getCurrent(context, true);
                StateInstance stateToBeCompensated =
                        compensationHolder.getStatesNeedCompensation().get(currentState.getName());
                AbstractTaskState compensateState = (AbstractTaskState) stateToBeCompensated
                        .getStateMachineInstance()
                        .getStateMachine()
                        .getState(EngineUtils.getOriginStateName(stateToBeCompensated));
                loop = compensateState.getLoop();
                loopCounter = LoopTaskUtils.reloadLoopCounter(stateToBeCompensated.getName());
                StateMachineConfig stateMachineConfig =
                        (StateMachineConfig) context.getVariable(DomainConstants.VAR_NAME_STATEMACHINE_CONFIG);
                ExpressionFactoryManager expressionFactoryManager =
                        stateMachineConfig != null ? stateMachineConfig.getExpressionFactoryManager() : null;

                if (expressionFactoryManager != null && StringUtils.isNotBlank(loop.getCollection())) {
                    ExpressionFactory expressionFactory = expressionFactoryManager.getExpressionFactory(
                            ExpressionFactoryManager.DEFAULT_EXPRESSION_TYPE);
                    Expression expression = expressionFactory.createExpression(loop.getCollection());

                    Object evaluatedResult = expression.getValue(contextVariables);
                    if (evaluatedResult instanceof Collection) {
                        collection = (Collection<?>) evaluatedResult;
                    }
                }
            } else {
                loop = currentState.getLoop();
                loopCounter = (int) context.getVariable(DomainConstants.LOOP_COUNTER);
                collection = LoopContextHolder.getCurrent(context, true).getCollection();
            }

            if (collection != null) {
                Map<String, Object> copyContextVariables = new ConcurrentHashMap<>(contextVariables);
                copyContextVariables.put(loop.getElementIndexName(), loopCounter);
                copyContextVariables.put(loop.getElementVariableName(), iterator(collection, loopCounter));
                ((HierarchicalProcessContext) context)
                        .setVariableLocally(DomainConstants.VAR_NAME_STATEMACHINE_CONTEXT, copyContextVariables);
            }
        }
    }

    @Override
    public void postProcess(ProcessContext context, Exception e) throws EngineExecutionException {

        if (context.hasVariable(DomainConstants.VAR_NAME_IS_LOOP_STATE)) {

            StateInstance stateInstance = (StateInstance) context.getVariable(DomainConstants.VAR_NAME_STATE_INST);
            if (null != stateInstance
                    && !LoopContextHolder.getCurrent(context, true).isFailEnd()) {
                if (!ExecutionStatus.SU.equals(stateInstance.getStatus())) {
                    LoopContextHolder.getCurrent(context, true).setFailEnd(true);
                }
            }

            Exception exp = (Exception) ((HierarchicalProcessContext) context)
                    .getVariableLocally(DomainConstants.VAR_NAME_CURRENT_EXCEPTION);
            if (exp == null) {
                exp = e;
            }

            if (null != e) {
                if (context.hasVariable(DomainConstants.LOOP_SEMAPHORE)) {
                    Semaphore semaphore = (Semaphore) context.getVariable(DomainConstants.LOOP_SEMAPHORE);
                    semaphore.release();
                }
            }

            if (null != exp) {
                LoopContextHolder.getCurrent(context, true).setFailEnd(true);
            } else {
                LoopContextHolder.getCurrent(context, true)
                        .getNrOfCompletedInstances()
                        .incrementAndGet();
            }
            LoopContextHolder.getCurrent(context, true).getNrOfActiveInstances().decrementAndGet();
        }
    }

    private Object iterator(Collection<?> collection, int loopCounter) {
        Iterator<?> iterator = collection.iterator();
        int index = 0;
        Object value = null;
        while (index <= loopCounter) {
            value = iterator.next();
            index += 1;
        }
        return value;
    }
}
