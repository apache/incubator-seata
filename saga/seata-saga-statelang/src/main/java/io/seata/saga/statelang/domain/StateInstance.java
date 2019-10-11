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

/**
 * State execution instance
 *
 * @author lorne.cl
 */
public interface StateInstance {

    /**
     * id
     * @return
     */
    String getId();

    /**
     * set id
     * @param id
     */
    void setId(String id);

    /**
     * get Machine InstanceId
     * @return
     */
    String getMachineInstanceId();

    /**
     * set Machine InstanceId
     * @param machineInstanceId
     */
    void setMachineInstanceId(String machineInstanceId);

    /**
     * get name
     * @return
     */
    String getName();

    /**
     * set name
     *
     * @param name
     */
    void setName(String name);

    /**
     * get type
     * @return
     */
    String getType();

    /**
     * set type
     * @param type
     */
    void setType(String type);

    /**
     * get service name
     * @return
     */
    String getServiceName();

    /**
     * set service name
     * @param serviceName
     */
    void setServiceName(String serviceName);

    /**
     * get service method
     * @return
     */
    String getServiceMethod();

    /**
     * set service method
     * @param serviceMethod
     */
    void setServiceMethod(String serviceMethod);

    /**
     * get service type
     * @return
     */
    String getServiceType();

    /**
     * get service type
     * @param serviceType
     */
    void setServiceType(String serviceType);

    /**
     * get businessKey
     * @return
     */
    String getBusinessKey();

    /**
     * set business key
     * @param businessKey
     */
    void setBusinessKey(String businessKey);

    /**
     * get start time
     * @return
     */
    Date getGmtStarted();

    /**
     * set start time
     * @param gmtStarted
     */
    void setGmtStarted(Date gmtStarted);

    /**
     * get end time
     * @return
     */
    Date getGmtEnd();

    /**
     * set end time
     * @param gmtEnd
     */
    void setGmtEnd(Date gmtEnd);

    /**
     * Is this state task will update data?
     * @return
     */
    boolean isForUpdate();

    /**
     * setForUpdate
     * @param forUpdate
     */
    void setForUpdate(boolean forUpdate);

    /**
     * get exception
     * @return
     */
    Exception getException();

    /**
     * set exception
     * @param exception
     */
    void setException(Exception exception);

    /**
     * get input params
     * @return
     */
    Object getInputParams();

    /**
     * set inout params
     * @param inputParams
     */
    void setInputParams(Object inputParams);

    /**
     * get output params
     * @return
     */
    Object getOutputParams();

    /**
     * Sets set output params.
     *
     * @param outputParams the output params
     */
    void setOutputParams(Object outputParams);

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
     * Gets get state id compensated for.
     *
     * @return the get state id compensated for
     */
    String getStateIdCompensatedFor();

    /**
     * Sets set state id compensated for.
     *
     * @param stateIdCompensatedFor the state id compensated for
     */
    void setStateIdCompensatedFor(String stateIdCompensatedFor);

    /**
     * Gets get state id retried for.
     *
     * @return the get state id retried for
     */
    String getStateIdRetriedFor();

    /**
     * Sets set state id retried for.
     *
     * @param stateIdRetriedFor the state id retried for
     */
    void setStateIdRetriedFor(String stateIdRetriedFor);

    /**
     * Gets get compensation state.
     *
     * @return the get compensation state
     */
    StateInstance getCompensationState();

    /**
     * Sets set compensation state.
     *
     * @param compensationState the compensation state
     */
    void setCompensationState(StateInstance compensationState);

    /**
     * Gets get state machine instance.
     *
     * @return the get state machine instance
     */
    StateMachineInstance getStateMachineInstance();

    /**
     * Sets set state machine instance.
     *
     * @param stateMachineInstance the state machine instance
     */
    void setStateMachineInstance(StateMachineInstance stateMachineInstance);

    /**
     * Is ignore status boolean.
     *
     * @return the boolean
     */
    boolean isIgnoreStatus();

    /**
     * Sets set ignore status.
     *
     * @param ignoreStatus the ignore status
     */
    void setIgnoreStatus(boolean ignoreStatus);

    /**
     * Is for compensation boolean.
     *
     * @return the boolean
     */
    boolean isForCompensation();

    /**
     * Gets get serialized input params.
     *
     * @return the get serialized input params
     */
    Object getSerializedInputParams();

    /**
     * Sets set serialized input params.
     *
     * @param serializedInputParams the serialized input params
     */
    void setSerializedInputParams(Object serializedInputParams);

    /**
     * Gets get serialized output params.
     *
     * @return the get serialized output params
     */
    Object getSerializedOutputParams();

    /**
     * Sets set serialized output params.
     *
     * @param serializedOutputParams the serialized output params
     */
    void setSerializedOutputParams(Object serializedOutputParams);

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

    /**
     * Gets get compensation status.
     *
     * @return the get compensation status
     */
    ExecutionStatus getCompensationStatus();
}