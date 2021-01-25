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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.seata.common.exception.FrameworkErrorCode;
import io.seata.saga.engine.StateMachineConfig;
import io.seata.saga.engine.exception.EngineExecutionException;
import io.seata.saga.engine.pcext.StateHandler;
import io.seata.saga.engine.pcext.StateInstruction;
import io.seata.saga.engine.pcext.utils.CompensationHolder;
import io.seata.saga.engine.pcext.utils.EngineUtils;
import io.seata.saga.engine.pcext.utils.LoopContextHolder;
import io.seata.saga.engine.pcext.utils.LoopTaskUtils;
import io.seata.saga.proctrl.HierarchicalProcessContext;
import io.seata.saga.proctrl.ProcessContext;
import io.seata.saga.proctrl.impl.ProcessContextImpl;
import io.seata.saga.statelang.domain.DomainConstants;
import io.seata.saga.statelang.domain.State;
import io.seata.saga.statelang.domain.StateInstance;
import io.seata.saga.statelang.domain.StateMachineInstance;
import io.seata.saga.statelang.domain.TaskState.Loop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loop State Handler
 * Start Loop Execution
 *
 * @author anselleeyy
 */
public class LoopStartStateHandler implements StateHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoopStartStateHandler.class);
    private static final int AWAIT_TIMEOUT = 1000;

    @Override
    public void process(ProcessContext context) throws EngineExecutionException {

        StateInstruction instruction = context.getInstruction(StateInstruction.class);
        StateMachineInstance stateMachineInstance = (StateMachineInstance)context.getVariable(
            DomainConstants.VAR_NAME_STATEMACHINE_INST);
        StateMachineConfig stateMachineConfig = (StateMachineConfig)context.getVariable(
            DomainConstants.VAR_NAME_STATEMACHINE_CONFIG);

        instruction.setTemporaryState(null);
        State currentState = instruction.getState(context);
        StateInstance stateToBeCompensated = null;

        State compensationTriggerState = (State)((HierarchicalProcessContext)context).getVariableLocally(
            DomainConstants.VAR_NAME_CURRENT_COMPEN_TRIGGER_STATE);
        if (null != compensationTriggerState) {
            CompensationHolder compensationHolder = CompensationHolder.getCurrent(context, true);
            stateToBeCompensated = compensationHolder.getStatesNeedCompensation().get(currentState.getName());
            currentState = stateMachineInstance.getStateMachine().getState(EngineUtils.getOriginStateName(stateToBeCompensated));
        }

        Loop loop = LoopTaskUtils.getLoopConfig(context, currentState);
        LoopContextHolder loopContextHolder = LoopContextHolder.getCurrent(context, true);
        CountDownLatch countDownLatch = null;

        if (null != loop) {

            if (!stateMachineConfig.isEnableAsync() || null == stateMachineConfig.getAsyncProcessCtrlEventPublisher()) {
                throw new EngineExecutionException(
                    "Asynchronous start is disabled. Loop execution will run asynchronous, please set StateMachineConfig.enableAsync=true first.",
                    FrameworkErrorCode.AsynchronousStartDisabled);
            }

            int maxInstances;
            if (DomainConstants.OPERATION_NAME_FORWARD.equals(context.getVariable(DomainConstants.VAR_NAME_OPERATION_NAME))) {
                LoopTaskUtils.reloadLoopContext(context, instruction.getState(context).getName());
                maxInstances = Math.min(loop.getParallel(),
                    loopContextHolder.getCollection().size() - loopContextHolder.getNrOfCompletedInstances().get());
            } else if (null != compensationTriggerState) {
                LoopTaskUtils.createCompensateContext(context, stateToBeCompensated);
                maxInstances = Math.min(loop.getParallel(), loopContextHolder.getNrOfInstances().get());
            } else {
                LoopTaskUtils.createLoopContext(context);
                maxInstances = Math.min(loop.getParallel(), loopContextHolder.getCollection().size());
            }
            countDownLatch = new CountDownLatch(maxInstances);
            context.setVariable(DomainConstants.LOOP_COUNT_DOWN_LATCH, countDownLatch);

            // publish loop tasks
            for (int i = 0; i < maxInstances; i++) {
                ProcessContextImpl tempContext;
                if (null != compensationTriggerState) {
                    StateInstance stateInstance = CompensationHolder.getCurrent(context, true).getStateStackNeedCompensation().pop();
                    tempContext = (ProcessContextImpl)LoopTaskUtils.createCompensateLoopEventContext(context, stateInstance);
                    tempContext.setVariableLocally(DomainConstants.VAR_NAME_CURRENT_LOOP_STATE, stateInstance);
                    loopContextHolder.getNrOfInstances().decrementAndGet();
                } else {
                    tempContext = (ProcessContextImpl)LoopTaskUtils.createLoopEventContext(context);
                    tempContext.setVariableLocally(DomainConstants.VAR_NAME_CURRENT_LOOP_STATE, instruction.getState(context));
                }
                stateMachineConfig.getAsyncProcessCtrlEventPublisher().publish(tempContext);
                loopContextHolder.getNrOfActiveInstances().incrementAndGet();
            }
        } else {
            LOGGER.warn("Loop config of State [{}] is illegal, will execute as normal", instruction.getStateName());
            instruction.setTemporaryState(instruction.getState(context));
        }

        try {
            if (null != countDownLatch) {
                boolean isFinished = false;
                while (!isFinished) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("wait {}ms for loop state [{}] finish", AWAIT_TIMEOUT, instruction.getStateName());
                    }
                    isFinished = countDownLatch.await(AWAIT_TIMEOUT, TimeUnit.MILLISECONDS);
                }
            }
        } catch (InterruptedException exception) {
            exception.printStackTrace();
        }

        if (LoopTaskUtils.needCompensate(context)) {
            LoopContextHolder.clearCurrent(context);
            // route to compensationTriggerState as normally
            if (!Boolean.TRUE.equals(context.getVariable(DomainConstants.VAR_NAME_FIRST_COMPENSATION_STATE_STARTED))) {
                context.setVariable(DomainConstants.VAR_NAME_CURRENT_EXCEPTION_ROUTE, DomainConstants.STATE_TYPE_COMPENSATION_TRIGGER);
            }
        } else if (!loopContextHolder.getLoopExpContext().isEmpty()) {
            Exception exception = loopContextHolder.getLoopExpContext().peek();
            LoopContextHolder.clearCurrent(context);
            EngineUtils.failStateMachine(context, exception);
        } else {
            LoopContextHolder.clearCurrent(context);
        }

    }
}
