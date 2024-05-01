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
package io.seata.saga.statelang.domain.impl;

import java.util.AbstractMap;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.seata.saga.statelang.domain.ExecutionStatus;
import io.seata.saga.statelang.domain.StateInstance;
import io.seata.saga.statelang.domain.StateMachine;
import io.seata.saga.statelang.domain.StateMachineInstance;

/**
 * state machine execution instance
 */
@Deprecated
public class StateMachineInstanceImpl implements StateMachineInstance {

    private final org.apache.seata.saga.statelang.domain.StateMachineInstance actual;

    private StateMachineInstanceImpl(org.apache.seata.saga.statelang.domain.StateMachineInstance actual) {
        this.actual = actual;
    }

    @Override
    public String getId() {
        return actual.getId();
    }

    @Override
    public void setId(String id) {
        actual.setId(id);
    }

    @Override
    public String getMachineId() {
        return actual.getMachineId();
    }

    @Override
    public void setMachineId(String machineId) {
        actual.setMachineId(machineId);
    }

    @Override
    public String getTenantId() {
        return actual.getTenantId();
    }

    @Override
    public void setTenantId(String tenantId) {
        actual.setTenantId(tenantId);
    }

    @Override
    public String getParentId() {
        return actual.getParentId();
    }

    @Override
    public void setParentId(String parentId) {
        actual.setParentId(parentId);
    }

    @Override
    public Date getGmtStarted() {
        return actual.getGmtStarted();
    }

    @Override
    public void setGmtStarted(Date gmtStarted) {
        actual.setGmtStarted(gmtStarted);
    }

    @Override
    public Date getGmtEnd() {
        return actual.getGmtEnd();
    }

    @Override
    public void setGmtEnd(Date gmtEnd) {
        actual.setGmtEnd(gmtEnd);
    }

    @Override
    public void putStateInstance(String stateId, StateInstance stateInstance) {
        actual.putStateInstance(stateId, ((StateInstanceImpl) stateInstance).unwrap());
    }

    @Override
    public ExecutionStatus getStatus() {
        return ExecutionStatus.wrap(actual.getStatus());
    }

    @Override
    public void setStatus(ExecutionStatus status) {
        if (status == null) {
            actual.setStatus(null);
        } else {
            actual.setStatus(status.unwrap());
        }
    }

    @Override
    public ExecutionStatus getCompensationStatus() {
        return ExecutionStatus.wrap(actual.getCompensationStatus());
    }

    @Override
    public void setCompensationStatus(ExecutionStatus compensationStatus) {
        if (compensationStatus == null) {
            actual.setCompensationStatus(null);
        } else {
            actual.setCompensationStatus(compensationStatus.unwrap());
        }
    }

    @Override
    public boolean isRunning() {
        return actual.isRunning();
    }

    @Override
    public void setRunning(boolean running) {
        actual.setRunning(running);
    }

    @Override
    public Date getGmtUpdated() {
        return actual.getGmtUpdated();
    }

    @Override
    public void setGmtUpdated(Date gmtUpdated) {
        actual.setGmtUpdated(gmtUpdated);
    }

    @Override
    public String getBusinessKey() {
        return actual.getBusinessKey();
    }

    @Override
    public void setBusinessKey(String businessKey) {
        actual.setBusinessKey(businessKey);
    }

    @Override
    public Exception getException() {
        return actual.getException();
    }

    @Override
    public void setException(Exception exception) {
        actual.setException(exception);
    }

    @Override
    public Map<String, Object> getStartParams() {
        return actual.getStartParams();
    }

    @Override
    public void setStartParams(Map<String, Object> startParams) {
        actual.setStartParams(startParams);
    }

    @Override
    public Map<String, Object> getEndParams() {
        return actual.getEndParams();
    }

    @Override
    public void setEndParams(Map<String, Object> endParams) {
        actual.setEndParams(endParams);
    }

    @Override
    public Map<String, Object> getContext() {
        return actual.getContext();
    }

    @Override
    public void setContext(Map<String, Object> context) {
        actual.setContext(context);
    }

    @Override
    public StateMachine getStateMachine() {
        return StateMachineImpl.wrap(actual.getStateMachine());
    }

    @Override
    public void setStateMachine(StateMachine stateMachine) {
        org.apache.seata.saga.statelang.domain.StateMachine unwrap = ((StateMachineImpl) stateMachine).unwrap();
        actual.setStateMachine(unwrap);
    }

    @Override
    public List<StateInstance> getStateList() {
        List<StateInstance> stateList = actual.getStateList().stream()
                .map(StateInstanceImpl::wrap).collect(Collectors.toList());
        stateList.forEach(state -> state.setStateMachineInstance(this));
        return stateList;
    }

    @Override
    public void setStateList(List<StateInstance> stateList) {
        List<org.apache.seata.saga.statelang.domain.StateInstance> actualStateList = stateList.stream()
                .map(state -> ((StateInstanceImpl) state).unwrap()).collect(Collectors.toList());

        actual.setStateList(actualStateList);
    }

    @Override
    public Map<String, StateInstance> getStateMap() {
        List<StateInstance> stateList = actual.getStateList().stream()
                .map(StateInstanceImpl::wrap).collect(Collectors.toList());
        stateList.forEach(state -> state.setStateMachineInstance(this));
        return stateList.stream()
                .collect(Collectors.toMap(StateInstance::getId, Function.identity()));
    }

    @Override
    public void setStateMap(Map<String, StateInstance> stateMap) {
        Map<String, org.apache.seata.saga.statelang.domain.StateInstance> actualStateMap = stateMap.entrySet().stream()
                .map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), ((StateInstanceImpl) e.getValue()).unwrap()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        actual.setStateMap(actualStateMap);
    }

    @Override
    public Object getSerializedStartParams() {
        return actual.getSerializedStartParams();
    }

    @Override
    public void setSerializedStartParams(Object serializedStartParams) {
        actual.setSerializedStartParams(serializedStartParams);
    }

    @Override
    public Object getSerializedEndParams() {
        return actual.getSerializedEndParams();
    }

    @Override
    public void setSerializedEndParams(Object serializedEndParams) {
        actual.setSerializedEndParams(serializedEndParams);
    }

    @Override
    public Object getSerializedException() {
        return actual.getSerializedException();
    }

    @Override
    public void setSerializedException(Object serializedException) {
        actual.setSerializedException(serializedException);
    }

    public static StateMachineInstanceImpl wrap(org.apache.seata.saga.statelang.domain.StateMachineInstance target) {
        return new StateMachineInstanceImpl(target);
    }

    public org.apache.seata.saga.statelang.domain.StateMachineInstance unwrap() {
        return actual;
    }
}
