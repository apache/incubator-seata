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
import io.seata.saga.engine.pcext.utils.ParallelTaskUtils;
import io.seata.saga.proctrl.ProcessContext;
import io.seata.saga.proctrl.impl.ProcessContextImpl;
import io.seata.saga.statelang.domain.DomainConstants;
import io.seata.saga.statelang.domain.ParallelState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parallel State Handler
 * Start execute parallel state task
 *
 * @author anselleeyy
 */
public class ParallelStateHandler implements StateHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParallelStateHandler.class);

    @Override
    public void process(ProcessContext context) throws EngineExecutionException {

        StateInstruction instruction = context.getInstruction(StateInstruction.class);
        StateMachineConfig stateMachineConfig = (StateMachineConfig) context.getVariable(
            DomainConstants.VAR_NAME_STATEMACHINE_CONFIG);

        ParallelState state = (ParallelState) instruction.getState(context);
        if (!stateMachineConfig.isEnableAsync() || null == stateMachineConfig.getAsyncProcessCtrlEventPublisher()) {
            throw new EngineExecutionException(
                "Asynchronous start is disabled. Parallel execution will run asynchronous, please set "
                    + "StateMachineConfig.enableAsync=true and async thread-pool correctly first.", FrameworkErrorCode.AsynchronousStartDisabled);
        }

        if (CollectionUtils.isEmpty(state.getBranches())) {
            throw new EngineExecutionException(
                "State [" + state.getName() + "] parallel branch should have at least one", FrameworkErrorCode.ParameterRequired);
        }

        List<String> branches = state.getBranches();
        List<ProcessContext> parallelProcessContexts = new ArrayList<>();
        // decide max concurrent threads
        int maxInstances = Math.min(state.getParallel(), branches.size());

        Semaphore semaphore = new Semaphore(maxInstances);
        context.setVariable(DomainConstants.PARALLEL_SEMAPHORE, semaphore);
        context.setVariable(DomainConstants.VAR_NAME_IS_PARALLEL_STATE, true);
        context.setVariable(DomainConstants.PARALLEL_PARENT_STATE_NAME, state.getName());
        LOGGER.info("start to publish parallel job");

        // async publish branches
        for (int i = 0; i < branches.size(); i++) {
            try {
                semaphore.acquire();
                ProcessContextImpl tempContext = (ProcessContextImpl) ParallelTaskUtils.createTempContext(context, branches.get(i), i);
                stateMachineConfig.getAsyncProcessCtrlEventPublisher().publish(tempContext);
                parallelProcessContexts.add(tempContext);
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
                    LOGGER.debug("wait {}ms for parallel state [{}] finish", 1000, instruction.getStateName());
                }
                isFinished = semaphore.tryAcquire(maxInstances, 1000, TimeUnit.MILLISECONDS);
            }
        } catch (InterruptedException e) {
            LOGGER.error("State: [{}] wait parallel execution complete is interrupted, message: [{}]",
                instruction.getStateName(), e.getMessage());
            throw new EngineExecutionException(e);
        } finally {
            context.removeVariable(DomainConstants.PARALLEL_SEMAPHORE);
            context.removeVariable(DomainConstants.VAR_NAME_IS_PARALLEL_STATE);
            context.removeVariable(DomainConstants.PARALLEL_PARENT_STATE_NAME);
        }

        for (ProcessContext processContext : parallelProcessContexts) {
            if (processContext.hasVariable(DomainConstants.VAR_NAME_CURRENT_EXCEPTION)) {
                Exception exception = (Exception)processContext.getVariable(DomainConstants.VAR_NAME_CURRENT_EXCEPTION);
                EngineUtils.failStateMachine(context, exception);
                break;
            }
        }
    }

}
