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
import io.seata.common.util.StringUtils;
import io.seata.saga.engine.StateMachineConfig;
import io.seata.saga.engine.exception.EngineExecutionException;
import io.seata.saga.engine.pcext.StateHandler;
import io.seata.saga.engine.pcext.StateInstruction;
import io.seata.saga.engine.pcext.utils.EngineUtils;
import io.seata.saga.engine.pcext.utils.ParallelContextHolder;
import io.seata.saga.engine.pcext.utils.ParallelTaskUtils;
import io.seata.saga.proctrl.HierarchicalProcessContext;
import io.seata.saga.proctrl.ProcessContext;
import io.seata.saga.proctrl.impl.ProcessContextImpl;
import io.seata.saga.statelang.domain.DomainConstants;
import io.seata.saga.statelang.domain.ParallelState;
import io.seata.saga.statelang.domain.StateMachineInstance;
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
        StateMachineInstance stateMachineInstance =
            (StateMachineInstance) context.getVariable(DomainConstants.VAR_NAME_STATEMACHINE_INST);
        StateMachineConfig stateMachineConfig =
            (StateMachineConfig) context.getVariable(DomainConstants.VAR_NAME_STATEMACHINE_CONFIG);

        ParallelState state = (ParallelState) instruction.getState(context);
        if (!stateMachineConfig.isEnableAsync() || null == stateMachineConfig.getAsyncProcessCtrlEventPublisher()) {
            throw new EngineExecutionException(
                "Asynchronous start is disabled. Parallel execution will run asynchronous, please set "
                    + "StateMachineConfig.enableAsync=true and async thread-pool correctly first.", FrameworkErrorCode.AsynchronousStartDisabled);
        }

        List<String> branches = state.getBranches();
        if (CollectionUtils.isEmpty(branches)) {
            throw new EngineExecutionException(
                "State [" + state.getName() + "] parallel branch should have at least one", FrameworkErrorCode.ParameterRequired);
        }
        // init parallel context
        ParallelTaskUtils.initParallelContext(context, state);
        ParallelContextHolder contextHolder = ParallelContextHolder.getCurrent(context, true);

        // decide max concurrent threads
        int totalInstances = branches.size();
        int maxInstances;
        List<String> unExecutedBranches = new ArrayList<>();

        if (DomainConstants.OPERATION_NAME_FORWARD.equals(context.getVariable(DomainConstants.VAR_NAME_OPERATION_NAME))) {
            ParallelTaskUtils.reloadParallelContext(context, state.getName());
            unExecutedBranches = ParallelTaskUtils.acquireUnExecutedBranches(context);
            totalInstances = unExecutedBranches.size() + ParallelContextHolder.getCurrent(context, true).getForwardBranches().size();
        }
        maxInstances = Math.min(state.getParallel(), totalInstances);

        Semaphore semaphore = new Semaphore(maxInstances);
        context.setVariable(DomainConstants.PARALLEL_SEMAPHORE, semaphore);

        // async publish branches
        List<ProcessContext> parallelProcessContexts = new ArrayList<>();
        for (int i = 0; i < totalInstances; i++) {

            // each index means one parallel line, if single succeed, then skip.
            try {
                semaphore.acquire();
                ProcessContextImpl tempContext;

                if (contextHolder.isFailEnd()) {
                    semaphore.release();
                    break;
                }

                // forward scene
                if (DomainConstants.OPERATION_NAME_FORWARD.equals(context.getVariable(DomainConstants.VAR_NAME_OPERATION_NAME))) {
                    int forwardIndex;
                    if (contextHolder.getForwardBranches().size() > 0) {
                        // fail-end, publish again first
                        forwardIndex = Integer.parseInt(contextHolder.getForwardBranches().remove(0));
                    } else {
                        forwardIndex = Integer.parseInt(unExecutedBranches.remove(0));
                    }
                    tempContext =
                        (ProcessContextImpl) ParallelTaskUtils.createTempContext(context, branches.get(forwardIndex), forwardIndex);
                } else {
                    tempContext = (ProcessContextImpl) ParallelTaskUtils.createTempContext(context, branches.get(i), i);
                }

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
            ParallelTaskUtils.clearParallelContext(context);
        }

        if (contextHolder.isFailEnd()) {
            String nextRoute =
                ParallelTaskUtils.decideCurrentExceptionRoute(parallelProcessContexts, stateMachineInstance.getStateMachine());

            if (StringUtils.isNotBlank(nextRoute)) {
                ((HierarchicalProcessContext) context).setVariableLocally(DomainConstants.VAR_NAME_CURRENT_EXCEPTION_ROUTE, nextRoute);
            } else {
                for (ProcessContext processContext : parallelProcessContexts) {
                    if (processContext.hasVariable(DomainConstants.VAR_NAME_CURRENT_EXCEPTION)) {
                        Exception exception =
                            (Exception) processContext.getVariable(DomainConstants.VAR_NAME_CURRENT_EXCEPTION);
                        EngineUtils.failStateMachine(context, exception);
                        break;
                    }
                }
            }
        }
        
    }

}
