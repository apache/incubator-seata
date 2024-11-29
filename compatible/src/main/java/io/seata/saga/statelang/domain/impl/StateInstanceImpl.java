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

import java.util.Date;

import io.seata.saga.statelang.domain.ExecutionStatus;
import io.seata.saga.statelang.domain.StateInstance;
import io.seata.saga.statelang.domain.StateMachineInstance;
import io.seata.saga.statelang.domain.StateType;

/**
 * state execution instance
 */
@Deprecated
public class StateInstanceImpl implements StateInstance {

    private final org.apache.seata.saga.statelang.domain.StateInstance actual;

    private StateMachineInstance stateMachineInstance;

    private StateInstanceImpl(org.apache.seata.saga.statelang.domain.StateInstance actual) {
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
    public String getMachineInstanceId() {
        return actual.getMachineInstanceId();
    }

    @Override
    public void setMachineInstanceId(String machineInstanceId) {
        actual.setMachineInstanceId(machineInstanceId);
    }

    @Override
    public String getName() {
        return actual.getName();
    }

    @Override
    public void setName(String name) {
        actual.setName(name);
    }

    @Override
    public StateType getType() {
        return StateType.wrap(actual.getType());
    }

    @Override
    public void setType(StateType type) {
        if (type == null) {
            actual.setType(null);
        } else {
            actual.setType(type.unwrap());
        }
    }

    @Override
    public String getServiceName() {
        return actual.getServiceName();
    }

    @Override
    public void setServiceName(String serviceName) {
        actual.setServiceName(serviceName);
    }

    @Override
    public String getServiceMethod() {
        return actual.getServiceMethod();
    }

    @Override
    public void setServiceMethod(String serviceMethod) {
        actual.setServiceMethod(serviceMethod);
    }

    @Override
    public String getServiceType() {
        return actual.getServiceType();
    }

    @Override
    public void setServiceType(String serviceType) {
        actual.setServiceType(serviceType);
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
    public Date getGmtStarted() {
        return actual.getGmtStarted();
    }

    @Override
    public void setGmtStarted(Date gmtStarted) {
        actual.setGmtStarted(gmtStarted);
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
    public Date getGmtEnd() {
        return actual.getGmtEnd();
    }

    @Override
    public void setGmtEnd(Date gmtEnd) {
        actual.setGmtEnd(gmtEnd);
    }

    @Override
    public boolean isForUpdate() {
        return actual.isForUpdate();
    }

    @Override
    public void setForUpdate(boolean forUpdate) {
        actual.setForUpdate(forUpdate);
    }

    @Override
    public String getStateIdCompensatedFor() {
        return actual.getStateIdCompensatedFor();
    }

    @Override
    public void setStateIdCompensatedFor(String stateIdCompensatedFor) {
        actual.setStateIdCompensatedFor(stateIdCompensatedFor);
    }

    @Override
    public String getStateIdRetriedFor() {
        return actual.getStateIdRetriedFor();
    }

    @Override
    public void setStateIdRetriedFor(String stateIdRetriedFor) {
        actual.setStateIdRetriedFor(stateIdRetriedFor);
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
    public Object getInputParams() {
        return actual.getInputParams();
    }

    @Override
    public void setInputParams(Object inputParams) {
        actual.setInputParams(inputParams);
    }

    @Override
    public Object getOutputParams() {
        return actual.getOutputParams();
    }

    @Override
    public void setOutputParams(Object outputParams) {
        actual.setOutputParams(outputParams);
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
    public StateInstance getCompensationState() {
        return wrap(actual.getCompensationState());
    }

    @Override
    public void setCompensationState(StateInstance compensationState) {
        actual.setCompensationState(((StateInstanceImpl)compensationState).unwrap());
    }

    @Override
    public StateMachineInstance getStateMachineInstance() {
        return stateMachineInstance;
    }

    @Override
    public void setStateMachineInstance(StateMachineInstance stateMachineInstance) {
        this.stateMachineInstance = stateMachineInstance;
    }

    @Override
    public boolean isIgnoreStatus() {
        return actual.isIgnoreStatus();
    }

    @Override
    public void setIgnoreStatus(boolean ignoreStatus) {
        actual.setIgnoreStatus(ignoreStatus);
    }

    @Override
    public boolean isForCompensation() {
        return actual.isForCompensation();
    }

    @Override
    public Object getSerializedInputParams() {
        return actual.getSerializedInputParams();
    }

    @Override
    public void setSerializedInputParams(Object serializedInputParams) {
        actual.setSerializedInputParams(serializedInputParams);
    }

    @Override
    public Object getSerializedOutputParams() {
        return actual.getSerializedOutputParams();
    }

    @Override
    public void setSerializedOutputParams(Object serializedOutputParams) {
        actual.setSerializedOutputParams(serializedOutputParams);
    }

    @Override
    public Object getSerializedException() {
        return actual.getSerializedException();
    }

    @Override
    public void setSerializedException(Object serializedException) {
        actual.setSerializedException(serializedException);
    }

    @Override
    public ExecutionStatus getCompensationStatus() {
        return ExecutionStatus.wrap(actual.getCompensationStatus());
    }

    public static StateInstanceImpl wrap(org.apache.seata.saga.statelang.domain.StateInstance target) {
        return new StateInstanceImpl(target);
    }

    public org.apache.seata.saga.statelang.domain.StateInstance unwrap() {
        return actual;
    }
}
