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

import io.seata.common.exception.FrameworkErrorCode;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.StringUtils;
import io.seata.saga.engine.StateMachineConfig;
import io.seata.saga.engine.exception.EngineExecutionException;
import io.seata.saga.engine.exception.ForwardInvalidException;
import io.seata.saga.engine.pcext.StateHandler;
import io.seata.saga.engine.pcext.StateInstruction;
import io.seata.saga.engine.pcext.utils.EngineUtils;
import io.seata.saga.engine.pcext.utils.LoopTaskUtils;
import io.seata.saga.engine.pcext.utils.ParallelTaskUtils;
import io.seata.saga.proctrl.ProcessContext;
import io.seata.saga.proctrl.eventing.impl.ProcessCtrlEventPublisher;
import io.seata.saga.statelang.domain.*;
import io.seata.saga.statelang.domain.impl.ForkStateImpl;
import io.seata.saga.statelang.domain.impl.LoopStartStateImpl;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * ForkState handler
 *
 * @author ptyin
 */
public class ForkStateHandler implements StateHandler {
    @Override
    public void process(ProcessContext context) throws EngineExecutionException {
        StateInstruction instruction = context.getInstruction(StateInstruction.class);
        ForkState forkState = (ForkState) instruction.getState(context);
        List<String> branches = forkState.getBranches();
        if (CollectionUtils.isEmpty(branches)) {
            throw new EngineExecutionException(
                    "State [" + forkState.getName() + "] parallel branch should have at least one",
                    FrameworkErrorCode.ParameterRequired
            );
        }

        StateMachineConfig stateMachineConfig =
                (StateMachineConfig) context.getVariable(DomainConstants.VAR_NAME_STATEMACHINE_CONFIG);
        if (!stateMachineConfig.isEnableAsync() || null == stateMachineConfig.getAsyncProcessCtrlEventPublisher()) {
            throw new EngineExecutionException(
                    "Asynchronous start is disabled. Parallel execution will run asynchronous, please set "
                            + "StateMachineConfig.enableAsync=true and async thread-pool correctly first.",
                    FrameworkErrorCode.AsynchronousStartDisabled
            );
        }

        publishBranches(stateMachineConfig.getAsyncProcessCtrlEventPublisher(), context, forkState);
    }

    /**
     * Publish branch context to event bus.
     *
     * @param publisher Event publisher
     * @param context   Current context
     * @param state     Current fork state
     */
    protected static void publishBranches(ProcessCtrlEventPublisher publisher,
                                                                 ProcessContext context, ForkState state) {
        List<String> branches = state.getBranches();
        int totalBranches = branches.size();
        int maxBranches = Math.min(totalBranches, state.getParallel());
        // Use latch to wait all parallel branches
        CountDownLatch latch = new CountDownLatch(totalBranches);
        // Use semaphore to control parallelism
        Semaphore semaphore = new Semaphore(maxBranches);

        StateMachine stateMachine = state.getStateMachine();
        boolean isForward = DomainConstants.OPERATION_NAME_FORWARD
                .equals(context.getVariable(DomainConstants.VAR_NAME_OPERATION_NAME));
        for (String branch : branches) {
            State branchState = stateMachine.getState(branch);
            if (branchState == null) {
                throw new EngineExecutionException(String.format("Fork state [%s] branch [%s] cannot be found",
                        state.getName(), branch), FrameworkErrorCode.ObjectNotExists);
            }

            // Fork context
            ProcessContext childContext = ParallelTaskUtils.forkProcessContext(context, branchState, latch, semaphore);
            if (Boolean.TRUE.equals(isForward)) {
                forwardBranch(childContext, (ForkStateImpl) state);
            }
            try {
                semaphore.acquire();  // TODO tryAcquire
                // Publish it to async event bus
                publisher.publish(childContext);
            } catch (InterruptedException e) {
                throw new EngineExecutionException(e, String.format("Waiting execute fork state [%s] is interrupted, " +
                        "branch: [%s], message: [%s]", state.getName(), branch, e.getMessage()));
            }
        }

        try {
            boolean executed = latch.await(state.getAwaitTimeout(), TimeUnit.MILLISECONDS);
            if (!executed) {
                throw new EngineExecutionException(String.format("Executing fork state [%s]: execution timeout",
                        state.getName()));  // TODO child await timeout greater than outer
            }
        } catch (InterruptedException e) {
            throw new EngineExecutionException(e, String.format("Waiting join branches for fork state [%s] is " +
                    "interrupted, message: [%s]", state.getName(), e.getMessage()));
        }
    }

    /**
     * Forward branch context to last executed state in branch.
     *
     * @param branchContext Context of child branch
     * @param forkState Fork state
     */
    protected static void forwardBranch(ProcessContext branchContext, ForkStateImpl forkState) {
        StateInstruction instruction = branchContext.getInstruction(StateInstruction.class);
        State branchInitialState = instruction.getState(branchContext);
        Set<String> branchStates = forkState.getAllBranchStates().get(branchInitialState.getName());

        StateMachineInstance stateMachineInstance = (StateMachineInstance)branchContext.getVariable(
                DomainConstants.VAR_NAME_STATEMACHINE_INST);
        List<StateInstance> stateInstanceList = stateMachineInstance.getStateList();
        if (CollectionUtils.isEmpty(stateInstanceList)) {
            return;
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
        if (lastForwardStateInstance != null ) {
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

        instruction.setStateName(toForwardState.getName());
        if (LoopTaskUtils.getLoopConfig(branchContext, toForwardState) != null) {
            instruction.setTemporaryState(new LoopStartStateImpl());
        }
    }
}
