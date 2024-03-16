package io.seata.saga.engine.store;


import io.seata.saga.proctrl.ProcessContext;
import io.seata.saga.statelang.domain.StateInstance;
import io.seata.saga.statelang.domain.StateMachineInstance;

import java.util.List;

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
    void clearUp(ProcessContext context);
}
