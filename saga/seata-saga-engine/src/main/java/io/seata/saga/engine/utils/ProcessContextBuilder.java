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
package io.seata.saga.engine.utils;

import java.util.Map;

import io.seata.saga.engine.AsyncCallback;
import io.seata.saga.engine.StateMachineConfig;
import io.seata.saga.engine.StateMachineEngine;
import io.seata.saga.proctrl.Instruction;
import io.seata.saga.proctrl.ProcessContext;
import io.seata.saga.proctrl.ProcessType;
import io.seata.saga.proctrl.impl.ProcessContextImpl;
import io.seata.saga.statelang.domain.DomainConstants;
import io.seata.saga.statelang.domain.StateInstance;
import io.seata.saga.statelang.domain.StateMachineInstance;

/**
 * Process Context Builder
 *
 * @author lorne.cl
 */
public class ProcessContextBuilder {

    private ProcessContextImpl processContext;

    private ProcessContextBuilder() {
        this.processContext = new ProcessContextImpl();
    }

    public static ProcessContextBuilder create() {
        return new ProcessContextBuilder();
    }

    public ProcessContext build() {
        return processContext;
    }

    public ProcessContextBuilder withProcessType(ProcessType processType) {
        if (processType != null) {
            this.processContext.setVariable(ProcessContext.VAR_NAME_PROCESS_TYPE, processType);
        }
        return this;
    }

    public ProcessContextBuilder withAsyncCallback(AsyncCallback asyncCallback) {
        if (asyncCallback != null) {
            this.processContext.setVariable(DomainConstants.VAR_NAME_ASYNC_CALLBACK, asyncCallback);
        }
        return this;
    }

    public ProcessContextBuilder withInstruction(Instruction instruction) {
        if (instruction != null) {
            this.processContext.setInstruction(instruction);
        }
        return this;
    }

    public ProcessContextBuilder withStateMachineInstance(StateMachineInstance stateMachineInstance) {
        if (stateMachineInstance != null) {
            this.processContext.setVariable(DomainConstants.VAR_NAME_STATEMACHINE_INST, stateMachineInstance);
            this.processContext.setVariable(DomainConstants.VAR_NAME_STATEMACHINE,
                stateMachineInstance.getStateMachine());
        }
        return this;
    }

    public ProcessContextBuilder withStateMachineEngine(StateMachineEngine stateMachineEngine) {
        if (stateMachineEngine != null) {
            this.processContext.setVariable(DomainConstants.VAR_NAME_STATEMACHINE_ENGINE, stateMachineEngine);
        }
        return this;
    }

    public ProcessContextBuilder withStateMachineConfig(StateMachineConfig stateMachineConfig) {
        if (stateMachineConfig != null) {
            this.processContext.setVariable(DomainConstants.VAR_NAME_STATEMACHINE_CONFIG, stateMachineConfig);
        }
        return this;
    }

    public ProcessContextBuilder withStateMachineContextVariables(Map<String, Object> contextVariables) {
        if (contextVariables != null) {
            this.processContext.setVariable(DomainConstants.VAR_NAME_STATEMACHINE_CONTEXT, contextVariables);
        }
        return this;
    }

    public ProcessContextBuilder withOperationName(String operationName) {
        if (operationName != null) {
            this.processContext.setVariable(DomainConstants.VAR_NAME_OPERATION_NAME, operationName);
        }
        return this;
    }

    public ProcessContextBuilder withStateInstance(StateInstance stateInstance) {
        if (stateInstance != null) {
            this.processContext.setVariable(DomainConstants.VAR_NAME_STATE_INST, stateInstance);
        }
        return this;
    }
}