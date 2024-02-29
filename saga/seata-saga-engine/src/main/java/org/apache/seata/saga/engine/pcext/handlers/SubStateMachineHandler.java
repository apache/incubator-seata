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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.seata.common.exception.FrameworkErrorCode;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.saga.engine.StateMachineConfig;
import org.apache.seata.saga.engine.StateMachineEngine;
import org.apache.seata.saga.engine.exception.EngineExecutionException;
import org.apache.seata.saga.engine.exception.ForwardInvalidException;
import org.apache.seata.saga.engine.pcext.InterceptableStateHandler;
import org.apache.seata.saga.engine.pcext.StateHandler;
import org.apache.seata.saga.engine.pcext.StateHandlerInterceptor;
import org.apache.seata.saga.engine.pcext.StateInstruction;
import org.apache.seata.saga.engine.pcext.utils.EngineUtils;
import org.apache.seata.saga.engine.store.StateLogStore;
import org.apache.seata.saga.proctrl.ProcessContext;
import org.apache.seata.saga.statelang.domain.DomainConstants;
import org.apache.seata.saga.statelang.domain.ExecutionStatus;
import org.apache.seata.saga.statelang.domain.StateInstance;
import org.apache.seata.saga.statelang.domain.StateMachineInstance;
import org.apache.seata.saga.statelang.domain.SubStateMachine;
import org.apache.seata.saga.statelang.domain.impl.SubStateMachineImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SubStateMachine Handler
 *
 */
