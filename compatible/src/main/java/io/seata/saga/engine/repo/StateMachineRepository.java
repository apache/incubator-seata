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
package io.seata.saga.engine.repo;

import java.io.IOException;
import java.io.InputStream;

import io.seata.saga.statelang.domain.StateMachine;

/**
 * StateMachineRepository
 */
@Deprecated
public interface StateMachineRepository {

    /**
     * Gets get state machine by id.
     *
     * @param stateMachineId the state machine id
     * @return the get state machine by id
     */
    StateMachine getStateMachineById(String stateMachineId);

    /**
     * Gets get state machine.
     *
     * @param stateMachineName the state machine name
     * @param tenantId         the tenant id
     * @return the get state machine
     */
    StateMachine getStateMachine(String stateMachineName, String tenantId);

    /**
     * Gets get state machine.
     *
     * @param stateMachineName the state machine name
     * @param tenantId         the tenant id
     * @param version          the version
     * @return the get state machine
     */
    StateMachine getStateMachine(String stateMachineName, String tenantId, String version);

    /**
     * Register the state machine to the repository (if the same version already exists, return the existing version)
     *
     * @param stateMachine stateMachine
     * @return the state machine
     */
    StateMachine registryStateMachine(StateMachine stateMachine);

    /**
     * Registry by resources.
     *
     * @param resourceAsStreamArray the resource as stream array
     * @param tenantId              the tenant id
     * @throws IOException the io exception
     */
    void registryByResources(InputStream[] resourceAsStreamArray, String tenantId) throws IOException;
}
