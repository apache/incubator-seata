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
package io.seata.saga.engine;

import java.util.Map;

import io.seata.saga.engine.exception.EngineExecutionException;
import io.seata.saga.engine.exception.ForwardInvalidException;
import io.seata.saga.statelang.domain.StateMachineInstance;

/**
 * State machine engine
 *
 * @author lorne.cl
 */
public interface StateMachineEngine {

    /**
     * start a state machine instance
     *
     * @param stateMachineName the state machine name
     * @param tenantId the tenant id
     * @param startParams the start params
     * @return the state machine instance
     * @throws EngineExecutionException the engine execution exception
     */
    StateMachineInstance start(String stateMachineName, String tenantId, Map<String, Object> startParams)
        throws EngineExecutionException;

    /**
     * start a state machine instance with businessKey
     *
     * @param stateMachineName the state machine name
     * @param tenantId the tenant id
     * @param businessKey the businessKey
     * @param startParams the start params
     * @return the state machine instance
     * @throws EngineExecutionException the engine execution exception
     */
    StateMachineInstance startWithBusinessKey(String stateMachineName, String tenantId, String businessKey,
                                              Map<String, Object> startParams) throws EngineExecutionException;

    /**
     * start a state machine instance asynchronously
     *
     * @param stateMachineName the state machine name
     * @param tenantId the tenant id
     * @param startParams the start params
     * @param callback callback after start machine
     * @return the state machine instance
     * @throws EngineExecutionException the engine execution exception
     */
    StateMachineInstance startAsync(String stateMachineName, String tenantId, Map<String, Object> startParams,
                                    AsyncCallback callback) throws EngineExecutionException;

    /**
     * start a state machine instance asynchronously with businessKey
     *
     * @param stateMachineName the state machine name
     * @param tenantId the tenant id
     * @param businessKey the businessKey
     * @param startParams the start params
     * @param callback the callback after start a state machine
     * @return the state machine instance
     * @throws EngineExecutionException the engine execution exception
     */
    StateMachineInstance startWithBusinessKeyAsync(String stateMachineName, String tenantId, String businessKey,
                                                   Map<String, Object> startParams, AsyncCallback callback)
        throws EngineExecutionException;

    /**
     * forward restart a failed state machine instance
     *
     * @param stateMachineInstId the state machine instance id
     * @param replaceParams the replace params
     * @return the state machine instance
     * @throws ForwardInvalidException forward invalid exception
     */
    StateMachineInstance forward(String stateMachineInstId, Map<String, Object> replaceParams)
        throws ForwardInvalidException;

    /**
     * forward restart a failed state machine instance asynchronously
     *
     * @param stateMachineInstId the state machine instance id
     * @param replaceParams the replace params
     * @param callback callback after forward restart a failed state machine
     * @return the state machine instance
     * @throws ForwardInvalidException the forward invalid exception
     */
    StateMachineInstance forwardAsync(String stateMachineInstId, Map<String, Object> replaceParams,
                                      AsyncCallback callback) throws ForwardInvalidException;

    /**
     * compensate a state machine instance
     *
     * @param stateMachineInstId the state machine id
     * @param replaceParams the replace params
     * @return the state machine instance
     * @throws EngineExecutionException the engin execution exception
     */
    StateMachineInstance compensate(String stateMachineInstId, Map<String, Object> replaceParams)
        throws EngineExecutionException;

    /**
     * compensate a state machine instance asynchronously
     *
     * @param stateMachineInstId the state machine instance id
     * @param replaceParams the replace params
     * @param callback callback after compensate a failed state machine
     * @return the state machine instance
     * @throws EngineExecutionException the engine execution exception
     */
    StateMachineInstance compensateAsync(String stateMachineInstId, Map<String, Object> replaceParams,
                                         AsyncCallback callback) throws EngineExecutionException;

    /**
     * skip current failed state instance and forward restart state machine instance
     *
     * @param stateMachineInstId the state machine instance id
     * @return the state machine instance
     * @throws EngineExecutionException the engine execution exception
     */
    StateMachineInstance skipAndForward(String stateMachineInstId, Map<String, Object> replaceParams) throws EngineExecutionException;

    /**
     * skip current failed state instance and forward restart state machine instance asynchronously
     *
     * @param stateMachineInstId the state machine instance id
     * @param callback callback after skip and forward restart a failed state machine
     * @return the state machine instance
     * @throws EngineExecutionException the engine execution exception
     */
    StateMachineInstance skipAndForwardAsync(String stateMachineInstId, AsyncCallback callback)
        throws EngineExecutionException;

    /**
     * get state machine configurations
     *
     * @return the state machine configurations
     */
    StateMachineConfig getStateMachineConfig();

    /**
     * Reload StateMachine Instance
     * @param instId the state machine instance id
     * @return the state machine instance
     */
    StateMachineInstance reloadStateMachineInstance(String instId);
}