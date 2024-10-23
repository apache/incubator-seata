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
package org.apache.seata.saga.engine.pcext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.seata.common.exception.FrameworkException;
import org.apache.seata.common.util.CollectionUtils;
import org.apache.seata.saga.engine.pcext.handlers.ChoiceStateHandler;
import org.apache.seata.saga.engine.pcext.handlers.CompensationTriggerStateHandler;
import org.apache.seata.saga.engine.pcext.handlers.FailEndStateHandler;
import org.apache.seata.saga.engine.pcext.handlers.LoopStartStateHandler;
import org.apache.seata.saga.engine.pcext.handlers.ScriptTaskStateHandler;
import org.apache.seata.saga.engine.pcext.handlers.ServiceTaskStateHandler;
import org.apache.seata.saga.engine.pcext.handlers.SubStateMachineHandler;
import org.apache.seata.saga.engine.pcext.handlers.SucceedEndStateHandler;
import org.apache.seata.saga.proctrl.ProcessContext;
import org.apache.seata.saga.proctrl.handler.ProcessHandler;
import org.apache.seata.saga.statelang.domain.StateType;
import org.apache.seata.saga.statelang.domain.State;

/**
 * StateMachine ProcessHandler
 *
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
        if (!stateHandlers.isEmpty()) {
            return;
        }

        stateHandlers.put(StateType.SERVICE_TASK.getValue(), new ServiceTaskStateHandler());
        stateHandlers.put(StateType.SCRIPT_TASK.getValue(), new ScriptTaskStateHandler());
        stateHandlers.put(StateType.SUB_MACHINE_COMPENSATION.getValue(), new ServiceTaskStateHandler());
        stateHandlers.put(StateType.SUB_STATE_MACHINE.getValue(), new SubStateMachineHandler());
        stateHandlers.put(StateType.CHOICE.getValue(), new ChoiceStateHandler());
        stateHandlers.put(StateType.SUCCEED.getValue(), new SucceedEndStateHandler());
        stateHandlers.put(StateType.FAIL.getValue(), new FailEndStateHandler());
        stateHandlers.put(StateType.COMPENSATION_TRIGGER.getValue(), new CompensationTriggerStateHandler());
        stateHandlers.put(StateType.LOOP_START.getValue(), new LoopStartStateHandler());

    }

    public Map<String, StateHandler> getStateHandlers() {
        return stateHandlers;
    }

    public void setStateHandlers(Map<String, StateHandler> stateHandlers) {
        this.stateHandlers.putAll(stateHandlers);
    }
}
