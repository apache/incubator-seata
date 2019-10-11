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
package io.seata.saga.engine.pcext.handlers;


import io.seata.saga.engine.StateMachineConfig;
import io.seata.saga.engine.exception.EngineExecutionException;
import io.seata.saga.engine.pcext.StateHandler;
import io.seata.saga.engine.pcext.StateInstruction;
import io.seata.saga.engine.pcext.utils.CompensationHolder;
import io.seata.saga.engine.pcext.utils.EngineUtils;
import io.seata.saga.proctrl.ProcessContext;
import io.seata.saga.statelang.domain.DomainConstants;
import io.seata.saga.statelang.domain.ExecutionStatus;
import io.seata.saga.statelang.domain.StateInstance;
import io.seata.saga.statelang.domain.StateMachineInstance;
import java.util.List;
import java.util.Stack;

/**
 * CompensationTriggerState Handler
 * Start to execute compensation
 *
 * @author lorne.cl
 */
public class CompensationTriggerStateHandler implements StateHandler {

    @Override
    public void process(ProcessContext context) throws EngineExecutionException {

        StateInstruction instruction = context.getInstruction(StateInstruction.class);

        StateMachineInstance stateMachineInstance = (StateMachineInstance) context.getVariable(DomainConstants.VAR_NAME_STATEMACHINE_INST);
        StateMachineConfig stateMachineConfig = (StateMachineConfig) context.getVariable(DomainConstants.VAR_NAME_STATEMACHINE_CONFIG);
        List<StateInstance> stateInstanceList = null;
        if (stateMachineInstance != null) {
            stateInstanceList = stateMachineInstance.getStateList();
        }
        else if (stateMachineConfig.getStateLogStore() != null) {
            stateInstanceList = stateMachineConfig.getStateLogStore().queryStateInstanceListByMachineInstanceId(stateMachineInstance.getId());
        }

        List<StateInstance> stateListToBeCompensated = CompensationHolder.findStateInstListToBeCompensated(context, stateInstanceList);
        if(stateListToBeCompensated != null && stateListToBeCompensated.size() > 0){

            //Clear exceptions that occur during forward execution
            context.removeVariable(DomainConstants.VAR_NAME_CURRENT_EXCEPTION);

            Stack<StateInstance> stateStackToBeCompensated = CompensationHolder.getCurrent(context, true).getStateStackNeedCompensation();
            stateStackToBeCompensated.addAll(stateListToBeCompensated);


            //If the forward running state is empty or running,
            // it indicates that the compensation state is automatically initiated in the state machine,
            // and the forward state needs to be changed to the UN state.
            //If the forward status is not the two states, then the compensation operation should be initiated by server recovery,
            // and the forward state should not be modified.
            if(stateMachineInstance.getStatus() == null || ExecutionStatus.RU.equals(stateMachineInstance.getStatus())) {
                stateMachineInstance.setStatus(ExecutionStatus.UN);
            }
            //Record the status of the state machine as "compensating", and the subsequent routing logic will route to the compensation state
            stateMachineInstance.setCompensationStatus(ExecutionStatus.RU);
            context.setVariable(DomainConstants.VAR_NAME_CURRENT_COMPEN_TRIGGER_STATE, instruction.getState(context));
        }
        else{
            EngineUtils.endStateMachine(context);
        }
    }
}