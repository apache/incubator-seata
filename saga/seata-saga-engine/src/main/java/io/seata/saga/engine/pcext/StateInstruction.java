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
package io.seata.saga.engine.pcext;

import io.seata.common.exception.FrameworkErrorCode;
import io.seata.saga.engine.StateMachineConfig;
import io.seata.saga.engine.exception.EngineExecutionException;
import io.seata.saga.proctrl.Instruction;
import io.seata.saga.proctrl.ProcessContext;
import io.seata.saga.statelang.domain.DomainConstants;
import io.seata.saga.statelang.domain.State;
import io.seata.saga.statelang.domain.StateMachine;
import org.springframework.util.StringUtils;

/**
 * State Instruction
 *
 * @see Instruction
 * @author lorne.cl
 */
public class StateInstruction implements Instruction {

    private String  stateName;
    private String  stateMachineName;
    private String  tenantId;
    private boolean end;

    /**
     * Temporary state node for running temporary nodes in the state machine
     */
    private State temporaryState;

    public StateInstruction() {
    }

    public StateInstruction(String stateMachineName, String tenantId) {
        this.stateMachineName = stateMachineName;
        this.tenantId = tenantId;
    }

    public State getState(ProcessContext context){

        if(getTemporaryState() != null){

            return temporaryState;
        }

        String stateName = getStateName();
        String stateMachineName = getStateMachineName();
        String tenantId = getTenantId();

        if (StringUtils.isEmpty(stateMachineName)) {
            throw new EngineExecutionException("StateMachineName is required", FrameworkErrorCode.ParameterRequired);
        }

        StateMachineConfig stateMachineConfig = (StateMachineConfig) context.getVariable(DomainConstants.VAR_NAME_STATEMACHINE_CONFIG);
        StateMachine stateMachine = stateMachineConfig.getStateMachineRepository().getStateMachine(stateMachineName, tenantId);
        if (stateMachine == null) {
            throw new EngineExecutionException("StateMachine[" + stateMachineName + "] is not exist", FrameworkErrorCode.ObjectNotExists);
        }



        if (StringUtils.isEmpty(stateName)) {

            stateName = stateMachine.getStartState();
            setStateName(stateName);
        }

        State state = stateMachine.getStates().get(stateName);
        if (state == null) {
            throw new EngineExecutionException("State[" + stateName + "] is not exist", FrameworkErrorCode.ObjectNotExists);
        }

        return state;
    }

    /**
     * Gets get state name.
     *
     * @return the get state name
     */
    public String getStateName() {
        return stateName;
    }

    /**
     * Sets set state name.
     *
     * @param stateName the state name
     */
    public void setStateName(String stateName) {
        this.stateName = stateName;
    }

    /**
     * Gets get state machine name.
     *
     * @return the get state machine name
     */
    public String getStateMachineName() {
        return stateMachineName;
    }

    /**
     * Sets set state machine name.
     *
     * @param stateMachineName the state machine name
     */
    public void setStateMachineName(String stateMachineName) {
        this.stateMachineName = stateMachineName;
    }

    /**
     * Is end boolean.
     *
     * @return the boolean
     */
    public boolean isEnd() {
        return end;
    }

    /**
     * Sets set end.
     *
     * @param end the end
     */
    public void setEnd(boolean end) {
        this.end = end;
    }

    /**
     * Gets get temporary state.
     *
     * @return the get temporary state
     */
    public State getTemporaryState() {
        return temporaryState;
    }

    /**
     * Sets set temporary state.
     *
     * @param temporaryState the temporary state
     */
    public void setTemporaryState(State temporaryState) {
        this.temporaryState = temporaryState;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }
}