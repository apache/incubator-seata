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


import io.seata.common.exception.FrameworkException;
import io.seata.saga.engine.pcext.handlers.ChoiceStateHandler;
import io.seata.saga.engine.pcext.handlers.CompensationTriggerStateHandler;
import io.seata.saga.engine.pcext.handlers.FailEndStateHandler;
import io.seata.saga.engine.pcext.handlers.ServiceTaskStateHandler;
import io.seata.saga.engine.pcext.handlers.SubStateMachineHandler;
import io.seata.saga.engine.pcext.handlers.SucceedEndStateHandler;
import io.seata.saga.engine.pcext.interceptors.ServiceTaskHandlerInterceptor;
import io.seata.saga.proctrl.ProcessContext;
import io.seata.saga.proctrl.handler.ProcessHandler;
import io.seata.saga.statelang.domain.DomainConstants;
import io.seata.saga.statelang.domain.State;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * StateMachine ProcessHandler
 *
 * @see ProcessHandler
 * @author lorne.cl
 */
public class StateMachineProcessHandler implements ProcessHandler {

    private Map<String, StateHandler> stateHandlers = new ConcurrentHashMap<String, StateHandler>();

    @Override
    public void process(ProcessContext context) throws FrameworkException {

        StateInstruction instruction = context.getInstruction(StateInstruction.class);
        State state = instruction.getState(context);
        String stateType = state.getType();
        StateHandler stateHandler = stateHandlers.get(stateType);

        List<StateHandlerInterceptor> interceptors = null;
        if (stateHandler instanceof InterceptibleStateHandler) {
            interceptors = ((InterceptibleStateHandler) stateHandler).getInterceptors();
        }

        List<StateHandlerInterceptor> executedInterceptors = null;
        Exception exception = null;
        try {
            if (interceptors != null && interceptors.size() > 0) {
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

            if (executedInterceptors != null && executedInterceptors.size() > 0) {
                for (int i = executedInterceptors.size() - 1; i >= 0; i--) {
                    StateHandlerInterceptor interceptor = executedInterceptors.get(i);
                    interceptor.postProcess(context, exception);
                }
            }
        }

    }

    public void initDefaultHandlers(){
        if(stateHandlers.size() == 0){

            //ServiceTask
            ServiceTaskStateHandler serviceTaskStateHandler = new ServiceTaskStateHandler();
            List<StateHandlerInterceptor> stateHandlerInterceptors = new ArrayList<>(1);
            stateHandlerInterceptors.add(new ServiceTaskHandlerInterceptor());
            serviceTaskStateHandler.setInterceptors(stateHandlerInterceptors);
            stateHandlers.put(DomainConstants.STATE_TYPE_SERVICE_TASK, serviceTaskStateHandler);

            stateHandlers.put(DomainConstants.STATE_TYPE_SUB_MACHINE_COMPENSATION, serviceTaskStateHandler);

            SubStateMachineHandler subStateMachineHandler = new SubStateMachineHandler();
            subStateMachineHandler.setInterceptors(stateHandlerInterceptors);
            stateHandlers.put(DomainConstants.STATE_TYPE_SUB_STATE_MACHINE, subStateMachineHandler);

            stateHandlers.put(DomainConstants.STATE_TYPE_CHOICE, new ChoiceStateHandler());
            stateHandlers.put(DomainConstants.STATE_TYPE_SUCCEED, new SucceedEndStateHandler());
            stateHandlers.put(DomainConstants.STATE_TYPE_FAIL, new FailEndStateHandler());
            stateHandlers.put(DomainConstants.STATE_TYPE_COMPENSATION_TRIGGER, new CompensationTriggerStateHandler());
        }
    }

    public Map<String, StateHandler> getStateHandlers() {
        return stateHandlers;
    }

    public void setStateHandlers(Map<String, StateHandler> stateHandlers) {
        this.stateHandlers.putAll(stateHandlers);
    }
}