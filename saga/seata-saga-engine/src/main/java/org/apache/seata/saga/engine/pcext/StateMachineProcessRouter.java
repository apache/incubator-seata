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
import org.apache.seata.saga.engine.StateMachineConfig;
import org.apache.seata.saga.engine.pcext.routers.EndStateRouter;
import org.apache.seata.saga.engine.pcext.routers.TaskStateRouter;
import org.apache.seata.saga.engine.pcext.utils.EngineUtils;
import org.apache.seata.saga.proctrl.Instruction;
import org.apache.seata.saga.proctrl.ProcessContext;
import org.apache.seata.saga.proctrl.ProcessRouter;
import org.apache.seata.saga.statelang.domain.DomainConstants;
import org.apache.seata.saga.statelang.domain.State;
import org.apache.seata.saga.statelang.domain.StateMachine;
import org.apache.seata.saga.statelang.domain.StateType;

/**
 * StateMachine ProcessRouter
 *
 * @see ProcessRouter
 */
public class StateMachineProcessRouter implements ProcessRouter {

    private final Map<StateType, StateRouter> stateRouters = new ConcurrentHashMap<>();

    @Override
    public Instruction route(ProcessContext context) throws FrameworkException {

        StateInstruction stateInstruction = context.getInstruction(StateInstruction.class);

        State state;
        if (stateInstruction.getTemporaryState() != null) {
            state = stateInstruction.getTemporaryState();
            stateInstruction.setTemporaryState(null);
        } else {
            StateMachineConfig stateMachineConfig = (StateMachineConfig)context.getVariable(
                DomainConstants.VAR_NAME_STATEMACHINE_CONFIG);
            StateMachine stateMachine = stateMachineConfig.getStateMachineRepository().getStateMachine(
                stateInstruction.getStateMachineName(), stateInstruction.getTenantId());
            state = stateMachine.getStates().get(stateInstruction.getStateName());
        }

        StateType stateType = state.getType();

        StateRouter router = stateRouters.get(stateType);

        Instruction instruction = null;

        List<StateRouterInterceptor> interceptors = null;
        if (router instanceof InterceptableStateRouter) {
            interceptors = ((InterceptableStateRouter)router).getInterceptors();
        }

        List<StateRouterInterceptor> executedInterceptors = null;
        Exception exception = null;
        try {
            if (CollectionUtils.isNotEmpty(interceptors)) {
                executedInterceptors = new ArrayList<>(interceptors.size());
                for (StateRouterInterceptor interceptor : interceptors) {
                    executedInterceptors.add(interceptor);
                    interceptor.preRoute(context, state);
                }
            }

            instruction = router.route(context, state);

        } catch (Exception e) {
            exception = e;
            throw e;
        } finally {
            if (CollectionUtils.isNotEmpty(executedInterceptors)) {
                for (int i = executedInterceptors.size() - 1; i >= 0; i--) {
                    StateRouterInterceptor interceptor = executedInterceptors.get(i);
                    interceptor.postRoute(context, state, instruction, exception);
                }
            }

            //if 'Succeed' or 'Fail' State did not configured, we must end the state machine
            if (instruction == null && !stateInstruction.isEnd()) {
                EngineUtils.endStateMachine(context);
            }
        }

        return instruction;
    }

    public void initDefaultStateRouters() {
        if (!stateRouters.isEmpty()) {
            return;
        }

        TaskStateRouter taskStateRouter = new TaskStateRouter();
        stateRouters.put(StateType.SERVICE_TASK, taskStateRouter);
        stateRouters.put(StateType.SCRIPT_TASK, taskStateRouter);
        stateRouters.put(StateType.CHOICE, taskStateRouter);
        stateRouters.put(StateType.COMPENSATION_TRIGGER, taskStateRouter);
        stateRouters.put(StateType.SUB_STATE_MACHINE, taskStateRouter);
        stateRouters.put(StateType.SUB_MACHINE_COMPENSATION, taskStateRouter);
        stateRouters.put(StateType.LOOP_START, taskStateRouter);

        stateRouters.put(StateType.SUCCEED, new EndStateRouter());
        stateRouters.put(StateType.FAIL, new EndStateRouter());
    }

    public Map<StateType, StateRouter> getStateRouters() {
        return stateRouters;
    }

    public void setStateRouters(Map<StateType, StateRouter> stateRouters) {
        this.stateRouters.putAll(stateRouters);
    }
}
