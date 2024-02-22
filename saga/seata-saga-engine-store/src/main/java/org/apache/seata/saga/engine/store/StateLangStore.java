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