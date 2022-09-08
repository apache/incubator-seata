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
package io.seata.saga.engine.store;

import java.util.List;

import io.seata.saga.proctrl.ProcessContext;
import io.seata.saga.statelang.domain.StateInstance;
import io.seata.saga.statelang.domain.StateMachineInstance;

/**
 * StateMachine engine log store
 *
 * @author lorne.cl
 */
public interface StateLogStore {

    /**
     * Record state machine startup events
     *
     * @param machineInstance the state machine instance
     * @param context the state machine process context
     */
    void recordStateMachineStarted(StateMachineInstance machineInstance, ProcessContext context);

    /**
     * Record status end event
     *
     * @param machineInstance the state machine instance
     * @param context the state machine process context
     */
    void recordStateMachineFinished(StateMachineInstance machineInstance, ProcessContext context);

    /**
     * Record state machine restarted
     *
     * @param machineInstance the state machine instance
     * @param context the state machine process context
     */
    void recordStateMachineRestarted(StateMachineInstance machineInstance, ProcessContext context);

    /**
     * Record state start execution event
     *
     * @param stateInstance the state machine instance
     * @param context the state machine process context
     */
    void recordStateStarted(StateInstance stateInstance, ProcessContext context);

    /**
     * Record state execution end event
     *
     * @param stateInstance the state machine instance
     * @param context the state machine process context
     */
    void recordStateFinished(StateInstance stateInstance, ProcessContext context);

    /**
     * Get state machine instance
     *
     * @param stateMachineInstanceId the state machine instance id
     * @return the state machine instance
     */
    StateMachineInstance getStateMachineInstance(String stateMachineInstanceId);

    /**
     * Get state machine instance by businessKey
     *
     * @param businessKey the businessKey
     * @param tenantId the tenant id
     * @return state machine message
     */
    StateMachineInstance getStateMachineInstanceByBusinessKey(String businessKey, String tenantId);

    /**
     * Query the list of state machine instances by parent id
     *
     * @param parentId the state machine parent's id
     * @return the state machine instance list
     */
    List<StateMachineInstance> queryStateMachineInstanceByParentId(String parentId);

    /**
     * Get state instance
     *
     * @param stateInstanceId the state instance id
     * @param machineInstId the machine instance id
     * @return state instance message
     */
    StateInstance getStateInstance(String stateInstanceId, String machineInstId);

    /**
     * Get a list of state instances by state machine instance id
     *
     * @param stateMachineInstanceId the state machine instance id
     * @return state instance list
     */
    List<StateInstance> queryStateInstanceListByMachineInstanceId(String stateMachineInstanceId);

    /**
     * clear the LocalThread
     */
    void clearUp();
}