public class SubStateMachineHandler implements StateHandler, InterceptableStateHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubStateMachineHandler.class);

    private List<StateHandlerInterceptor> interceptors = new ArrayList<>();

    private static ExecutionStatus decideStatus(StateMachineInstance stateMachineInstance, boolean isForward) {

        if (isForward && ExecutionStatus.SU.equals(stateMachineInstance.getStatus())) {
            return ExecutionStatus.SU;
        } else if (stateMachineInstance.getCompensationStatus() == null || ExecutionStatus.FA.equals(
            stateMachineInstance.getCompensationStatus())) {
            return stateMachineInstance.getStatus();
        } else if (ExecutionStatus.SU.equals(stateMachineInstance.getCompensationStatus())) {
            return ExecutionStatus.FA;
        } else {
            return ExecutionStatus.UN;
        }
    }

    @Override
    public void process(ProcessContext context) throws EngineExecutionException {

        StateInstruction instruction = context.getInstruction(StateInstruction.class);
        SubStateMachineImpl subStateMachine = (SubStateMachineImpl)instruction.getState(context);

        StateMachineEngine engine = (StateMachineEngine)context.getVariable(
            DomainConstants.VAR_NAME_STATEMACHINE_ENGINE);
        StateMachineInstance stateMachineInstance = (StateMachineInstance)context.getVariable(
            DomainConstants.VAR_NAME_STATEMACHINE_INST);
        StateInstance stateInstance = (StateInstance)context.getVariable(DomainConstants.VAR_NAME_STATE_INST);

        Object inputParamsObj = context.getVariable(DomainConstants.VAR_NAME_INPUT_PARAMS);
        Map<String, Object> startParams = new HashMap<>(0);
        if (inputParamsObj instanceof List) {
            List<Object> listInputParams = (List<Object>)inputParamsObj;
            if (listInputParams.size() > 0) {
                startParams = (Map<String, Object>)listInputParams.get(0);
            }
        } else if (inputParamsObj instanceof Map) {
            startParams = (Map<String, Object>)inputParamsObj;
        }

        startParams.put(DomainConstants.VAR_NAME_PARENT_ID, EngineUtils.generateParentId(stateInstance));
        try {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(">>>>>>>>>>>>>>>>>>>>>> Start to execute SubStateMachine [{}] by state[{}]",
                    subStateMachine.getStateMachineName(), subStateMachine.getName());
            }
            StateMachineInstance subStateMachineInstance = callSubStateMachine(startParams, engine, context,
                stateInstance, subStateMachine);

            Map<String, Object> outputParams = subStateMachineInstance.getEndParams();
            boolean isForward = DomainConstants.OPERATION_NAME_FORWARD.equals(
                context.getVariable(DomainConstants.VAR_NAME_OPERATION_NAME));
            ExecutionStatus callSubMachineStatus = decideStatus(subStateMachineInstance, isForward);
            stateInstance.setStatus(callSubMachineStatus);
            outputParams.put(DomainConstants.VAR_NAME_SUB_STATEMACHINE_EXEC_STATUE, callSubMachineStatus.toString());
            context.setVariable(DomainConstants.VAR_NAME_OUTPUT_PARAMS, outputParams);
            stateInstance.setOutputParams(outputParams);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(
                    "<<<<<<<<<<<<<<<<<<<<<< SubStateMachine[{}] execute finish with status[{}], compensateStatus[{}]",
                    subStateMachine.getStateMachineName(), subStateMachineInstance.getStatus(),
                    subStateMachineInstance.getCompensationStatus());
            }

        } catch (Exception e) {

            LOGGER.error("SubStateMachine[{}] execute failed by state[name:{}]", subStateMachine.getStateMachineName(),
                subStateMachine.getName(), e);

            if (e instanceof ForwardInvalidException) {

                String retriedId = stateInstance.getStateIdRetriedFor();
                StateInstance stateToBeRetried = null;
                for (StateInstance stateInst : stateMachineInstance.getStateList()) {
                    if (retriedId.equals(stateInst.getId())) {
                        stateToBeRetried = stateInst;
                        break;
                    }
                }
                if (stateToBeRetried != null) {
                    stateInstance.setStatus(stateToBeRetried.getStatus());
                }
            }

            context.setVariable(DomainConstants.VAR_NAME_CURRENT_EXCEPTION, e);

            EngineUtils.handleException(context, subStateMachine, e);
        }
    }

    private StateMachineInstance callSubStateMachine(Map<String, Object> startParams, StateMachineEngine engine,
                                                     ProcessContext context, StateInstance stateInstance,
                                                     SubStateMachine subStateMachine) {
        if (!Boolean.TRUE.equals(context.getVariable(DomainConstants.VAR_NAME_IS_FOR_SUB_STATMACHINE_FORWARD))) {
            return startNewStateMachine(startParams, engine, stateInstance, subStateMachine);
        } else {
            context.removeVariable(DomainConstants.VAR_NAME_IS_FOR_SUB_STATMACHINE_FORWARD);

            return forwardStateMachine(startParams, engine, context, stateInstance, subStateMachine);
        }
    }

    private StateMachineInstance startNewStateMachine(Map<String, Object> startParams, StateMachineEngine engine,
                                                      StateInstance stateInstance, SubStateMachine subStateMachine) {

        StateMachineInstance subStateMachineInstance;
        if (stateInstance.getBusinessKey() != null) {
            subStateMachineInstance = engine.startWithBusinessKey(subStateMachine.getStateMachineName(),
                stateInstance.getStateMachineInstance().getTenantId(), stateInstance.getBusinessKey(), startParams);
        } else {
            subStateMachineInstance = engine.start(subStateMachine.getStateMachineName(),
                stateInstance.getStateMachineInstance().getTenantId(), startParams);
        }
        return subStateMachineInstance;
    }

    private StateMachineInstance forwardStateMachine(Map<String, Object> startParams, StateMachineEngine engine,
                                                     ProcessContext context, StateInstance stateInstance,
                                                     SubStateMachine subStateMachine) {
        StateMachineConfig stateMachineConfig = (StateMachineConfig)context.getVariable(
            DomainConstants.VAR_NAME_STATEMACHINE_CONFIG);
        StateLogStore statePersister = stateMachineConfig.getStateLogStore();
        if (statePersister == null) {
            throw new ForwardInvalidException("StatePersister is not configured", FrameworkErrorCode.ObjectNotExists);
        }

        StateInstance originalStateInst = stateInstance;
        do {
            originalStateInst = statePersister.getStateInstance(originalStateInst.getStateIdRetriedFor(),
                originalStateInst.getMachineInstanceId());
        } while (StringUtils.hasText(originalStateInst.getStateIdRetriedFor()));

        List<StateMachineInstance> subInst = statePersister.queryStateMachineInstanceByParentId(
            EngineUtils.generateParentId(originalStateInst));
        if (subInst.size() > 0) {
            String subInstId = subInst.get(0).getId();

            return engine.forward(subInstId, startParams);
        } else {
            originalStateInst.setStateMachineInstance(stateInstance.getStateMachineInstance());
            return startNewStateMachine(startParams, engine, originalStateInst, subStateMachine);
        }
    }

    @Override
    public List<StateHandlerInterceptor> getInterceptors() {
        return interceptors;
    }

    @Override
    public void addInterceptor(StateHandlerInterceptor interceptor) {
        if (interceptors != null && !interceptors.contains(interceptor)) {
            interceptors.add(interceptor);
        }
    }

    public void setInterceptors(List<StateHandlerInterceptor> interceptors) {
        this.interceptors = interceptors;
    }
}
