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
package io.seata.saga.engine.pcext.utils;

import io.seata.saga.engine.AsyncCallback;
import io.seata.saga.engine.StateMachineConfig;
import io.seata.saga.engine.pcext.StateInstruction;
import io.seata.saga.engine.utils.ExceptionUtils;
import io.seata.saga.proctrl.ProcessContext;
import io.seata.saga.statelang.domain.DomainConstants;
import io.seata.saga.statelang.domain.StateInstance;
import io.seata.saga.statelang.domain.StateMachineInstance;
import java.util.Date;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author lorne.cl
 */
public class EngineUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionUtils.class);

    /**
     * generate parent id
     *
     * @param stateInstance
     * @return
     */
    public static String generateParentId(StateInstance stateInstance){
        return stateInstance.getMachineInstanceId() + ":" + stateInstance.getId();
    }

    /**
     * end StateMachine
     * @param context
     */
    public static void endStateMachine(ProcessContext context) {

        StateMachineInstance stateMachineInstance = (StateMachineInstance) context.getVariable(DomainConstants.VAR_NAME_STATEMACHINE_INST);

        stateMachineInstance.setGmtEnd(new Date());

        Exception exp = (Exception) context.getVariable(DomainConstants.VAR_NAME_CURRENT_EXCEPTION);
        if (exp != null) {
            stateMachineInstance.setException(exp);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Exception Occurred: " + exp);
            }
        }

        StateMachineConfig stateMachineConfig = (StateMachineConfig) context.getVariable(DomainConstants.VAR_NAME_STATEMACHINE_CONFIG);

        stateMachineConfig.getStatusDecisionStrategy().decideOnEndState(context, stateMachineInstance, exp);

        stateMachineInstance.setRunning(false);
        stateMachineInstance.getEndParams().putAll((Map<String, Object>) context.getVariable(DomainConstants.VAR_NAME_STATEMACHINE_CONTEXT));

        StateInstruction instruction = context.getInstruction(StateInstruction.class);
        instruction.setEnd(true);

        if (stateMachineInstance.getStateMachine().isPersist() && stateMachineConfig.getStateLogStore() != null) {
            stateMachineInstance.getEndParams().putAll((Map<String, Object>) context.getVariable(DomainConstants.VAR_NAME_STATEMACHINE_CONTEXT));
            stateMachineConfig.getStateLogStore().recordStateMachineFinished(stateMachineInstance, context);
        }

        AsyncCallback callback = (AsyncCallback) context.getVariable(DomainConstants.VAR_NAME_ASYNC_CALLBACK);
        if(callback != null){
            if(exp != null){
                callback.onError(context, stateMachineInstance, exp);
            }
            else{
                callback.onFinished(context, stateMachineInstance);
            }
        }
    }

    /**
     * fail StateMachine
     * @param context
     * @param exp
     */
    public static void failStateMachine(ProcessContext context, Exception exp) {

        StateMachineInstance stateMachineInstance = (StateMachineInstance) context.getVariable(DomainConstants.VAR_NAME_STATEMACHINE_INST);

        StateMachineConfig stateMachineConfig = (StateMachineConfig)context.getVariable(DomainConstants.VAR_NAME_STATEMACHINE_CONFIG);

        stateMachineConfig.getStatusDecisionStrategy().decideOnTaskStateFail(context, stateMachineInstance, exp);

        stateMachineInstance.getEndParams().putAll((Map<String, Object>) context.getVariable(DomainConstants.VAR_NAME_STATEMACHINE_CONTEXT));

        StateInstruction instruction = context.getInstruction(StateInstruction.class);
        instruction.setEnd(true);

        if(stateMachineInstance.getStateMachine().isPersist() && stateMachineConfig.getStateLogStore() != null){
            stateMachineConfig.getStateLogStore().recordStateMachineFinished(stateMachineInstance, context);
        }

        AsyncCallback callback = (AsyncCallback) context.getVariable(DomainConstants.VAR_NAME_ASYNC_CALLBACK);
        if(callback != null){
            callback.onError(context, stateMachineInstance, exp);
        }
    }
}