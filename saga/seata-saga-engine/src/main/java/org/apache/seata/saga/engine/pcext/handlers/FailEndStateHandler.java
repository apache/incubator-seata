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
package org.apache.seata.saga.engine.pcext.handlers;

import java.util.Map;

import org.apache.seata.saga.engine.exception.EngineExecutionException;
import org.apache.seata.saga.engine.pcext.StateHandler;
import org.apache.seata.saga.engine.pcext.StateInstruction;
import org.apache.seata.saga.proctrl.ProcessContext;
import org.apache.seata.saga.statelang.domain.DomainConstants;
import org.apache.seata.saga.statelang.domain.FailEndState;

/**
 * FailEndState Handler
 *
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
