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
package io.seata.saga.statelang.domain.impl;

import java.util.Date;

import io.seata.common.util.StringUtils;
import io.seata.saga.statelang.domain.ExecutionStatus;
import io.seata.saga.statelang.domain.StateInstance;
import io.seata.saga.statelang.domain.StateMachineInstance;

/**
 * state execution instance
 *
 * @author lorne.cl
 */
public class StateInstanceImpl implements StateInstance {

    private String id;
    private String machineInstanceId;
    private String name;
    private String type;
    private String serviceName;
    private String serviceMethod;
    private String serviceType;
    private String businessKey;
    private Date gmtStarted;
    private Date gmtEnd;
    private boolean isForUpdate;
    private Exception exception;
    private Object serializedException;
    private Object inputParams;
    private Object serializedInputParams;
    private Object outputParams;
    private Object serializedOutputParams;
    private ExecutionStatus status;
    private String stateIdCompensatedFor;
    private String stateIdRetriedFor;
    private StateInstance compensationState;
    private StateMachineInstance stateMachineInstance;
    private boolean ignoreStatus;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getMachineInstanceId() {
        return machineInstanceId;
    }

    @Override
    public void setMachineInstanceId(String machineInstanceId) {
        this.machineInstanceId = machineInstanceId;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String getServiceName() {
        return serviceName;
    }

    @Override
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    @Override
    public String getServiceMethod() {
        return serviceMethod;
    }

    @Override
    public void setServiceMethod(String serviceMethod) {
        this.serviceMethod = serviceMethod;
    }

    @Override
    public String getServiceType() {
        return serviceType;
    }

    @Override
    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    @Override
    public String getBusinessKey() {
        return businessKey;
    }

    @Override
    public void setBusinessKey(String businessKey) {
        this.businessKey = businessKey;
    }

    @Override
    public Date getGmtStarted() {
        return gmtStarted;
    }

    @Override
    public void setGmtStarted(Date gmtStarted) {
        this.gmtStarted = gmtStarted;
    }

    @Override
    public Date getGmtEnd() {
        return gmtEnd;
    }

    @Override
    public void setGmtEnd(Date gmtEnd) {
        this.gmtEnd = gmtEnd;
    }

    @Override
    public boolean isForUpdate() {
        return isForUpdate;
    }

    @Override
    public void setForUpdate(boolean forUpdate) {
        isForUpdate = forUpdate;
    }

    @Override
    public String getStateIdCompensatedFor() {
        return stateIdCompensatedFor;
    }

    @Override
    public void setStateIdCompensatedFor(String stateIdCompensatedFor) {
        this.stateIdCompensatedFor = stateIdCompensatedFor;
    }

    @Override
    public String getStateIdRetriedFor() {
        return stateIdRetriedFor;
    }

    @Override
    public void setStateIdRetriedFor(String stateIdRetriedFor) {
        this.stateIdRetriedFor = stateIdRetriedFor;
    }

    @Override
    public Exception getException() {
        return exception;
    }

    @Override
    public void setException(Exception exception) {
        this.exception = exception;
    }

    @Override
    public Object getInputParams() {
        return inputParams;
    }

    @Override
    public void setInputParams(Object inputParams) {
        this.inputParams = inputParams;
    }

    @Override
    public Object getOutputParams() {
        return outputParams;
    }

    @Override
    public void setOutputParams(Object outputParams) {
        this.outputParams = outputParams;
    }

    @Override
    public ExecutionStatus getStatus() {
        return status;
    }

    @Override
    public void setStatus(ExecutionStatus status) {
        this.status = status;
    }

    @Override
    public StateInstance getCompensationState() {
        return compensationState;
    }

    @Override
    public void setCompensationState(StateInstance compensationState) {
        this.compensationState = compensationState;
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
        return ignoreStatus;
    }

    @Override
    public void setIgnoreStatus(boolean ignoreStatus) {
        this.ignoreStatus = ignoreStatus;
    }

    @Override
    public boolean isForCompensation() {
        return StringUtils.isNotBlank(this.stateIdCompensatedFor);
    }

    @Override
    public Object getSerializedInputParams() {
        return serializedInputParams;
    }

    @Override
    public void setSerializedInputParams(Object serializedInputParams) {
        this.serializedInputParams = serializedInputParams;
    }

    @Override
    public Object getSerializedOutputParams() {
        return serializedOutputParams;
    }

    @Override
    public void setSerializedOutputParams(Object serializedOutputParams) {
        this.serializedOutputParams = serializedOutputParams;
    }

    @Override
    public Object getSerializedException() {
        return serializedException;
    }

    @Override
    public void setSerializedException(Object serializedException) {
        this.serializedException = serializedException;
    }

    @Override
    public ExecutionStatus getCompensationStatus() {
        if (this.compensationState != null) {
            return this.compensationState.getStatus();
        } else {
            return null;
        }
    }
}