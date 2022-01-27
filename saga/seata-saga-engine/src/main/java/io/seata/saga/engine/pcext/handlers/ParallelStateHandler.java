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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import io.seata.common.exception.FrameworkErrorCode;
import io.seata.common.util.CollectionUtils;
import io.seata.saga.engine.StateMachineConfig;
import io.seata.saga.engine.exception.EngineExecutionException;
import io.seata.saga.engine.pcext.StateHandler;
import io.seata.saga.engine.pcext.StateInstruction;
import io.seata.saga.engine.pcext.utils.EngineUtils;
import io.seata.saga.engine.pcext.utils.ParallelContextHolder;
import io.seata.saga.engine.pcext.utils.ParallelTaskUtils;
import io.seata.saga.proctrl.ProcessContext;
import io.seata.saga.statelang.domain.DomainConstants;
import io.seata.saga.statelang.domain.ExecutionStatus;
import io.seata.saga.statelang.domain.ParallelState;
import io.seata.saga.statelang.domain.StateInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parallel State Handler
 * Start execute parallel state task
 *
 * @author anselleeyy
 */
public class ParallelStateHandler implements StateHandler {

    private static final Logger LOGGER        = LoggerFactory.getLogger(ParallelStateHandler.class);
    private static final int    AWAIT_TIMEOUT = 1000;

    @Override
    public void process(ProcessContext context) throws EngineExecutionException {

        StateInstruction instruction = context.getInstruction(StateInstruction.class);
        StateMachineConfig stateMachineConfig =
            (StateMachineConfig) context.getVariable(DomainConstants.VAR_NAME_STATEMACHINE_CONFIG);

        if (!stateMachineConfig.isEnableAsync() || null == stateMachineConfig.getAsyncProcessCtrlEventPublisher()) {
            throw new EngineExecutionException(
                "Asynchronous start is disabled. Parallel execution will run asynchronous, please set "
                    + "StateMachineConfig.enableAsync=true and async thread-pool correctly first.", FrameworkErrorCode.AsynchronousStartDisabled);
        }

        ParallelState state = (ParallelState) instruction.getState(context);
        List<String> branches = state.getBranches();
        if (CollectionUtils.isEmpty(branches)) {
            throw new EngineExecutionException(
                "State [" + state.getName() + "] parallel branch should have at least one", FrameworkErrorCode.ParameterRequired);
        }
        // init parallel context
        ParallelTaskUtils.initParallelContext(context, state);
        ParallelContextHolder contextHolder = ParallelContextHolder.getCurrent(context, false);

        // decide max concurrent threads
        int totalInstances = branches.size();
        List<String> unExecutedBranches = new ArrayList<>();

        if (DomainConstants.OPERATION_NAME_FORWARD.equals(context.getVariable(DomainConstants.VAR_NAME_OPERATION_NAME))) {
            // forward scene should reload Retry-Committing parallel state
            ParallelTaskUtils.reloadParallelContext(context, state.getName());
            unExecutedBranches = ParallelTaskUtils.acquireUnExecutedBranches(context);
            totalInstances = unExecutedBranches.size() + contextHolder.getForwardBranches().size();
        }
        int maxInstances = Math.min(state.getParallel(), totalInstances);

        Semaphore semaphore = new Semaphore(maxInstances);
        context.setVariable(DomainConstants.PARALLEL_SEMAPHORE, semaphore);

        // async publish branches
        List<ProcessContext> forkProcessContexts = new ArrayList<>();
        for (int i = 0; i < totalInstances; i++) {

            // each index means one parallel line
            try {
                semaphore.acquire();
                if (contextHolder.isFailEnd()) {
                    semaphore.release();
                    break;
                }

                ProcessContext forkContext;
                // forward scene
                if (DomainConstants.OPERATION_NAME_FORWARD.equals(context.getVariable(DomainConstants.VAR_NAME_OPERATION_NAME))) {
                    int forwardIndex;
                    if (contextHolder.getForwardBranches().size() > 0) {
                        // retry-committing branches should be published again first
                        forwardIndex = Integer.parseInt(contextHolder.getForwardBranches().remove(0));
                    } else {
                        forwardIndex = Integer.parseInt(unExecutedBranches.remove(0));
                    }
                    forkContext = ParallelTaskUtils.createTempContext(context, branches.get(forwardIndex), forwardIndex);
                } else {
                    forkContext = ParallelTaskUtils.createTempContext(context, branches.get(i), i);
                }

                stateMachineConfig.getAsyncProcessCtrlEventPublisher().publish(forkContext);
                forkProcessContexts.add(forkContext);
            } catch (InterruptedException e) {
                LOGGER.error("try execute parallel task for State: [{}] is interrupted, branch: [{}], message: [{}]",
                    instruction.getStateName(), branches.get(i), e.getMessage());
                throw new EngineExecutionException(e);
            }
        }

        try {
            boolean isFinished = false;
            while (!isFinished) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("wait {}ms for parallel state [{}] finish", AWAIT_TIMEOUT, instruction.getStateName());
                }
                isFinished = semaphore.tryAcquire(maxInstances, AWAIT_TIMEOUT, TimeUnit.MILLISECONDS);
            }
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Parallel State [{}] has finished [{}] branches with status [{}]", state.getName(),
                    forkProcessContexts.size(), decideExecutionStatus(forkProcessContexts));
            }
            ParallelTaskUtils.putContextToParent(context, forkProcessContexts, state);
        } catch (InterruptedException e) {
            LOGGER.error("State: [{}] wait parallel execution complete is interrupted, message: [{}]",
                instruction.getStateName(), e.getMessage());
            throw new EngineExecutionException(e);
        } finally {
            ParallelTaskUtils.clearParallelContext(context);
        }

        if (contextHolder.isFailEnd()) {
            context.setVariable(DomainConstants.VAR_NAME_ASYNC_EXECUTION_INSTANCE, forkProcessContexts);
            EngineUtils.handleExceptionWithMultiInstances(context);
        }
        
    }

    private static String decideExecutionStatus(List<ProcessContext> asyncExecutionInstances) {
        if (CollectionUtils.isNotEmpty(asyncExecutionInstances)) {
            for (ProcessContext processContext : asyncExecutionInstances) {
                StateInstance stateInstance = (StateInstance) processContext.getVariable(DomainConstants.VAR_NAME_STATE_INST);
                if (stateInstance != null && stateInstance.getStatus() != ExecutionStatus.SU) {
                    return stateInstance.getStatus().toString();
                }
            }
        }
        return ExecutionStatus.SU.toString();
    }

}
