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
package org.apache.seata.saga.engine.store;

import org.apache.seata.saga.statelang.domain.StateMachine;

/**
 * State language definition store
 *
 */
public interface StateLangStore {

    /**
     * Query the state machine definition by id
     *
     * @param stateMachineId the state machine id
     * @return the state machine message
     */
    StateMachine getStateMachineById(String stateMachineId);

    /**
     * Get the latest version of the state machine by state machine name
     *
     * @param stateMachineName the state machine name
     * @param tenantId the tenant id
     * @return the state machine message
     */
    StateMachine getLastVersionStateMachine(String stateMachineName, String tenantId);

    /**
     * Storage state machine definition
     *
     * @param stateMachine the state machine message
     * @return whether the store state machine action is successful
     */
    boolean storeStateMachine(StateMachine stateMachine);
}
