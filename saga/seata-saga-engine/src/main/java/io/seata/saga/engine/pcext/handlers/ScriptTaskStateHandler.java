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

import io.seata.common.exception.FrameworkErrorCode;
import io.seata.common.util.CollectionUtils;
import io.seata.saga.engine.StateMachineConfig;
import io.seata.saga.engine.exception.EngineExecutionException;
import io.seata.saga.engine.pcext.InterceptableStateHandler;
import io.seata.saga.engine.pcext.StateHandler;
import io.seata.saga.engine.pcext.StateHandlerInterceptor;
import io.seata.saga.engine.pcext.StateInstruction;
import io.seata.saga.engine.pcext.utils.EngineUtils;
import io.seata.saga.proctrl.HierarchicalProcessContext;
import io.seata.saga.proctrl.ProcessContext;
import io.seata.saga.statelang.domain.DomainConstants;
import io.seata.saga.statelang.domain.impl.ScriptTaskStateImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.SimpleBindings;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ScriptTaskState Handler
 *
 * @author lorne.cl
 */
public class ScriptTaskStateHandler implements StateHandler, InterceptableStateHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScriptTaskStateHandler.class);

    private List<StateHandlerInterceptor> interceptors = new ArrayList<>();

    private volatile Map<String, ScriptEngine> scriptEngineCache = new ConcurrentHashMap<>();

    @Override
    public void process(ProcessContext context) throws EngineExecutionException {

        StateInstruction instruction = context.getInstruction(StateInstruction.class);
        ScriptTaskStateImpl state = (ScriptTaskStateImpl) instruction.getState(context);

        String scriptType = state.getScriptType();
        String scriptContent = state.getScriptContent();

        Object result;
        try {
            List<Object> input = (List<Object>) context.getVariable(DomainConstants.VAR_NAME_INPUT_PARAMS);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(">>>>>>>>>>>>>>>>>>>>>> Start to execute ScriptTaskState[{}], ScriptType[{}], Input:{}",
                        state.getName(), scriptType, input);
            }

            StateMachineConfig stateMachineConfig = (StateMachineConfig) context.getVariable(
                    DomainConstants.VAR_NAME_STATEMACHINE_CONFIG);

            ScriptEngine scriptEngine = getScriptEngineFromCache(scriptType, stateMachineConfig.getScriptEngineManager());
            if (scriptEngine == null) {
                throw new EngineExecutionException("No such ScriptType[" + scriptType + "]",
                        FrameworkErrorCode.ObjectNotExists);
            }

            Bindings bindings = null;
            Map<String, Object> inputMap = null;
            if (CollectionUtils.isNotEmpty(input) && input.get(0) instanceof Map) {
                inputMap = (Map<String, Object>) input.get(0);
            }
            List<Object> inputExps = state.getInput();
            if (CollectionUtils.isNotEmpty(inputExps) && inputExps.get(0) instanceof Map) {
                Map<String, Object> inputExpMap = (Map<String, Object>) inputExps.get(0);
                if (inputExpMap.size() > 0) {
                    bindings = new SimpleBindings();
                    for (String property : inputExpMap.keySet()) {
                        if (inputMap != null && inputMap.containsKey(property)) {
                            bindings.put(property, inputMap.get(property));
                        } else {
                            //if we do not bind the null value property, groovy will throw MissingPropertyException
                            bindings.put(property, null);
                        }
                    }
                }
            }
            if (bindings != null) {
                result = scriptEngine.eval(scriptContent, bindings);
            }
            else {
                result = scriptEngine.eval(scriptContent);
            }

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("<<<<<<<<<<<<<<<<<<<<<< ScriptTaskState[{}], ScriptType[{}], Execute finish. result: {}",
                        state.getName(), scriptType, result);
            }

            if (result != null) {
                ((HierarchicalProcessContext) context).setVariableLocally(DomainConstants.VAR_NAME_OUTPUT_PARAMS,
                        result);
            }

        } catch (Throwable e) {

            LOGGER.error("<<<<<<<<<<<<<<<<<<<<<< ScriptTaskState[{}], ScriptTaskState[{}] Execute failed.",
                    state.getName(), scriptType, e);

            ((HierarchicalProcessContext) context).setVariableLocally(DomainConstants.VAR_NAME_CURRENT_EXCEPTION, e);

            EngineUtils.handleException(context, state, e);
        }

    }

    protected ScriptEngine getScriptEngineFromCache(String scriptType, ScriptEngineManager scriptEngineManager) {
        return CollectionUtils.computeIfAbsent(scriptEngineCache, scriptType,
            key -> scriptEngineManager.getEngineByName(scriptType));
    }

    @Override
    public List<StateHandlerInterceptor> getInterceptors() {
        return interceptors;
    }

    @Override
    public void addInterceptor(StateHandlerInterceptor interceptor) {
        if (interceptors != null && !interceptors.contains(interceptor)) {
            interceptors.add(interceptor);
        }
    }

    public void setInterceptors(List<StateHandlerInterceptor> interceptors) {
        this.interceptors = interceptors;
    }
}