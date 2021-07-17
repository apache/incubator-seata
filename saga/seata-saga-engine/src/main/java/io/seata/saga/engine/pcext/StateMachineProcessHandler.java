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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.seata.common.exception.FrameworkException;
import io.seata.common.util.CollectionUtils;
import io.seata.saga.engine.pcext.handlers.ChoiceStateHandler;
import io.seata.saga.engine.pcext.handlers.CompensationTriggerStateHandler;
import io.seata.saga.engine.pcext.handlers.FailEndStateHandler;
import io.seata.saga.engine.pcext.handlers.LoopStartStateHandler;
import io.seata.saga.engine.pcext.handlers.ScriptTaskStateHandler;
import io.seata.saga.engine.pcext.handlers.ServiceTaskStateHandler;
import io.seata.saga.engine.pcext.handlers.SubStateMachineHandler;
import io.seata.saga.engine.pcext.handlers.SucceedEndStateHandler;
import io.seata.saga.proctrl.ProcessContext;
import io.seata.saga.proctrl.handler.ProcessHandler;
import io.seata.saga.statelang.domain.DomainConstants;
import io.seata.saga.statelang.domain.State;

/**
 * StateMachine ProcessHandler
 *
 * @author lorne.cl
 * @see ProcessHandler
 */
public class StateMachineProcessHandler implements ProcessHandler {

    private final Map<String, StateHandler> stateHandlers = new ConcurrentHashMap<>();

    @Override
    public void process(ProcessContext context) throws FrameworkException {
        StateInstruction instruction = context.getInstruction(StateInstruction.class);
        State state = instruction.getState(context);
        String stateType = state.getType();
        StateHandler stateHandler = stateHandlers.get(stateType);

        List<StateHandlerInterceptor> interceptors = null;
        if (stateHandler instanceof InterceptableStateHandler) {
            interceptors = ((InterceptableStateHandler)stateHandler).getInterceptors();
        }

        List<StateHandlerInterceptor> executedInterceptors = null;
        Exception exception = null;
        try {
            if (CollectionUtils.isNotEmpty(interceptors)) {
                executedInterceptors = new ArrayList<>(interceptors.size());
                for (StateHandlerInterceptor interceptor : interceptors) {
                    executedInterceptors.add(interceptor);
                    interceptor.preProcess(context);
                }
            }

            stateHandler.process(context);

        } catch (Exception e) {
            exception = e;
            throw e;
        } finally {
            if (CollectionUtils.isNotEmpty(executedInterceptors)) {
                for (int i = executedInterceptors.size() - 1; i >= 0; i--) {
                    StateHandlerInterceptor interceptor = executedInterceptors.get(i);
                    interceptor.postProcess(context, exception);
                }
            }
        }
    }

    public void initDefaultHandlers() {
        if (stateHandlers.isEmpty()) {
            stateHandlers.put(DomainConstants.STATE_TYPE_SERVICE_TASK, new ServiceTaskStateHandler());

            stateHandlers.put(DomainConstants.STATE_TYPE_SCRIPT_TASK, new ScriptTaskStateHandler());

            stateHandlers.put(DomainConstants.STATE_TYPE_SUB_MACHINE_COMPENSATION, new ServiceTaskStateHandler());

            stateHandlers.put(DomainConstants.STATE_TYPE_SUB_STATE_MACHINE, new SubStateMachineHandler());

            stateHandlers.put(DomainConstants.STATE_TYPE_CHOICE, new ChoiceStateHandler());
            stateHandlers.put(DomainConstants.STATE_TYPE_SUCCEED, new SucceedEndStateHandler());
            stateHandlers.put(DomainConstants.STATE_TYPE_FAIL, new FailEndStateHandler());
            stateHandlers.put(DomainConstants.STATE_TYPE_COMPENSATION_TRIGGER, new CompensationTriggerStateHandler());
            stateHandlers.put(DomainConstants.STATE_TYPE_LOOP_START, new LoopStartStateHandler());
        }
    }

    public Map<String, StateHandler> getStateHandlers() {
        return stateHandlers;
    }

    public void setStateHandlers(Map<String, StateHandler> stateHandlers) {
        this.stateHandlers.putAll(stateHandlers);
    }
}