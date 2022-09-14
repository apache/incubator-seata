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
package io.seata.saga.engine.repo;

import java.util.List;

import io.seata.saga.statelang.domain.StateInstance;
import io.seata.saga.statelang.domain.StateMachineInstance;

/**
 * State Log Repository
 *
 * @author lorne.cl
 */
public interface StateLogRepository {

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
     * @param businessKey the business key
     * @param tenantId the tenant id
     * @return the state machine instance
     */
    StateMachineInstance getStateMachineInstanceByBusinessKey(String businessKey, String tenantId);

    /**
     * Query the list of state machine instances by parent id
     *
     * @param parentId the state parent id
     * @return state machine instance list
     */
    List<StateMachineInstance> queryStateMachineInstanceByParentId(String parentId);

    /**
     * Get state instance
     *
     * @param stateInstanceId the state instance id
     * @param machineInstId the state machine instance id
     * @return the state instance
     */
    StateInstance getStateInstance(String stateInstanceId, String machineInstId);

    /**
     * Get a list of state instances by state machine instance id
     *
     * @param stateMachineInstanceId the state machine instance id
     * @return the state machine instance list
     */
    List<StateInstance> queryStateInstanceListByMachineInstanceId(String stateMachineInstanceId);
}