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

import org.apache.seata.common.exception.FrameworkErrorCode;
import org.apache.seata.common.loader.LoadLevel;
import org.apache.seata.common.util.CollectionUtils;
import org.apache.seata.saga.engine.StateMachineConfig;
import org.apache.seata.saga.engine.exception.EngineExecutionException;
import org.apache.seata.saga.engine.pcext.InterceptableStateHandler;
import org.apache.seata.saga.engine.pcext.StateHandlerInterceptor;
import org.apache.seata.saga.engine.pcext.StateInstruction;
import org.apache.seata.saga.engine.pcext.handlers.ScriptTaskStateHandler;
import org.apache.seata.saga.engine.pcext.utils.EngineUtils;
import org.apache.seata.saga.engine.pcext.utils.ParameterUtils;
import org.apache.seata.saga.engine.utils.ExceptionUtils;
import org.apache.seata.saga.proctrl.HierarchicalProcessContext;
import org.apache.seata.saga.proctrl.ProcessContext;
import org.apache.seata.saga.statelang.domain.DomainConstants;
import org.apache.seata.saga.statelang.domain.StateMachineInstance;
import org.apache.seata.saga.statelang.domain.impl.ScriptTaskStateImpl;

import java.util.List;
import java.util.Map;

/**
 * StateInterceptor for ScriptTask
 *
 */
@LoadLevel(name = "ScriptTask", order = 100)
public class ScriptTaskHandlerInterceptor implements StateHandlerInterceptor {

    @Override
    public boolean match(Class<? extends InterceptableStateHandler> clazz) {
        return clazz != null &&
                ScriptTaskStateHandler.class.isAssignableFrom(clazz);
    }

    @Override
    public void preProcess(ProcessContext context) throws EngineExecutionException {

        StateInstruction instruction = context.getInstruction(StateInstruction.class);

        StateMachineInstance stateMachineInstance = (StateMachineInstance)context.getVariable(
            DomainConstants.VAR_NAME_STATEMACHINE_INST);
        StateMachineConfig stateMachineConfig = (StateMachineConfig)context.getVariable(
            DomainConstants.VAR_NAME_STATEMACHINE_CONFIG);

        Map<String, Object> contextVariables = (Map<String, Object>)context.getVariable(
            DomainConstants.VAR_NAME_STATEMACHINE_CONTEXT);
        ScriptTaskStateImpl state = (ScriptTaskStateImpl)instruction.getState(context);
        List<Object> serviceInputParams = null;
        if (contextVariables != null) {
            try {
                serviceInputParams = ParameterUtils.createInputParams(stateMachineConfig.getExpressionResolver(), null,
                    state, contextVariables);
            } catch (Exception e) {

                String message = "Task [" + state.getName()
                    + "] input parameters assign failed, please check 'Input' expression:" + e.getMessage();

                EngineExecutionException exception = ExceptionUtils.createEngineExecutionException(e,
                    FrameworkErrorCode.VariablesAssignError, message, stateMachineInstance, state.getName());

                EngineUtils.failStateMachine(context, exception);

                throw exception;
            }
        }

        ((HierarchicalProcessContext)context).setVariableLocally(DomainConstants.VAR_NAME_INPUT_PARAMS,
            serviceInputParams);
    }

    @Override
    public void postProcess(ProcessContext context, Exception exp) throws EngineExecutionException {

        StateInstruction instruction = context.getInstruction(StateInstruction.class);
        ScriptTaskStateImpl state = (ScriptTaskStateImpl)instruction.getState(context);

        StateMachineInstance stateMachineInstance = (StateMachineInstance)context.getVariable(
            DomainConstants.VAR_NAME_STATEMACHINE_INST);

        StateMachineConfig stateMachineConfig = (StateMachineConfig)context.getVariable(
            DomainConstants.VAR_NAME_STATEMACHINE_CONFIG);

        if (exp == null) {
            exp = (Exception)context.getVariable(DomainConstants.VAR_NAME_CURRENT_EXCEPTION);
        }

        Map<String, Object> contextVariables = (Map<String, Object>)context.getVariable(
            DomainConstants.VAR_NAME_STATEMACHINE_CONTEXT);
        Object serviceOutputParams = context.getVariable(DomainConstants.VAR_NAME_OUTPUT_PARAMS);
        if (serviceOutputParams != null) {
            try {
                Map<String, Object> outputVariablesToContext = ParameterUtils.createOutputParams(
                    stateMachineConfig.getExpressionResolver(), state, serviceOutputParams);
                if (CollectionUtils.isNotEmpty(outputVariablesToContext)) {
                    contextVariables.putAll(outputVariablesToContext);
                }
            } catch (Exception e) {
                String message = "Task [" + state.getName()
                    + "] output parameters assign failed, please check 'Output' expression:" + e.getMessage();

                EngineExecutionException exception = ExceptionUtils.createEngineExecutionException(e,
                    FrameworkErrorCode.VariablesAssignError, message, stateMachineInstance, state.getName());

                EngineUtils.failStateMachine(context, exception);

                throw exception;
            }
        }

        context.removeVariable(DomainConstants.VAR_NAME_OUTPUT_PARAMS);
        context.removeVariable(DomainConstants.VAR_NAME_INPUT_PARAMS);

        if (exp != null && context.getVariable(DomainConstants.VAR_NAME_IS_EXCEPTION_NOT_CATCH) != null
            && (Boolean)context.getVariable(DomainConstants.VAR_NAME_IS_EXCEPTION_NOT_CATCH)) {
            //If there is an exception and there is no catch, need to exit the state machine to execute.

            context.removeVariable(DomainConstants.VAR_NAME_IS_EXCEPTION_NOT_CATCH);
            EngineUtils.failStateMachine(context, exp);
        }

    }
}
