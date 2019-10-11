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

import io.seata.saga.statelang.domain.ExecutionStatus;
import io.seata.saga.statelang.domain.StateInstance;
import io.seata.saga.statelang.domain.StateMachine;
import io.seata.saga.statelang.domain.StateMachineInstance;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * state machine execution instance
 * @author lorne.cl
 */
public class StateMachineInstanceImpl implements StateMachineInstance {

    private String              id;
    private String              machineId;
    private String              tenantId;
    private String              parentId;
    private Date                gmtStarted;
    private String              businessKey;
    private Map<String, Object> startParams = new HashMap<>();
    private Object              serializedStartParams;
    private Date                gmtEnd;
    private Exception           exception;
    private Object              serializedException;
    private Map<String, Object> endParams = new HashMap<>();
    private Object              serializedEndParams;
    private ExecutionStatus     status;
    private ExecutionStatus     compensationStatus;
    private boolean             isRunning;
    private Date                gmtUpdated;
    private Map<String, Object> context;

    private StateMachine                     stateMachine;
    private List<StateInstance>              stateList       = Collections.synchronizedList(new ArrayList<>());
    private Map<String, StateInstance>       stateMap        = new ConcurrentHashMap<>();

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getMachineId() {
        return machineId;
    }

    @Override
    public void setMachineId(String machineId) {
        this.machineId = machineId;
    }

    @Override
    public String getTenantId() {
        return tenantId;
    }

    @Override
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    @Override
    public String getParentId() {
        return parentId;
    }

    @Override
    public void setParentId(String parentId) {
        this.parentId = parentId;
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
    public void putStateInstance(String stateId, StateInstance stateInstance) {
        stateInstance.setStateMachineInstance(this);
        stateMap.put(stateId, stateInstance);
        stateList.add(stateInstance);
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
    public ExecutionStatus getCompensationStatus() {
        return compensationStatus;
    }

    @Override
    public void setCompensationStatus(ExecutionStatus compensationStatus) {
        this.compensationStatus = compensationStatus;
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }

    @Override
    public void setRunning(boolean running) {
        isRunning = running;
    }

    @Override
    public Date getGmtUpdated() {
        return gmtUpdated;
    }

    @Override
    public void setGmtUpdated(Date gmtUpdated) {
        this.gmtUpdated = gmtUpdated;
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
    public Exception getException() {
        return exception;
    }

    @Override
    public void setException(Exception exception) {
        this.exception = exception;
    }

    @Override
    public Map<String, Object> getStartParams() {
        return startParams;
    }

    @Override
    public void setStartParams(Map<String, Object> startParams) {
        this.startParams = startParams;
    }

    @Override
    public Map<String, Object> getEndParams() {
        return endParams;
    }

    @Override
    public void setEndParams(Map<String, Object> endParams) {
        this.endParams = endParams;
    }

    @Override
    public Map<String, Object> getContext() {
        return context;
    }

    @Override
    public void setContext(Map<String, Object> context) {
        this.context = context;
    }

    @Override
    public StateMachine getStateMachine() {
        return stateMachine;
    }

    @Override
    public void setStateMachine(StateMachine stateMachine) {
        this.stateMachine = stateMachine;
    }

    @Override
    public List<StateInstance> getStateList() {
        return stateList;
    }

    @Override
    public void setStateList(List<StateInstance> stateList) {
        this.stateList = stateList;
    }

    @Override
    public Map<String, StateInstance> getStateMap() {
        return stateMap;
    }

    @Override
    public void setStateMap(Map<String, StateInstance> stateMap) {
        this.stateMap = stateMap;
    }

    @Override
    public Object getSerializedStartParams() {
        return serializedStartParams;
    }

    @Override
    public void setSerializedStartParams(Object serializedStartParams) {
        this.serializedStartParams = serializedStartParams;
    }

    @Override
    public Object getSerializedEndParams() {
        return serializedEndParams;
    }

    @Override
    public void setSerializedEndParams(Object serializedEndParams) {
        this.serializedEndParams = serializedEndParams;
    }

    @Override
    public Object getSerializedException() {
        return serializedException;
    }

    @Override
    public void setSerializedException(Object serializedException) {
        this.serializedException = serializedException;
    }
}