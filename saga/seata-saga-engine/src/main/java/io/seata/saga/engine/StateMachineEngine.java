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

import io.seata.saga.engine.exception.EngineExecutionException;
import io.seata.saga.engine.exception.ForwardInvalidException;
import io.seata.saga.statelang.domain.StateMachineInstance;
import java.util.Map;

/**
 * State machine engine
 *
 * @author lorne.cl
 */
public interface StateMachineEngine {

    /**
     * start a state machine instance
     * @param stateMachineName
     * @param tenantId
     * @param startParams
     * @return
     * @throws EngineExecutionException
     */
    StateMachineInstance start(String stateMachineName, String tenantId, Map<String, Object> startParams) throws EngineExecutionException;

    /**
     * start a state machine instance with businessKey
     * @param stateMachineName
     * @param tenantId
     * @param businessKey
     * @param startParams
     * @return
     * @throws EngineExecutionException
     */
    StateMachineInstance startWithBusinessKey(String stateMachineName, String tenantId, String businessKey, Map<String, Object> startParams) throws EngineExecutionException;

    /**
     * start a state machine instance asynchronously
     * @param stateMachineName
     * @param tenantId
     * @param startParams
     * @param callback
     * @return
     * @throws EngineExecutionException
     */
    StateMachineInstance startAsync(String stateMachineName, String tenantId, Map<String, Object> startParams, AsyncCallback callback) throws EngineExecutionException;

    /**
     * start a state machine instance asynchronously with businessKey
     * @param stateMachineName
     * @param tenantId
     * @param businessKey
     * @param startParams
     * @param callback
     * @return
     * @throws EngineExecutionException
     */
    StateMachineInstance startWithBusinessKeyAsync(String stateMachineName, String tenantId, String businessKey, Map<String, Object> startParams, AsyncCallback callback) throws EngineExecutionException;

    /**
     * forward restart a failed state machine instance
     * @param stateMachineInstId
     * @param replaceParams
     * @return
     * @throws ForwardInvalidException
     */
    StateMachineInstance forward(String stateMachineInstId, Map<String, Object> replaceParams) throws ForwardInvalidException;

    /**
     * forward restart a failed state machine instance asynchronously
     * @param stateMachineInstId
     * @param replaceParams
     * @param callback
     * @return
     * @throws ForwardInvalidException
     */
    StateMachineInstance forwardAsync(String stateMachineInstId, Map<String, Object> replaceParams, AsyncCallback callback) throws ForwardInvalidException;

    /**
     * compensate a state machine instance
     * @param stateMachineInstId
     * @param replaceParams
     * @return
     * @throws EngineExecutionException
     */
    StateMachineInstance compensate(String stateMachineInstId, Map<String, Object> replaceParams) throws EngineExecutionException;

    /**
     * compensate a state machine instance asynchronously
     * @param stateMachineInstId
     * @param replaceParams
     * @param callback
     * @return
     * @throws EngineExecutionException
     */
    StateMachineInstance compensateAsync(String stateMachineInstId, Map<String, Object> replaceParams, AsyncCallback callback) throws EngineExecutionException;

    /**
     * skip current failed state instance and forward restart state machine instance
     * @param stateMachineInstId
     * @return
     * @throws EngineExecutionException
     */
    StateMachineInstance skipAndForward(String stateMachineInstId) throws EngineExecutionException;

    /**
     * skip current failed state instance and forward restart state machine instance asynchronously
     * @param stateMachineInstId
     * @param callback
     * @return
     * @throws EngineExecutionException
     */
    StateMachineInstance skipAndForwardAsync(String stateMachineInstId, AsyncCallback callback) throws EngineExecutionException;

    /**
     * get state machine configurations
     * @return
     */
    StateMachineConfig getStateMachineConfig();
}