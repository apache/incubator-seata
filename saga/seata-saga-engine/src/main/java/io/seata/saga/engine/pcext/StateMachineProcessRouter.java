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
import io.seata.saga.engine.StateMachineConfig;
import io.seata.saga.engine.pcext.interceptors.EndStateRouterInterceptor;
import io.seata.saga.engine.pcext.routers.EndStateRouter;
import io.seata.saga.engine.pcext.routers.TaskStateRouter;
import io.seata.saga.engine.pcext.utils.EngineUtils;
import io.seata.saga.proctrl.Instruction;
import io.seata.saga.proctrl.ProcessContext;
import io.seata.saga.proctrl.ProcessRouter;
import io.seata.saga.statelang.domain.DomainConstants;
import io.seata.saga.statelang.domain.State;
import io.seata.saga.statelang.domain.StateMachine;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * StateMachine ProcessRouter
 *
 * @see ProcessRouter
 * @author lorne.cl
 */
public class StateMachineProcessRouter implements ProcessRouter {

    private Map<String, StateRouter> stateRouters = new ConcurrentHashMap<String, StateRouter>();

    @Override
    public Instruction route(ProcessContext context) throws FrameworkException {

        StateInstruction stateInstruction = context.getInstruction(StateInstruction.class);

        State state;
        if(stateInstruction.getTemporaryState() != null){
            state = stateInstruction.getTemporaryState();
            stateInstruction.setTemporaryState(null);
        }
        else{
            StateMachineConfig stateMachineConfig = (StateMachineConfig) context.getVariable(DomainConstants.VAR_NAME_STATEMACHINE_CONFIG);
            StateMachine stateMachine = stateMachineConfig.getStateMachineRepository().getStateMachine(stateInstruction.getStateMachineName(), stateInstruction.getTenantId());
            state = stateMachine.getStates().get(stateInstruction.getStateName());
        }


        String stateType = state.getType();

        StateRouter router = stateRouters.get(stateType);

        Instruction instruction = null;

        List<StateRouterInterceptor> interceptors = null;
        if (router instanceof InterceptibleStateRouter) {
            interceptors = ((InterceptibleStateRouter) router).getInterceptors();
        }

        List<StateRouterInterceptor> executedInterceptors = null;
        Exception exception = null;
        try {
            if (interceptors != null && interceptors.size() > 0) {
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

            if (executedInterceptors != null && executedInterceptors.size() > 0) {
                for (int i = executedInterceptors.size() - 1; i >= 0; i--) {
                    StateRouterInterceptor interceptor = executedInterceptors.get(i);
                    interceptor.postRoute(context, state, instruction, exception);
                }
            }

            //if 'Succeed' or 'Fail' State did not configured, we must end the state machine
            if(instruction == null && !stateInstruction.isEnd()){
                EngineUtils.endStateMachine(context);
            }
        }

        return instruction;
    }

    public void initDefaultStateRouters(){
        if(this.stateRouters.size() == 0){
            TaskStateRouter taskStateRouter = new TaskStateRouter();
            this.stateRouters.put(DomainConstants.STATE_TYPE_SERVICE_TASK, taskStateRouter);
            this.stateRouters.put(DomainConstants.STATE_TYPE_CHOICE, taskStateRouter);
            this.stateRouters.put(DomainConstants.STATE_TYPE_COMPENSATION_TRIGGER, taskStateRouter);
            this.stateRouters.put(DomainConstants.STATE_TYPE_SUB_STATE_MACHINE, taskStateRouter);
            this.stateRouters.put(DomainConstants.STATE_TYPE_SUB_MACHINE_COMPENSATION, taskStateRouter);

            EndStateRouter endStateRouter = new EndStateRouter();
            List<StateRouterInterceptor> stateRouterInterceptors = new ArrayList<>(1);
            stateRouterInterceptors.add(new EndStateRouterInterceptor());
            endStateRouter.setInterceptors(stateRouterInterceptors);

            this.stateRouters.put(DomainConstants.STATE_TYPE_SUCCEED, endStateRouter);
            this.stateRouters.put(DomainConstants.STATE_TYPE_FAIL, endStateRouter);
        }
    }

    public Map<String, StateRouter> getStateRouters() {
        return stateRouters;
    }

    public void setStateRouters(Map<String, StateRouter> stateRouters) {
        this.stateRouters.putAll(stateRouters);
    }
}