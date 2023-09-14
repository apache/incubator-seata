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

package io.seata.saga.engine.pcext.utils;

import io.seata.common.exception.FrameworkErrorCode;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.StringUtils;
import io.seata.saga.engine.exception.EngineExecutionException;
import io.seata.saga.engine.exception.ForwardInvalidException;
import io.seata.saga.engine.pcext.StateInstruction;
import io.seata.saga.proctrl.ProcessContext;
import io.seata.saga.proctrl.impl.SideEffectFreeProcessContextImpl;
import io.seata.saga.statelang.domain.DomainConstants;
import io.seata.saga.statelang.domain.ExecutionStatus;
import io.seata.saga.statelang.domain.ForkState;
import io.seata.saga.statelang.domain.State;
import io.seata.saga.statelang.domain.StateInstance;
import io.seata.saga.statelang.domain.StateMachineInstance;
import io.seata.saga.statelang.domain.impl.LoopStartStateImpl;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

/**
 * Parallel task util
 *
 * @author ptyin
 */
public class ParallelTaskUtils {

    /**
     * Forked context based on parameter
     *
     * @param parentContext parent context
     * @return child context
     */
    public static ProcessContext forkProcessContext(ProcessContext parentContext, State branchState) {
        SideEffectFreeProcessContextImpl childContext = new SideEffectFreeProcessContextImpl();
        childContext.setParent(parentContext);

        StateInstruction parentInstruction = parentContext.getInstruction(StateInstruction.class);
        StateInstruction copiedInstruction = copyInstruction(parentInstruction);
        copiedInstruction.setStateName(branchState.getName());
        if (LoopTaskUtils.getLoopConfig(parentContext, branchState) != null) {
            copiedInstruction.setTemporaryState(new LoopStartStateImpl());
        }
        childContext.setInstruction(copiedInstruction);

        return childContext;
    }

    public static StateInstruction copyInstruction(StateInstruction instruction) {
        StateInstruction copiedInstruction = new StateInstruction();
        copiedInstruction.setStateName(instruction.getStateName());
        copiedInstruction.setStateMachineName(instruction.getStateMachineName());
        copiedInstruction.setEnd(instruction.isEnd());
        copiedInstruction.setTemporaryState(instruction.getTemporaryState());
        copiedInstruction.setTenantId(instruction.getTenantId());
        return copiedInstruction;
    }

    public static void endBranch(ProcessContext context) {
        if (context.hasVariable(DomainConstants.PARALLEL_SEMAPHORE)) {
            Semaphore semaphore = (Semaphore) context.getVariable(DomainConstants.PARALLEL_SEMAPHORE);
            semaphore.release();
        }

        if (context.hasVariable(DomainConstants.PARALLEL_LATCH)) {
            CountDownLatch latch = (CountDownLatch) context.getVariable(DomainConstants.PARALLEL_LATCH);
            latch.countDown();
        }
    }

    /**
     * Forward branch context to last executed state in branch.
     *
     * @param branchContext Branch context
     * @param forkState     Fork state
     * @return The state to forward
     */
    public static State forwardBranch(ProcessContext branchContext, ForkState forkState) {
        StateInstruction instruction = branchContext.getInstruction(StateInstruction.class);
        StateMachineInstance stateMachineInstance = (StateMachineInstance) branchContext.getVariable(
                DomainConstants.VAR_NAME_STATEMACHINE_INST);

        return forwardBranch(instruction.getState(branchContext), stateMachineInstance, forkState);
    }


    /**
     * Forward branch context to last executed state in branch.
     *
     * @param branchInitialState   Branch initial state
     * @param stateMachineInstance Statemachine instance
     * @param forkState            Fork state
     * @return The state to forward
     */
    public static State forwardBranch(State branchInitialState, StateMachineInstance stateMachineInstance,
                                      ForkState forkState) {
        Set<String> branchStates = forkState.getAllBranchStates().get(branchInitialState.getName());

        List<StateInstance> stateInstanceList = stateMachineInstance.getStateList();
        if (CollectionUtils.isEmpty(stateInstanceList)) {
            return branchInitialState;
        }

        // Forward to the last state
        StateInstance lastForwardStateInstance = null;

        for (StateInstance stateInstance : stateInstanceList) {
            // If it is not in the branch or has been compensated, then continue
            if (!branchStates.contains(EngineUtils.getOriginStateName(stateInstance))
                    || ExecutionStatus.SU.equals(stateInstance.getCompensationStatus())) {
                continue;
            } else if (stateInstance.isForCompensation()) {
                throw new ForwardInvalidException(String.format("Compensation state instance exists in branch [%s] " +
                                "execution, Operation [forward] denied, stateInstanceId: %s",
                        branchInitialState.getName(), stateInstance.getId()), FrameworkErrorCode.OperationDenied);
            } else if (ExecutionStatus.UN.equals(stateInstance.getCompensationStatus())) {
                throw new ForwardInvalidException(
                        String.format("Last forward execution state instance compensation status is [UN], " +
                                "Operation [forward] denied, stateInstanceId: %s", stateInstance.getId()),
                        FrameworkErrorCode.OperationDenied);
            }

            lastForwardStateInstance = stateInstance;
        }

        State toForwardState = branchInitialState;
        if (lastForwardStateInstance != null) {
            String lastForwardStateName = EngineUtils.getOriginStateName(lastForwardStateInstance);
            State lastForwardState = stateMachineInstance.getStateMachine().getState(lastForwardStateName);

            // If last forward state successfully executed, then forward to the next
            if (ExecutionStatus.SU.equals(lastForwardStateInstance.getStatus())) {
                String nextOfForward = lastForwardState.getNext();

                if (StringUtils.isBlank(nextOfForward)) {
                    throw new ForwardInvalidException(String.format("Last forward state instance [%s] in branch [%s] " +
                                    "succeeded, and it has no next, should refer to join state [%s]",
                            lastForwardStateInstance.getId(), branchInitialState.getName(),
                            forkState.getPairedJoinState()));
                }

                State nextStateOfForward = stateMachineInstance.getStateMachine().getState(nextOfForward);
                if (nextStateOfForward == null) {
                    throw new EngineExecutionException(String.format("Cannot find next [%s] of last forward state [%s]",
                            nextOfForward, lastForwardStateName), FrameworkErrorCode.ObjectNotExists);
                }
                toForwardState = nextStateOfForward;
            } else {
                toForwardState = lastForwardState;
            }
        }

        return toForwardState;
    }
}
