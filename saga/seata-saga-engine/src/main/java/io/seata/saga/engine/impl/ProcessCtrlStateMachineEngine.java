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
package io.seata.saga.engine.impl;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.seata.common.exception.FrameworkErrorCode;
import io.seata.saga.engine.AsyncCallback;
import io.seata.saga.engine.StateMachineConfig;
import io.seata.saga.engine.StateMachineEngine;
import io.seata.saga.engine.exception.EngineExecutionException;
import io.seata.saga.engine.exception.ForwardInvalidException;
import io.seata.saga.engine.pcext.StateInstruction;
import io.seata.saga.engine.pcext.interceptors.ServiceTaskHandlerInterceptor;
import io.seata.saga.engine.pcext.utils.EngineUtils;
import io.seata.saga.engine.utils.ProcessContextBuilder;
import io.seata.saga.proctrl.ProcessContext;
import io.seata.saga.proctrl.ProcessType;
import io.seata.saga.statelang.domain.DomainConstants;
import io.seata.saga.statelang.domain.ExecutionStatus;
import io.seata.saga.statelang.domain.State;
import io.seata.saga.statelang.domain.StateInstance;
import io.seata.saga.statelang.domain.StateMachine;
import io.seata.saga.statelang.domain.StateMachineInstance;
import io.seata.saga.statelang.domain.impl.AbstractTaskState;
import io.seata.saga.statelang.domain.impl.CompensationTriggerStateImpl;
import io.seata.saga.statelang.domain.impl.ServiceTaskStateImpl;
import io.seata.saga.statelang.domain.impl.StateMachineInstanceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 * ProcessCtrl-based state machine engine
 *
 * @author lorne.cl
 */
