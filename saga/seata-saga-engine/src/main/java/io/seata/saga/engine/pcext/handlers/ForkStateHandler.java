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
import io.seata.saga.engine.StateMachineConfig;
import io.seata.saga.engine.exception.EngineExecutionException;
import io.seata.saga.engine.pcext.StateHandler;
import io.seata.saga.engine.pcext.StateInstruction;
import io.seata.saga.engine.pcext.utils.LoopTaskUtils;
import io.seata.saga.engine.pcext.utils.ParallelContextHolder;
import io.seata.saga.engine.pcext.utils.ParallelTaskUtils;
import io.seata.saga.proctrl.ProcessContext;
import io.seata.saga.proctrl.eventing.impl.ProcessCtrlEventPublisher;
import io.seata.saga.statelang.domain.DomainConstants;
import io.seata.saga.statelang.domain.ForkState;
import io.seata.saga.statelang.domain.State;
import io.seata.saga.statelang.domain.StateMachine;
import io.seata.saga.statelang.domain.impl.LoopStartStateImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * ForkState handler
 *
 * @author ptyin
 */
public class ForkStateHandler implements StateHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ForkStateHandler.class);

    @Override
    public void process(ProcessContext context) throws EngineExecutionException {
        StateInstruction instruction = context.getInstruction(StateInstruction.class);
        ForkState forkState = (ForkState) instruction.getState(context);
        List<String> branches = forkState.getBranches();
        checkBranches(forkState, branches);

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

    protected static void checkBranches(State forkState, List<String> branches) {
        if (CollectionUtils.isEmpty(branches)) {
            throw new EngineExecutionException(
                    "State [" + forkState.getName() + "] parallel branch should have at least one",
                    FrameworkErrorCode.ParameterRequired
            );
        }
        StateMachine stateMachine = forkState.getStateMachine();
        for (String branch: branches) {
            State branchState = stateMachine.getState(branch);
            if (branchState == null) {
                throw new EngineExecutionException(String.format("Fork state [%s] branch [%s] cannot be found",
                        forkState.getName(), branch), FrameworkErrorCode.ObjectNotExists);
            }
        }
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
        if (context.hasVariable(DomainConstants.VAR_NAME_IS_IN_PARALLEL_BRANCH)) {
            // If this fork is inside a parallel branch, it does not block.
            publishBranchesNonBlocking(publisher, context, state);
        } else {
            // Otherwise it keeps blocked until all branches join.
            publishBranchesBlocking(publisher, context, state);
        }
    }

    /**
     * Publish branch context to event bus in a non-blocking manner.
     * Use this method under the circumstance that the current fork
     * state is inside a parallel branch published by an outer fork.
     * This method can prevent deadlock in a nesting fork setup.
     *
     * @param publisher Event publisher
     * @param context   Current context
     * @param state     Current fork state
     */
    protected static void publishBranchesNonBlocking(ProcessCtrlEventPublisher publisher,
                                                     ProcessContext context, ForkState state) {
        List<String> branches = state.getBranches();
        int totalBranches = branches.size();
        int maxBranches = state.getParallel() == 0 ? totalBranches : Math.min(totalBranches, state.getParallel());
        StateMachine stateMachine = state.getStateMachine();

        ParallelContextHolder parallelContextHolder = ParallelContextHolder.getInstance(context, state);
        for (int i = 0; i < maxBranches; i++) {
            State branchState = stateMachine.getState(parallelContextHolder.next());
            if (branchState == null) {
                throw new EngineExecutionException(String.format("Fork state [%s] branch [%s] cannot be found",
                        state.getName(), branches.get(i)), FrameworkErrorCode.ObjectNotExists);
            }

            // Fork context
            ProcessContext childContext = ParallelTaskUtils.forkProcessContext(context, branchState);
            childContext.setVariable(DomainConstants.VAR_NAME_CURRENT_PARALLEL_CONTEXT_HOLDER, parallelContextHolder);
            // Publish it to async event bus
            publisher.publish(childContext);
        }
    }

    /**
     * Publish branch context to event bus in a blocking manner.
     * Use this method under the circumstance that the current fork state is inside the caller thread.
     * This method can prevent early termination of caller thread when starting state machine synchronously.
     *
     * @param publisher Event publisher
     * @param context   Current context
     * @param state     Current fork state
     */
    protected static void publishBranchesBlocking(ProcessCtrlEventPublisher publisher,
                                                  ProcessContext context, ForkState state) {
        List<String> branches = state.getBranches();
        int totalBranches = branches.size();
        int maxBranches = state.getParallel() == 0 ? totalBranches : Math.min(totalBranches, state.getParallel());
        // Use latch to wait all parallel branches
        CountDownLatch latch = new CountDownLatch(totalBranches);
        // Use semaphore to control parallelism
        Semaphore semaphore = new Semaphore(maxBranches);

        StateMachine stateMachine = state.getStateMachine();
        boolean isForward = DomainConstants.OPERATION_NAME_FORWARD
                .equals(context.getVariable(DomainConstants.VAR_NAME_OPERATION_NAME));
        for (String branch : branches) {
            State branchState = stateMachine.getState(branch);
            // Fork context
            ProcessContext childContext = ParallelTaskUtils.forkProcessContext(context, branchState);
            childContext.setVariable(DomainConstants.PARALLEL_LATCH, latch);
            childContext.setVariable(DomainConstants.PARALLEL_SEMAPHORE, semaphore);
            childContext.setVariable(DomainConstants.VAR_NAME_IS_IN_PARALLEL_BRANCH, true);
            if (Boolean.TRUE.equals(isForward)) {
                StateInstruction instruction = childContext.getInstruction(StateInstruction.class);
                State toForwardState = ParallelTaskUtils.forwardBranch(childContext, state);
                instruction.setStateName(toForwardState.getName());

                if (LoopTaskUtils.getLoopConfig(childContext, toForwardState) != null) {
                    instruction.setTemporaryState(new LoopStartStateImpl());
                }
            }
            try {
                boolean acquired = semaphore.tryAcquire(state.getTimeout(), TimeUnit.MILLISECONDS);
                if (!acquired) {
                    LOGGER.warn(String.format("Fork state [%s] branch [%s] waiting time out. " +
                            "Parallel execution exceeds parallelism limits.", state.getName(), branch));
                }
                // Publish it to async event bus
                publisher.publish(childContext);
            } catch (InterruptedException e) {
                throw new EngineExecutionException(e, String.format("Waiting execute fork state [%s] is interrupted, " +
                        "branch: [%s], message: [%s]", state.getName(), branch, e.getMessage()));
            }
        }

        try {
            boolean executed = latch.await(state.getTimeout(), TimeUnit.MILLISECONDS);
            if (!executed) {
                throw new EngineExecutionException(String.format("Executing fork state [%s]: execution timeout",
                        state.getName()));
            }
        } catch (InterruptedException e) {
            throw new EngineExecutionException(e, String.format("Waiting join branches for fork state [%s] is " +
                    "interrupted, message: [%s]", state.getName(), e.getMessage()));
        }
    }
}
