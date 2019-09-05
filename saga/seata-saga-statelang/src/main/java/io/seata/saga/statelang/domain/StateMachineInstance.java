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
package io.seata.saga.statelang.domain;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * StateMachine execution instance
 * @author lorne.cl
 */
public interface StateMachineInstance {

    /**
     * Gets get id.
     *
     * @return the get id
     */
    String getId();

    /**
     * Sets set id.
     *
     * @param id the id
     */
    void setId(String id);

    /**
     * Gets get machine id.
     *
     * @return the get machine id
     */
    String getMachineId();

    /**
     * Sets set machine id.
     *
     * @param machineId the machine id
     */
    void setMachineId(String machineId);

    /**
     * Gets get tenant id.
     *
     * @return the tenant id
     */
    String getTenantId();

    /**
     * Sets set tenant id.
     *
     * @param tenantId the tenant id
     */
    void setTenantId(String tenantId);

    /**
     * Gets get parent id.
     *
     * @return the get parent id
     */
    String getParentId();

    /**
     * Sets set parent id.
     *
     * @param parentId the parent id
     */
    void setParentId(String parentId);

    /**
     * Gets get gmt started.
     *
     * @return the get gmt started
     */
    Date getGmtStarted();

    /**
     * Sets set gmt started.
     *
     * @param gmtStarted the gmt started
     */
    void setGmtStarted(Date gmtStarted);

    /**
     * Gets get gmt end.
     *
     * @return the get gmt end
     */
    Date getGmtEnd();

    /**
     * Sets set gmt end.
     *
     * @param gmtEnd the gmt end
     */
    void setGmtEnd(Date gmtEnd);

    /**
     * Put state instance.
     *
     * @param stateId the state id
     * @param stateInstance the state instance
     */
    void putStateInstance(String stateId, StateInstance stateInstance);

    /**
     * Gets get status.
     *
     * @return the get status
     */
    ExecutionStatus getStatus();

    /**
     * Sets set status.
     *
     * @param status the status
     */
    void setStatus(ExecutionStatus status);

    /**
     * Gets get compensation status.
     *
     * @return the get compensation status
     */
    ExecutionStatus getCompensationStatus();

    /**
     * Sets set compensation status.
     *
     * @param compensationStatus the compensation status
     */
    void setCompensationStatus(ExecutionStatus compensationStatus);

    /**
     * Is running boolean.
     *
     * @return the boolean
     */
    boolean isRunning();

    /**
     * Sets set running.
     *
     * @param running the running
     */
    void setRunning(boolean running);

    /**
     * Gets get gmt updated.
     *
     * @return the get gmt updated
     */
    Date getGmtUpdated();

    /**
     * Sets set gmt updated.
     *
     * @param gmtUpdated the gmt updated
     */
    void setGmtUpdated(Date gmtUpdated);

    /**
     * Gets get business key.
     *
     * @return the get business key
     */
    String getBusinessKey();

    /**
     * Sets set business key.
     *
     * @param businessKey the business key
     */
    void setBusinessKey(String businessKey);

    /**
     * Gets get exception.
     *
     * @return the get exception
     */
    Exception getException();

    /**
     * Sets set exception.
     *
     * @param exception the exception
     */
    void setException(Exception exception);

    /**
     * Gets get start params.
     *
     * @return the get start params
     */
    Map<String, Object> getStartParams();

    /**
     * Sets set start params.
     *
     * @param startParams the start params
     */
    void setStartParams(Map<String, Object> startParams);

    /**
     * Gets get end params.
     *
     * @return the get end params
     */
    Map<String, Object> getEndParams();

    /**
     * Sets set end params.
     *
     * @param endParams the end params
     */
    void setEndParams(Map<String, Object> endParams);

    /**
     * Gets get context.
     * @return
     */
    Map<String, Object> getContext();

    /**
     * Sets set context.
     *
     * @param context
     */
    void setContext(Map<String, Object> context);

    /**
     * Gets get state machine.
     *
     * @return the get state machine
     */
    StateMachine getStateMachine();

    /**
     * Sets set state machine.
     *
     * @param stateMachine the state machine
     */
    void setStateMachine(StateMachine stateMachine);

    /**
     * Gets get state list.
     *
     * @return the get state list
     */
    List<StateInstance> getStateList();

    /**
     * Sets set state list.
     *
     * @param stateList the state list
     */
    void setStateList(List<StateInstance> stateList);

    /**
     * Gets get state map.
     *
     * @return the get state map
     */
    Map<String, StateInstance> getStateMap();

    /**
     * Sets set state map.
     *
     * @param stateMap the state map
     */
    void setStateMap(Map<String, StateInstance> stateMap);

    /**
     * Gets get serialized start params.
     *
     * @return the get serialized start params
     */
    Object getSerializedStartParams();

    /**
     * Sets set serialized start params.
     *
     * @param serializedStartParams the serialized start params
     */
    void setSerializedStartParams(Object serializedStartParams);

    /**
     * Gets get serialized end params.
     *
     * @return the get serialized end params
     */
    Object getSerializedEndParams();

    /**
     * Sets set serialized end params.
     *
     * @param serializedEndParams the serialized end params
     */
    void setSerializedEndParams(Object serializedEndParams);

    /**
     * Gets get serialized exception.
     *
     * @return the get serialized exception
     */
    Object getSerializedException();

    /**
     * Sets set serialized exception.
     *
     * @param serializedException the serialized exception
     */
    void setSerializedException(Object serializedException);
}