public class ProcessCtrlStateMachineEngine implements StateMachineEngine {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessCtrlStateMachineEngine.class);

    private StateMachineConfig stateMachineConfig;

    private static void nullSafeCopy(Map<String, Object> srcMap, Map<String, Object> destMap) {
        for (String key : srcMap.keySet()) {
            Object value = srcMap.get(key);
            if (value != null) {
                destMap.put(key, value);
            }
        }
    }

    @Override
    public StateMachineInstance start(String stateMachineName, String tenantId, Map<String, Object> startParams)
        throws EngineExecutionException {

        return startInternal(stateMachineName, tenantId, null, startParams, false, null);
    }

    @Override
    public StateMachineInstance startAsync(String stateMachineName, String tenantId, Map<String, Object> startParams,
                                           AsyncCallback callback) throws EngineExecutionException {

        return startInternal(stateMachineName, tenantId, null, startParams, true, callback);
    }

    @Override
    public StateMachineInstance startWithBusinessKey(String stateMachineName, String tenantId, String businessKey,
                                                     Map<String, Object> startParams) throws EngineExecutionException {

        return startInternal(stateMachineName, tenantId, businessKey, startParams, false, null);
    }

    @Override
    public StateMachineInstance startWithBusinessKeyAsync(String stateMachineName, String tenantId, String businessKey,
                                                          Map<String, Object> startParams, AsyncCallback callback)
        throws EngineExecutionException {

        return startInternal(stateMachineName, tenantId, businessKey, startParams, true, callback);
    }

    private StateMachineInstance startInternal(String stateMachineName, String tenantId, String businessKey,
                                               Map<String, Object> startParams, boolean async, AsyncCallback callback)
        throws EngineExecutionException {

        if (async && !stateMachineConfig.isEnableAsync()) {
            throw new EngineExecutionException(
                "Asynchronous start is disabled. please set StateMachineConfig.enableAsync=true first.",
                FrameworkErrorCode.AsynchronousStartDisabled);
        }

        if (StringUtils.isEmpty(tenantId)) {
            tenantId = stateMachineConfig.getDefaultTenantId();
        }

        StateMachineInstance instance = createMachineInstance(stateMachineName, tenantId, businessKey, startParams);

        ProcessContextBuilder contextBuilder = ProcessContextBuilder.create().withProcessType(ProcessType.STATE_LANG)
            .withOperationName(DomainConstants.OPERATION_NAME_START).withAsyncCallback(callback).withInstruction(
                new StateInstruction(stateMachineName, tenantId)).withStateMachineInstance(instance)
            .withStateMachineConfig(getStateMachineConfig()).withStateMachineEngine(this);

        Map<String, Object> contextVariables;
        if (startParams != null) {
            contextVariables = new ConcurrentHashMap<>(startParams.size());
            nullSafeCopy(startParams, contextVariables);
        } else {
            contextVariables = new ConcurrentHashMap<>();
        }
        instance.setContext(contextVariables);

        contextBuilder.withStateMachineContextVariables(contextVariables);

        ProcessContext processContext = contextBuilder.build();

        if (instance.getStateMachine().isPersist() && stateMachineConfig.getStateLogStore() != null) {
            stateMachineConfig.getStateLogStore().recordStateMachineStarted(instance, processContext);
        }
        if (StringUtils.isEmpty(instance.getId())) {
            instance.setId(
                stateMachineConfig.getSeqGenerator().generate(DomainConstants.SEQ_ENTITY_STATE_MACHINE_INST));
        }

        if (async) {
            stateMachineConfig.getAsyncProcessCtrlEventPublisher().publish(processContext);
        } else {
            stateMachineConfig.getProcessCtrlEventPublisher().publish(processContext);
        }

        return instance;
    }

    private StateMachineInstance createMachineInstance(String stateMachineName, String tenantId, String businessKey,
                                                       Map<String, Object> startParams) {
        StateMachine stateMachine = stateMachineConfig.getStateMachineRepository().getStateMachine(stateMachineName,
            tenantId);
        if (stateMachine == null) {
            throw new EngineExecutionException("StateMachine[" + stateMachineName + "] is not exists",
                FrameworkErrorCode.ObjectNotExists);
        }

        StateMachineInstanceImpl inst = new StateMachineInstanceImpl();
        inst.setStateMachine(stateMachine);
        inst.setMachineId(stateMachine.getId());
        inst.setTenantId(tenantId);
        inst.setBusinessKey(businessKey);

        inst.setStartParams(startParams);
        if (StringUtils.hasText(businessKey) && startParams != null) {
            startParams.put(DomainConstants.VAR_NAME_BUSINESSKEY, businessKey);
        }

        if (StringUtils.hasText((String)startParams.get(DomainConstants.VAR_NAME_PARENT_ID))) {
            inst.setParentId((String)startParams.get(DomainConstants.VAR_NAME_PARENT_ID));
            startParams.remove(DomainConstants.VAR_NAME_PARENT_ID);
        }

        inst.setStatus(ExecutionStatus.RU);

        inst.setRunning(true);

        inst.setGmtStarted(new Date());
        inst.setGmtStarted(inst.getGmtStarted());

        return inst;
    }

    @Override
    public StateMachineInstance forward(String stateMachineInstId, Map<String, Object> replaceParams)
        throws EngineExecutionException {
        return forwardInternal(stateMachineInstId, replaceParams, false, false, null);
    }

    @Override
    public StateMachineInstance forwardAsync(String stateMachineInstId, Map<String, Object> replaceParams,
                                             AsyncCallback callback) throws EngineExecutionException {
        return forwardInternal(stateMachineInstId, replaceParams, false, true, callback);
    }

    protected StateMachineInstance forwardInternal(String stateMachineInstId, Map<String, Object> replaceParams,
                                                   boolean skip, boolean async, AsyncCallback callback)
        throws EngineExecutionException {

        StateMachineInstance stateMachineInstance = reloadStateMachineInstance(stateMachineInstId);

        if (stateMachineInstance == null) {
            throw new ForwardInvalidException("StateMachineInstance is not exits",
                FrameworkErrorCode.StateMachineInstanceNotExists);
        }
        if (ExecutionStatus.SU.equals(stateMachineInstance.getStatus())
            && stateMachineInstance.getCompensationStatus() == null) {
            return stateMachineInstance;
        }

        ExecutionStatus[] acceptStatus = new ExecutionStatus[] {ExecutionStatus.FA, ExecutionStatus.UN};
        checkStatus(stateMachineInstance, acceptStatus, null, stateMachineInstance.getStatus(), null, "forward");

        List<StateInstance> actList = stateMachineInstance.getStateList();
        if (actList == null || actList.size() == 0) {
            throw new ForwardInvalidException("StateMachineInstance[id:" + stateMachineInstId
                + "] has no stateInstance, pls start a new StateMachine execution instead",
                FrameworkErrorCode.OperationDenied);
        }

        StateInstance lastForwardState = findOutLastForwardStateInstance(actList);

        if (lastForwardState == null) {
            throw new ForwardInvalidException(
                "StateMachineInstance[id:" + stateMachineInstId + "] Cannot find last forward execution stateInstance",
                FrameworkErrorCode.OperationDenied);
        }

        ProcessContextBuilder contextBuilder = ProcessContextBuilder.create().withProcessType(ProcessType.STATE_LANG)
            .withOperationName(DomainConstants.OPERATION_NAME_FORWARD).withAsyncCallback(callback)
            .withStateMachineInstance(stateMachineInstance).withStateInstance(lastForwardState).withStateMachineConfig(
                getStateMachineConfig()).withStateMachineEngine(this);

        ProcessContext context = contextBuilder.build();

        Map<String, Object> contextVariables = getStateMachineContextVariables(context, stateMachineInstance);

        if (replaceParams != null) {
            contextVariables.putAll(replaceParams);
        }
        putBusinesskeyToContextariables(stateMachineInstance, contextVariables);

        ConcurrentHashMap<String, Object> concurrentContextVariables = new ConcurrentHashMap<>(contextVariables.size());
        nullSafeCopy(contextVariables, concurrentContextVariables);

        context.setVariable(DomainConstants.VAR_NAME_STATEMACHINE_CONTEXT, concurrentContextVariables);
        stateMachineInstance.setContext(concurrentContextVariables);

        context.setVariable(lastForwardState.getName() + DomainConstants.VAR_NAME_RETRIED_STATE_INST_ID,
            lastForwardState.getId());
        if (DomainConstants.STATE_TYPE_SUB_STATE_MACHINE.equals(lastForwardState.getType()) && !ExecutionStatus.SU
            .equals(lastForwardState.getCompensationStatus())) {

            context.setVariable(DomainConstants.VAR_NAME_IS_FOR_SUB_STATMACHINE_FORWARD, true);
        }

        if (!ExecutionStatus.SU.equals(lastForwardState.getStatus())) {
            lastForwardState.setIgnoreStatus(true);
        }

        if (stateMachineInstance.getStateMachine().isPersist()) {
            stateMachineConfig.getStateLogStore().recordStateMachineRestarted(stateMachineInstance, context);
        }

        try {
            StateInstruction inst = new StateInstruction();
            inst.setTenantId(stateMachineInstance.getTenantId());
            inst.setStateMachineName(stateMachineInstance.getStateMachine().getName());
            if (skip || ExecutionStatus.SU.equals(lastForwardState.getStatus())) {

                String next = null;
                State state = stateMachineInstance.getStateMachine().getState(lastForwardState.getName());
                if (state != null && state instanceof AbstractTaskState) {
                    next = ((AbstractTaskState)state).getNext();
                }
                if (StringUtils.isEmpty(next)) {
                    LOGGER.warn(
                        "Last Forward execution StateInstance was succeed, and it has not Next State , skip forward "
                            + "operation");
                    return stateMachineInstance;
                }
                inst.setStateName(next);
            } else {

                inst.setStateName(lastForwardState.getName());
            }
            context.setInstruction(inst);

            stateMachineInstance.setStatus(ExecutionStatus.RU);
            stateMachineInstance.setRunning(true);

            if (async) {
                stateMachineConfig.getAsyncProcessCtrlEventPublisher().publish(context);
            } else {
                stateMachineConfig.getProcessCtrlEventPublisher().publish(context);
            }
        } catch (EngineExecutionException e) {
            LOGGER.error("Operate [forward] failed", e);
            throw e;
        }
        return stateMachineInstance;
    }

    private Map<String, Object> getStateMachineContextVariables(ProcessContext context,
                                                                StateMachineInstance stateMachineInstance) {

        Map<String, Object> contextVariables = stateMachineInstance.getEndParams();
        if (contextVariables == null || contextVariables.size() == 0) {
            contextVariables = stateMachineInstance.getStartParams();
        }
        if (contextVariables == null) {
            contextVariables = new HashMap<>();
        }

        if (stateMachineInstance.isRunning()) {
            List<StateInstance> stateInstanceList = stateMachineInstance.getStateList();
            if (stateInstanceList == null || stateInstanceList.size() == 0) {
                return contextVariables;
            }

            for (StateInstance stateInstance : stateInstanceList) {
                Object serviceOutputParams = stateInstance.getOutputParams();
                if (serviceOutputParams != null) {
                    ServiceTaskStateImpl state = (ServiceTaskStateImpl)stateMachineInstance.getStateMachine().getState(
                        stateInstance.getName());
                    if (state == null) {
                        throw new EngineExecutionException(
                            "Cannot find State by state name [" + stateInstance.getName() + "], may be this is a bug",
                            FrameworkErrorCode.ObjectNotExists);
                    }

                    if (state.getOutput() != null && state.getOutput().size() > 0) {
                        try {
                            Map<String, Object> outputVariablesToContext = ServiceTaskHandlerInterceptor
                                .createOutputParams(stateMachineConfig.getExpressionFactoryManager(), state,
                                    serviceOutputParams);
                            if (outputVariablesToContext != null && outputVariablesToContext.size() > 0) {
                                contextVariables.putAll(outputVariablesToContext);
                            }

                            if (StringUtils.hasLength(stateInstance.getBusinessKey())) {

                                ((Map<String, Object>)context.getVariable(
                                    DomainConstants.VAR_NAME_STATEMACHINE_CONTEXT)).put(
                                    state.getName() + DomainConstants.VAR_NAME_BUSINESSKEY,
                                    stateInstance.getBusinessKey());
                            }
                        } catch (Exception e) {
                            throw new EngineExecutionException(e, "Context variables replay faied",
                                FrameworkErrorCode.ContextVariableReplayFailed);
                        }
                    }

                }

            }
        }
        return contextVariables;
    }

    /**
     * Find the last instance of the forward execution state
     *
     * @param stateInstanceList
     * @return
     */
    public StateInstance findOutLastForwardStateInstance(List<StateInstance> stateInstanceList) {
        StateInstance lastForwardStateInstance = null;
        for (int i = stateInstanceList.size() - 1; i >= 0; i--) {
            StateInstance stateInstance = stateInstanceList.get(i);
            if (!stateInstance.isForCompensation()) {

                if (ExecutionStatus.SU.equals(stateInstance.getCompensationStatus())) {
                    continue;
                }

                if (DomainConstants.STATE_TYPE_SUB_STATE_MACHINE.equals(stateInstance.getType())) {

                    StateInstance finalState = stateInstance;

                    while (StringUtils.hasText(finalState.getStateIdRetriedFor())) {
                        finalState = stateMachineConfig.getStateLogStore().getStateInstance(
                            finalState.getStateIdRetriedFor(), finalState.getMachineInstanceId());
                    }

                    List<StateMachineInstance> subInst = stateMachineConfig.getStateLogStore()
                        .queryStateMachineInstanceByParentId(EngineUtils.generateParentId(finalState));
                    if (subInst != null && subInst.size() > 0) {

                        if (ExecutionStatus.SU.equals(subInst.get(0).getCompensationStatus())) {
                            continue;
                        }

                        if (ExecutionStatus.UN.equals(subInst.get(0).getCompensationStatus())) {
                            throw new ForwardInvalidException(
                                "Last forward execution state instance is SubStateMachine and compensation status is "
                                    + "[UN], Operation[forward] denied, stateInstanceId:"
                                    + stateInstance.getId(), FrameworkErrorCode.OperationDenied);
                        }

                    }
                } else if (ExecutionStatus.UN.equals(stateInstance.getCompensationStatus())) {

                    throw new ForwardInvalidException(
                        "Last forward execution state instance compensation status is [UN], Operation[forward] "
                            + "denied, stateInstanceId:"
                            + stateInstance.getId(), FrameworkErrorCode.OperationDenied);
                }

                lastForwardStateInstance = stateInstance;
                break;
            }
        }
        return lastForwardStateInstance;
    }

    @Override
    public StateMachineInstance compensate(String stateMachineInstId, Map<String, Object> replaceParams)
        throws EngineExecutionException {
        return compensateInternal(stateMachineInstId, replaceParams, false, null);
    }

    @Override
    public StateMachineInstance compensateAsync(String stateMachineInstId, Map<String, Object> replaceParams,
                                                AsyncCallback callback) throws EngineExecutionException {
        return compensateInternal(stateMachineInstId, replaceParams, true, callback);
    }

    public StateMachineInstance compensateInternal(String stateMachineInstId, Map<String, Object> replaceParams,
                                                   boolean async, AsyncCallback callback)
        throws EngineExecutionException {

        StateMachineInstance stateMachineInstance = reloadStateMachineInstance(stateMachineInstId);

        if (stateMachineInstance == null) {
            throw new EngineExecutionException("StateMachineInstance is not exits",
                FrameworkErrorCode.StateMachineInstanceNotExists);
        }

        if (ExecutionStatus.SU.equals(stateMachineInstance.getCompensationStatus())) {
            return stateMachineInstance;
        }

        if (stateMachineInstance.getCompensationStatus() != null) {
            ExecutionStatus[] denyStatus = new ExecutionStatus[] {ExecutionStatus.SU};
            checkStatus(stateMachineInstance, null, denyStatus, null, stateMachineInstance.getCompensationStatus(),
                "compensate");
        }

        if (replaceParams != null) {
            stateMachineInstance.getEndParams().putAll(replaceParams);
        }

        ProcessContextBuilder contextBuilder = ProcessContextBuilder.create().withProcessType(ProcessType.STATE_LANG)
            .withOperationName(DomainConstants.OPERATION_NAME_COMPENSATE).withAsyncCallback(callback)
            .withStateMachineInstance(stateMachineInstance).withStateMachineConfig(getStateMachineConfig())
            .withStateMachineEngine(this);

        ProcessContext context = contextBuilder.build();

        Map<String, Object> contextVariables = getStateMachineContextVariables(context, stateMachineInstance);

        if (replaceParams != null) {
            contextVariables.putAll(replaceParams);
        }
        putBusinesskeyToContextariables(stateMachineInstance, contextVariables);

        ConcurrentHashMap<String, Object> concurrentContextVariables = new ConcurrentHashMap<>(contextVariables.size());
        nullSafeCopy(contextVariables, concurrentContextVariables);

        context.setVariable(DomainConstants.VAR_NAME_STATEMACHINE_CONTEXT, concurrentContextVariables);
        stateMachineInstance.setContext(concurrentContextVariables);

        CompensationTriggerStateImpl tempCompensationTriggerState = new CompensationTriggerStateImpl();
        tempCompensationTriggerState.setStateMachine(stateMachineInstance.getStateMachine());

        stateMachineInstance.setRunning(true);
        if (stateMachineInstance.getStateMachine().isPersist()) {
            stateMachineConfig.getStateLogStore().recordStateMachineRestarted(stateMachineInstance, context);
        }
        try {
            StateInstruction inst = new StateInstruction();
            inst.setTenantId(stateMachineInstance.getTenantId());
            inst.setStateMachineName(stateMachineInstance.getStateMachine().getName());
            inst.setTemporaryState(tempCompensationTriggerState);

            context.setInstruction(inst);

            if (async) {
                stateMachineConfig.getAsyncProcessCtrlEventPublisher().publish(context);
            } else {
                stateMachineConfig.getProcessCtrlEventPublisher().publish(context);
            }

        } catch (EngineExecutionException e) {
            LOGGER.error("Operate [compensate] failed", e);
            throw e;
        }

        return stateMachineInstance;
    }

    @Override
    public StateMachineInstance skipAndForward(String stateMachineInstId) throws EngineExecutionException {
        return forwardInternal(stateMachineInstId, null, false, true, null);
    }

    @Override
    public StateMachineInstance skipAndForwardAsync(String stateMachineInstId, AsyncCallback callback)
        throws EngineExecutionException {
        return forwardInternal(stateMachineInstId, null, false, true, callback);
    }

    /**
     * override state machine instance
     *
     * @param instId
     * @return
     */
    protected StateMachineInstance reloadStateMachineInstance(String instId) {

        StateMachineInstance inst = stateMachineConfig.getStateLogStore().getStateMachineInstance(instId);
        if (inst != null) {
            StateMachine stateMachine = inst.getStateMachine();
            if (stateMachine == null) {
                stateMachine = stateMachineConfig.getStateMachineRepository().getStateMachineById(inst.getMachineId());
                inst.setStateMachine(stateMachine);
            }
            if (stateMachine == null) {
                throw new EngineExecutionException("StateMachine[id:" + inst.getMachineId() + "] not exist.",
                    FrameworkErrorCode.ObjectNotExists);
            }

            List<StateInstance> stateList = inst.getStateList();
            if (stateList == null || stateList.size() == 0) {
                stateList = stateMachineConfig.getStateLogStore().queryStateInstanceListByMachineInstanceId(instId);
                if (stateList != null && stateList.size() > 0) {
                    for (StateInstance tmpStateInstance : stateList) {
                        inst.putStateInstance(tmpStateInstance.getId(), tmpStateInstance);
                    }
                }
            }
        }
        return inst;
    }

    /**
     * Check if the status is legal
     *
     * @param stateMachineInstance
     * @param acceptStatus
     * @param denyStatus
     * @param status
     * @param compenStatus
     * @param operation
     * @return
     */
    protected boolean checkStatus(StateMachineInstance stateMachineInstance, ExecutionStatus[] acceptStatus,
                                  ExecutionStatus[] denyStatus, ExecutionStatus status, ExecutionStatus compenStatus,
                                  String operation) {
        if (status != null && compenStatus != null) {
            throw new EngineExecutionException("status and compensationStatus are not supported at the same time",
                FrameworkErrorCode.InvalidParameter);
        }
        if (status == null && compenStatus == null) {
            throw new EngineExecutionException("status and compensationStatus must input at least one",
                FrameworkErrorCode.InvalidParameter);
        }
        if (ExecutionStatus.SU.equals(compenStatus)) {
            String message = buildExceptionMessage(stateMachineInstance, null, null, null, ExecutionStatus.SU,
                operation);
            throw new EngineExecutionException(message, FrameworkErrorCode.OperationDenied);
        }

        if (stateMachineInstance.isRunning()) {
            throw new EngineExecutionException(
                "StateMachineInstance [id:" + stateMachineInstance.getId() + "]is running, operation[" + operation
                    + "] denied", FrameworkErrorCode.OperationDenied);
        }

        if ((denyStatus == null || denyStatus.length == 0) && (acceptStatus == null || acceptStatus.length == 0)) {
            throw new EngineExecutionException("StateMachineInstance[id:" + stateMachineInstance.getId()
                + "], acceptable status and deny status must input at least one", FrameworkErrorCode.InvalidParameter);
        }

        ExecutionStatus currentStatus = (status != null) ? status : compenStatus;

        if (!(denyStatus == null || denyStatus.length == 0)) {
            for (ExecutionStatus tempDenyStatus : denyStatus) {
                if (tempDenyStatus.compareTo(currentStatus) == 0) {
                    String message = buildExceptionMessage(stateMachineInstance, acceptStatus, denyStatus, status,
                        compenStatus, operation);
                    throw new EngineExecutionException(message, FrameworkErrorCode.OperationDenied);
                }
            }
        }

        if (acceptStatus == null || acceptStatus.length == 0) {
            return true;
        } else {
            for (ExecutionStatus tempStatus : acceptStatus) {
                if (tempStatus.compareTo(currentStatus) == 0) {
                    return true;
                }
            }
        }

        String message = buildExceptionMessage(stateMachineInstance, acceptStatus, denyStatus, status, compenStatus,
            operation);
        throw new EngineExecutionException(message, FrameworkErrorCode.OperationDenied);
    }

    private String buildExceptionMessage(StateMachineInstance stateMachineInstance, ExecutionStatus[] acceptStatus,
                                         ExecutionStatus[] denyStatus, ExecutionStatus status,
                                         ExecutionStatus compenStatus, String operation) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("StateMachineInstance[id:").append(stateMachineInstance.getId()).append("]");
        if (acceptStatus != null) {
            stringBuilder.append(",acceptable status :");
            for (ExecutionStatus tempStatus : acceptStatus) {
                stringBuilder.append(tempStatus.toString());
                stringBuilder.append(" ");
            }
        }
        if (denyStatus != null) {
            stringBuilder.append(",deny status:");
            for (ExecutionStatus tempStatus : denyStatus) {
                stringBuilder.append(tempStatus.toString());
                stringBuilder.append(" ");
            }
        }
        if (status != null) {
            stringBuilder.append(",current status:");
            stringBuilder.append(status.toString());
        }
        if (compenStatus != null) {
            stringBuilder.append(",current compensation status:");
            stringBuilder.append(compenStatus.toString());
        }
        stringBuilder.append(",so operation [").append(operation).append("] denied");
        return stringBuilder.toString();
    }

    private void putBusinesskeyToContextariables(StateMachineInstance stateMachineInstance,
                                                 Map<String, Object> contextVariables) {
        if (StringUtils.hasText(stateMachineInstance.getBusinessKey()) && !contextVariables.containsKey(
            DomainConstants.VAR_NAME_BUSINESSKEY)) {
            contextVariables.put(DomainConstants.VAR_NAME_BUSINESSKEY, stateMachineInstance.getBusinessKey());
        }
    }

    @Override
    public StateMachineConfig getStateMachineConfig() {
        return stateMachineConfig;
    }

    public void setStateMachineConfig(StateMachineConfig stateMachineConfig) {
        this.stateMachineConfig = stateMachineConfig;
    }
}