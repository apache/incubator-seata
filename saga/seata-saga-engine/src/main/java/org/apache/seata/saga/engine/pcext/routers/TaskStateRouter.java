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
package org.apache.seata.saga.engine.pcext.routers;

import java.util.Stack;

import org.apache.seata.common.exception.FrameworkErrorCode;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.saga.engine.exception.EngineExecutionException;
import org.apache.seata.saga.engine.pcext.StateInstruction;
import org.apache.seata.saga.engine.pcext.StateRouter;
import org.apache.seata.saga.engine.pcext.utils.CompensationHolder;
import org.apache.seata.saga.engine.pcext.utils.EngineUtils;
import org.apache.seata.saga.engine.pcext.utils.LoopTaskUtils;
import org.apache.seata.saga.proctrl.HierarchicalProcessContext;
import org.apache.seata.saga.proctrl.Instruction;
import org.apache.seata.saga.proctrl.ProcessContext;
import org.apache.seata.saga.statelang.domain.CompensateSubStateMachineState;
import org.apache.seata.saga.statelang.domain.DomainConstants;
import org.apache.seata.saga.statelang.domain.ExecutionStatus;
import org.apache.seata.saga.statelang.domain.State;
import org.apache.seata.saga.statelang.domain.StateInstance;
import org.apache.seata.saga.statelang.domain.StateMachine;
import org.apache.seata.saga.statelang.domain.SubStateMachine;
import org.apache.seata.saga.statelang.domain.impl.AbstractTaskState;
import org.apache.seata.saga.statelang.domain.impl.LoopStartStateImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TaskState Router
 *
 */
public class TaskStateRouter implements StateRouter {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskStateRouter.class);

    @Override
    public Instruction route(ProcessContext context, State state) throws EngineExecutionException {

        StateInstruction stateInstruction = context.getInstruction(StateInstruction.class);
        if (stateInstruction.isEnd()) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(
                    "StateInstruction is ended, Stop the StateMachine executing. StateMachine[{}] Current State[{}]",
                    stateInstruction.getStateMachineName(), state.getName());
            }
            return null;
        }

        // check if in loop async condition
        if (Boolean.TRUE.equals(context.getVariable(DomainConstants.VAR_NAME_IS_LOOP_STATE))) {
            return null;
        }

        //The current CompensationTriggerState can mark the compensation process is started and perform compensation
        // route processing.
        State compensationTriggerState = (State)context.getVariable(
            DomainConstants.VAR_NAME_CURRENT_COMPEN_TRIGGER_STATE);
        if (compensationTriggerState != null) {
            return compensateRoute(context, compensationTriggerState);
        }

        //There is an exception route, indicating that an exception is thrown, and the exception route is prioritized.
        String next = (String)context.getVariable(DomainConstants.VAR_NAME_CURRENT_EXCEPTION_ROUTE);

        if (StringUtils.hasLength(next)) {
            context.removeVariable(DomainConstants.VAR_NAME_CURRENT_EXCEPTION_ROUTE);
        } else {
            next = state.getNext();
        }

        //If next is empty, the state selected by the Choice state was taken.
        if (!StringUtils.hasLength(next) && context.hasVariable(DomainConstants.VAR_NAME_CURRENT_CHOICE)) {
            next = (String)context.getVariable(DomainConstants.VAR_NAME_CURRENT_CHOICE);
            context.removeVariable(DomainConstants.VAR_NAME_CURRENT_CHOICE);
        }

        if (!StringUtils.hasLength(next)) {
            return null;
        }

        StateMachine stateMachine = state.getStateMachine();

        State nextState = stateMachine.getState(next);
        if (nextState == null) {
            throw new EngineExecutionException("Next state[" + next + "] is not exits",
                FrameworkErrorCode.ObjectNotExists);
        }

        stateInstruction.setStateName(next);

        if (null != LoopTaskUtils.getLoopConfig(context, nextState)) {
            stateInstruction.setTemporaryState(new LoopStartStateImpl());
        }

        return stateInstruction;
    }

    private Instruction compensateRoute(ProcessContext context, State compensationTriggerState) {

        //If there is already a compensation state that has been executed,
        // it is judged whether it is wrong or unsuccessful,
        // and the compensation process is interrupted.
        if (Boolean.TRUE.equals(context.getVariable(DomainConstants.VAR_NAME_FIRST_COMPENSATION_STATE_STARTED))) {

            Exception exception = (Exception)context.getVariable(DomainConstants.VAR_NAME_CURRENT_EXCEPTION);
            if (exception != null) {
                EngineUtils.endStateMachine(context);
                return null;
            }

            StateInstance stateInstance = (StateInstance)context.getVariable(DomainConstants.VAR_NAME_STATE_INST);
            if (stateInstance != null && (!ExecutionStatus.SU.equals(stateInstance.getStatus()))) {
                EngineUtils.endStateMachine(context);
                return null;
            }
        }

        Stack<StateInstance> stateStackToBeCompensated = CompensationHolder.getCurrent(context, true)
            .getStateStackNeedCompensation();
        if (!stateStackToBeCompensated.isEmpty()) {

            StateInstance stateToBeCompensated = stateStackToBeCompensated.pop();

            StateMachine stateMachine = (StateMachine)context.getVariable(DomainConstants.VAR_NAME_STATEMACHINE);
            State state = stateMachine.getState(EngineUtils.getOriginStateName(stateToBeCompensated));
            if (state instanceof AbstractTaskState) {

                AbstractTaskState taskState = (AbstractTaskState)state;

                StateInstruction instruction = context.getInstruction(StateInstruction.class);

                State compensateState = null;
                String compensateStateName = taskState.getCompensateState();
                if (StringUtils.hasLength(compensateStateName)) {
                    compensateState = stateMachine.getState(compensateStateName);
                }

                if (compensateState == null && (taskState instanceof SubStateMachine)) {
                    compensateState = ((SubStateMachine)taskState).getCompensateStateObject();
                    instruction.setTemporaryState(compensateState);
                }

                if (compensateState == null) {
                    EngineUtils.endStateMachine(context);
                    return null;
                }

                instruction.setStateName(compensateState.getName());

                CompensationHolder.getCurrent(context, true).addToBeCompensatedState(compensateState.getName(),
                    stateToBeCompensated);

                ((HierarchicalProcessContext)context).setVariableLocally(
                    DomainConstants.VAR_NAME_FIRST_COMPENSATION_STATE_STARTED, true);

                if (compensateState instanceof CompensateSubStateMachineState) {
                    ((HierarchicalProcessContext)context).setVariableLocally(
                        compensateState.getName() + DomainConstants.VAR_NAME_SUB_MACHINE_PARENT_ID,
                        EngineUtils.generateParentId(stateToBeCompensated));
                }

                return instruction;
            }
        }

        context.removeVariable(DomainConstants.VAR_NAME_CURRENT_COMPEN_TRIGGER_STATE);

        String compensationTriggerStateNext = compensationTriggerState.getNext();
        if (StringUtils.isEmpty(compensationTriggerStateNext)) {
            EngineUtils.endStateMachine(context);
            return null;
        }

        StateInstruction instruction = context.getInstruction(StateInstruction.class);
        instruction.setStateName(compensationTriggerStateNext);
        return instruction;
    }

}
