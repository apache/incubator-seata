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

import java.util.Map;

import io.seata.saga.engine.exception.EngineExecutionException;
import io.seata.saga.engine.pcext.StateHandler;
import io.seata.saga.engine.pcext.StateInstruction;
import io.seata.saga.proctrl.ProcessContext;
import io.seata.saga.statelang.domain.DomainConstants;
import io.seata.saga.statelang.domain.FailEndState;

/**
 * FailEndState Handler
 *
 * @author lorne.cl
 */
public class FailEndStateHandler implements StateHandler {

    @Override
    public void process(ProcessContext context) throws EngineExecutionException {

        context.setVariable(DomainConstants.VAR_NAME_FAIL_END_STATE_FLAG, true);

        StateInstruction instruction = context.getInstruction(StateInstruction.class);
        FailEndState state = (FailEndState)instruction.getState(context);
        Map<String, Object> contextVariables = (Map<String, Object>)context.getVariable(
            DomainConstants.VAR_NAME_STATEMACHINE_CONTEXT);
        contextVariables.put(DomainConstants.VAR_NAME_STATEMACHINE_ERROR_CODE, state.getErrorCode());
        contextVariables.put(DomainConstants.VAR_NAME_STATEMACHINE_ERROR_MSG, state.getMessage());
    }
}