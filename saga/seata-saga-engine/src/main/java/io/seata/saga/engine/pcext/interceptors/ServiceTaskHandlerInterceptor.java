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
package io.seata.saga.engine.pcext.interceptors;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.seata.common.exception.FrameworkErrorCode;
import io.seata.saga.engine.StateMachineConfig;
import io.seata.saga.engine.evaluation.Evaluator;
import io.seata.saga.engine.evaluation.EvaluatorFactory;
import io.seata.saga.engine.evaluation.EvaluatorFactoryManager;
import io.seata.saga.engine.evaluation.expression.ExpressionEvaluator;
import io.seata.saga.engine.exception.EngineExecutionException;
import io.seata.saga.engine.expression.Expression;
import io.seata.saga.engine.expression.ExpressionFactory;
import io.seata.saga.engine.expression.ExpressionFactoryManager;
import io.seata.saga.engine.expression.seq.SequenceExpression;
import io.seata.saga.engine.pcext.StateHandlerInterceptor;
import io.seata.saga.engine.pcext.StateInstruction;
import io.seata.saga.engine.pcext.utils.CompensationHolder;
import io.seata.saga.engine.pcext.utils.EngineUtils;
import io.seata.saga.engine.utils.ExceptionUtils;
import io.seata.saga.proctrl.HierarchicalProcessContext;
import io.seata.saga.proctrl.ProcessContext;
import io.seata.saga.statelang.domain.DomainConstants;
import io.seata.saga.statelang.domain.ExecutionStatus;
import io.seata.saga.statelang.domain.StateInstance;
import io.seata.saga.statelang.domain.StateMachineInstance;
import io.seata.saga.statelang.domain.impl.ServiceTaskStateImpl;
import io.seata.saga.statelang.domain.impl.StateInstanceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 * ServiceTaskHandler Interceptor
 *
 * @author lorne.cl
 */
public class ServiceTaskHandlerInterceptor implements StateHandlerInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceTaskHandlerInterceptor.class);

    private static List<Object> createInputParams(ExpressionFactoryManager expressionFactoryManager,
                                                  StateInstanceImpl stateInstance,
                                                  ServiceTaskStateImpl serviceTaskState, Object variablesFrom) {

        List<Object> inputAssignments = serviceTaskState.getInput();
        if (inputAssignments == null || inputAssignments.size() == 0) {
            return new ArrayList<>(0);
        }

        List<Object> inputExpressions = serviceTaskState.getInputExpressions();
        if (inputExpressions == null) {
            synchronized (serviceTaskState) {
                inputExpressions = serviceTaskState.getInputExpressions();
                if (inputExpressions == null) {
                    inputExpressions = new ArrayList<>(inputAssignments.size());
                    for (Object inputAssignment : inputAssignments) {
                        inputExpressions.add(createValueExpression(expressionFactoryManager, inputAssignment));
                    }
                }
                serviceTaskState.setInputExpressions(inputExpressions);
            }
        }
        List<Object> inputValues = new ArrayList<>(inputExpressions.size());
        for (Object valueExpression : inputExpressions) {
            Object value = getValue(valueExpression, variablesFrom, stateInstance);
            inputValues.add(value);
        }

        return inputValues;
    }

    public static Map<String, Object> createOutputParams(ExpressionFactoryManager expressionFactoryManager,
                                                         ServiceTaskStateImpl serviceTaskState, Object variablesFrom) {

        Map<String, Object> outputAssignments = serviceTaskState.getOutput();
        if (outputAssignments == null || outputAssignments.size() == 0) {
            return new LinkedHashMap<>(0);
        }

        Map<String, Object> outputExpressions = serviceTaskState.getOutputExpressions();
        if (outputExpressions == null) {
            synchronized (serviceTaskState) {
                outputExpressions = serviceTaskState.getOutputExpressions();
                if (outputExpressions == null) {
                    outputExpressions = new LinkedHashMap<>(outputAssignments.size());
                    for (String paramName : outputAssignments.keySet()) {
                        outputExpressions.put(paramName,
                            createValueExpression(expressionFactoryManager, outputAssignments.get(paramName)));
                    }
                }
                serviceTaskState.setOutputExpressions(outputExpressions);
            }
        }
        Map<String, Object> outputValues = new LinkedHashMap<>(outputExpressions.size());
        for (String paramName : outputExpressions.keySet()) {
            outputValues.put(paramName, getValue(outputExpressions.get(paramName), variablesFrom, null));
        }
        return outputValues;
    }

    private static Object getValue(Object valueExpression, Object variablesFrom, StateInstance stateInstance) {
        if (valueExpression instanceof Expression) {
            Object value = ((Expression)valueExpression).getValue(variablesFrom);
            if (value != null && stateInstance != null && StringUtils.isEmpty(stateInstance.getBusinessKey())
                && valueExpression instanceof SequenceExpression) {
                stateInstance.setBusinessKey(String.valueOf(value));
            }
            return value;
        } else if (valueExpression instanceof Map) {
            Map<String, Object> mapValueExpression = (Map<String, Object>)valueExpression;
            Map<String, Object> mapValue = new LinkedHashMap<>();
            for (String paramName : mapValueExpression.keySet()) {
                Object value = getValue(mapValueExpression.get(paramName), variablesFrom, stateInstance);
                if (value != null) {
                    mapValue.put(paramName, value);
                }
            }
            return mapValue;
        } else if (valueExpression instanceof List) {
            List<Object> listValueExpression = (List<Object>)valueExpression;
            List<Object> listValue = new ArrayList<>(listValueExpression.size());
            for (Object aValueExpression : listValueExpression) {
                listValue.add(getValue(aValueExpression, variablesFrom, stateInstance));
            }
            return listValue;
        } else {
            return valueExpression;
        }
    }

    private static Object createValueExpression(ExpressionFactoryManager expressionFactoryManager,
                                                Object paramAssignment) {

        Object valueExpression;

        if (paramAssignment instanceof Expression) {
            valueExpression = paramAssignment;
        } else if (paramAssignment instanceof Map) {
            Map<String, Object> paramMapAssignment = (Map<String, Object>)paramAssignment;
            Map<String, Object> paramMap = new LinkedHashMap<>(paramMapAssignment.size());
            for (String paramName : paramMapAssignment.keySet()) {
                Object valueAssignment = paramMapAssignment.get(paramName);
                paramMap.put(paramName, createValueExpression(expressionFactoryManager, valueAssignment));
            }
            valueExpression = paramMap;
        } else if (paramAssignment instanceof List) {
            List<Object> paramListAssignment = (List<Object>)paramAssignment;
            List<Object> paramList = new ArrayList<>(paramListAssignment.size());
            for (Object aParamAssignment : paramListAssignment) {
                paramList.add(createValueExpression(expressionFactoryManager, aParamAssignment));
            }
            valueExpression = paramList;
        } else if (paramAssignment instanceof String && ((String)paramAssignment).startsWith("$")) {

            String expressionStr = (String)paramAssignment;
            int expTypeStart = expressionStr.indexOf("$");
            int expTypeEnd = expressionStr.indexOf(".", expTypeStart);

            String expressionType = null;
            if (expTypeStart >= 0 && expTypeEnd > expTypeStart) {
                expressionType = expressionStr.substring(expTypeStart + 1, expTypeEnd);
            }

            int expEnd = expressionStr.length();
            String expressionContent = null;
            if (expTypeEnd > 0 && expEnd > expTypeEnd) {
                expressionContent = expressionStr.substring(expTypeEnd + 1, expEnd);
            }

            ExpressionFactory expressionFactory = expressionFactoryManager.getExpressionFactory(expressionType);
            if (expressionFactory == null) {
                throw new IllegalArgumentException("Cannot get ExpressionFactory by Type[" + expressionType + "]");
            }
            valueExpression = expressionFactory.createExpression(expressionContent);
        } else {
            valueExpression = paramAssignment;
        }
        return valueExpression;
    }

    @Override
    public void preProcess(ProcessContext context) throws EngineExecutionException {

        StateInstruction instruction = context.getInstruction(StateInstruction.class);

        StateMachineInstance stateMachineInstance = (StateMachineInstance)context.getVariable(
            DomainConstants.VAR_NAME_STATEMACHINE_INST);
        StateMachineConfig stateMachineConfig = (StateMachineConfig)context.getVariable(
            DomainConstants.VAR_NAME_STATEMACHINE_CONFIG);

        StateInstanceImpl stateInstance = new StateInstanceImpl();

        Map<String, Object> contextVariables = (Map<String, Object>)context.getVariable(
            DomainConstants.VAR_NAME_STATEMACHINE_CONTEXT);
        ServiceTaskStateImpl state = (ServiceTaskStateImpl)instruction.getState(context);
        List<Object> serviceInputParams = null;
        if (contextVariables != null) {
            try {
                serviceInputParams = createInputParams(stateMachineConfig.getExpressionFactoryManager(), stateInstance,
                    state, contextVariables);
            } catch (Exception e) {

                String message = "Task [" + state.getName()
                    + "] input parameters assign failed, please check from/to expression:" + e.getMessage();

                EngineExecutionException exception = ExceptionUtils.createEngineExecutionException(e,
                    FrameworkErrorCode.VariablesAssignError, message, stateMachineInstance, state.getName());

                EngineUtils.failStateMachine(context, exception);

                throw exception;
            }
        }

        ((HierarchicalProcessContext)context).setVariableLocally(DomainConstants.VAR_NAME_INPUT_PARAMS,
            serviceInputParams);

        stateInstance.setMachineInstanceId(stateMachineInstance.getId());
        stateInstance.setStateMachineInstance(stateMachineInstance);
        stateInstance.setName(state.getName());
        stateInstance.setGmtStarted(new Date());
        stateInstance.setStatus(ExecutionStatus.RU);

        stateInstance.setStateIdRetriedFor(
            (String)context.getVariable(state.getName() + DomainConstants.VAR_NAME_RETRIED_STATE_INST_ID));

        if (StringUtils.hasLength(stateInstance.getBusinessKey())) {

            ((Map<String, Object>)context.getVariable(DomainConstants.VAR_NAME_STATEMACHINE_CONTEXT)).put(
                state.getName() + DomainConstants.VAR_NAME_BUSINESSKEY, stateInstance.getBusinessKey());
        }

        stateInstance.setType(state.getType());

        stateInstance.setForUpdate(state.isForUpdate());
        stateInstance.setServiceName(state.getServiceName());
        stateInstance.setServiceMethod(state.getServiceMethod());
        stateInstance.setServiceType(state.getServiceType());

        Object isForCompensation = state.isForCompensation();
        if (isForCompensation != null && (Boolean)isForCompensation) {
            CompensationHolder compensationHolder = CompensationHolder.getCurrent(context, true);
            StateInstance stateToBeCompensated = compensationHolder.getStatesNeedCompensation().get(state.getName());
            if (stateToBeCompensated != null) {

                stateToBeCompensated.setCompensationState(stateInstance);
                stateInstance.setStateIdCompensatedFor(stateToBeCompensated.getId());
            } else {
                LOGGER.error("Compensation State[{}] has no state to compensate, maybe this is a bug.",
                    state.getName());
            }
            CompensationHolder.getCurrent(context, true).addForCompensationState(stateInstance.getName(),
                stateInstance);
        }

        if (DomainConstants.OPERATION_NAME_FORWARD.equals(context.getVariable(DomainConstants.VAR_NAME_OPERATION_NAME))
            && StringUtils.isEmpty(stateInstance.getStateIdRetriedFor()) && !state.isForCompensation()) {

            List<StateInstance> stateList = stateMachineInstance.getStateList();
            if (stateList != null && stateList.size() > 0) {
                for (int i = stateList.size() - 1; i >= 0; i--) {
                    StateInstance executedState = stateList.get(i);

                    if (stateInstance.getName().equals(executedState.getName())) {
                        stateInstance.setStateIdRetriedFor(executedState.getId());
                        executedState.setIgnoreStatus(true);
                        break;
                    }
                }
            }
        }

        stateInstance.setInputParams(serviceInputParams);

        if (stateMachineInstance.getStateMachine().isPersist() && state.isPersist()
            && stateMachineConfig.getStateLogStore() != null) {

            stateMachineConfig.getStateLogStore().recordStateStarted(stateInstance, context);
        }

        if (StringUtils.isEmpty(stateInstance.getId())) {
            stateInstance.setId(stateMachineConfig.getSeqGenerator().generate(DomainConstants.SEQ_ENTITY_STATE_INST));
        }
        stateMachineInstance.putStateInstance(stateInstance.getId(), stateInstance);
        ((HierarchicalProcessContext)context).setVariableLocally(DomainConstants.VAR_NAME_STATE_INST, stateInstance);
    }

    @Override
    public void postProcess(ProcessContext context, Exception exp) throws EngineExecutionException {

        StateInstruction instruction = context.getInstruction(StateInstruction.class);
        ServiceTaskStateImpl state = (ServiceTaskStateImpl)instruction.getState(context);

        StateMachineInstance stateMachineInstance = (StateMachineInstance)context.getVariable(
            DomainConstants.VAR_NAME_STATEMACHINE_INST);
        StateInstance stateInstance = (StateInstance)context.getVariable(DomainConstants.VAR_NAME_STATE_INST);
        if (stateInstance == null) {
            return;
        }

        StateMachineConfig stateMachineConfig = (StateMachineConfig)context.getVariable(
            DomainConstants.VAR_NAME_STATEMACHINE_CONFIG);

        if (exp == null) {
            exp = (Exception)context.getVariable(DomainConstants.VAR_NAME_CURRENT_EXCEPTION);
        }
        stateInstance.setException(exp);

        decideExecutionStatus(context, stateInstance, state, exp);

        if (ExecutionStatus.SU.equals(stateInstance.getStatus()) && exp != null) {

            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(
                    "Although an exception occurs, the execution status map to SU, and the exception is ignored when "
                        + "the execution status decision.");
            }
            context.removeVariable(DomainConstants.VAR_NAME_CURRENT_EXCEPTION);
        }

        Map<String, Object> contextVariables = (Map<String, Object>)context.getVariable(
            DomainConstants.VAR_NAME_STATEMACHINE_CONTEXT);
        Object serviceOutputParams = context.getVariable(DomainConstants.VAR_NAME_OUTPUT_PARAMS);
        if (serviceOutputParams != null) {
            try {
                Map<String, Object> outputVariablesToContext = createOutputParams(
                    stateMachineConfig.getExpressionFactoryManager(), state, serviceOutputParams);
                if (outputVariablesToContext != null && outputVariablesToContext.size() > 0) {
                    contextVariables.putAll(outputVariablesToContext);
                }
            } catch (Exception e) {
                String message = "Task [" + state.getName()
                    + "] output parameters assign failed, please check from/to expression:" + e.getMessage();

                EngineExecutionException exception = ExceptionUtils.createEngineExecutionException(e,
                    FrameworkErrorCode.VariablesAssignError, message, stateMachineInstance, stateInstance);

                if (stateMachineInstance.getStateMachine().isPersist() && state.isPersist()
                    && stateMachineConfig.getStateLogStore() != null) {

                    stateMachineConfig.getStateLogStore().recordStateFinished(stateInstance, context);
                }

                EngineUtils.failStateMachine(context, exception);

                throw exception;
            }
        }

        context.removeVariable(DomainConstants.VAR_NAME_OUTPUT_PARAMS);
        context.removeVariable(DomainConstants.VAR_NAME_INPUT_PARAMS);

        stateInstance.setGmtEnd(new Date());

        if (stateMachineInstance.getStateMachine().isPersist() && state.isPersist()
            && stateMachineConfig.getStateLogStore() != null) {
            stateMachineConfig.getStateLogStore().recordStateFinished(stateInstance, context);
        }

        if (exp != null && context.getVariable(DomainConstants.VAR_NAME_IS_EXCEPTION_NOT_CATCH) != null
            && (Boolean)context.getVariable(DomainConstants.VAR_NAME_IS_EXCEPTION_NOT_CATCH)) {
            //If there is an exception and there is no catch, need to exit the state machine to execute.

            context.removeVariable(DomainConstants.VAR_NAME_IS_EXCEPTION_NOT_CATCH);
            EngineUtils.failStateMachine(context, exp);
        }

    }

    private void decideExecutionStatus(ProcessContext context, StateInstance stateInstance, ServiceTaskStateImpl state,
                                       Exception exp) {

        Map<String, String> statusMatchList = state.getStatus();
        if (statusMatchList != null && statusMatchList.size() > 0) {

            if (state.isAsync()) {
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn(
                        "Service[{}.{}] is execute asynchronously, null return value collected, so user defined "
                            + "Status Matching skipped. stateName: {}, branchId: {}", state.getServiceName(),
                        state.getServiceMethod(), state.getName(), stateInstance.getId());
                }
            } else {

                StateMachineConfig stateMachineConfig = (StateMachineConfig)context.getVariable(
                    DomainConstants.VAR_NAME_STATEMACHINE_CONFIG);

                Map<Object, String> statusEvaluators = state.getStatusEvaluators();
                if (statusEvaluators == null) {
                    synchronized (state) {
                        statusEvaluators = state.getStatusEvaluators();
                        if (statusEvaluators == null) {
                            statusEvaluators = new LinkedHashMap<>(statusMatchList.size());
                            for (String expressionStr : statusMatchList.keySet()) {

                                String statusVal = statusMatchList.get(expressionStr);
                                Evaluator evaluator = createEvaluator(stateMachineConfig.getEvaluatorFactoryManager(),
                                    expressionStr);
                                if (evaluator != null) {
                                    statusEvaluators.put(evaluator, statusVal);
                                }
                            }
                        }
                        state.setStatusEvaluators(statusEvaluators);
                    }
                }

                for (Object evaluatorObj : statusEvaluators.keySet()) {
                    Evaluator evaluator = (Evaluator)evaluatorObj;
                    String statusVal = statusEvaluators.get(evaluator);
                    if (evaluator.evaluate(context.getVariables())) {
                        stateInstance.setStatus(ExecutionStatus.valueOf(statusVal));
                        break;
                    }
                }

                if (exp == null && (stateInstance.getStatus() == null || ExecutionStatus.RU.equals(
                    stateInstance.getStatus()))) {

                    if (state.isForUpdate()) {
                        stateInstance.setStatus(ExecutionStatus.UN);
                    } else {
                        stateInstance.setStatus(ExecutionStatus.FA);
                    }
                    stateInstance.setGmtEnd(new Date());

                    StateMachineInstance stateMachineInstance = stateInstance.getStateMachineInstance();

                    if (stateMachineInstance.getStateMachine().isPersist() && state.isPersist()
                        && stateMachineConfig.getStateLogStore() != null) {
                        stateMachineConfig.getStateLogStore().recordStateFinished(stateInstance, context);
                    }

                    EngineExecutionException exception = new EngineExecutionException("State [" + state.getName()
                        + "] execute finished, but cannot matching status, pls check its status manually",
                        FrameworkErrorCode.NoMatchedStatus);
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("State[{}] execute finish with status[{}]", state.getName(),
                            stateInstance.getStatus());
                    }
                    EngineUtils.failStateMachine(context, exception);

                    throw exception;
                }
            }
        }

        if (stateInstance.getStatus() == null || ExecutionStatus.RU.equals(stateInstance.getStatus())) {

            if (exp == null) {
                stateInstance.setStatus(ExecutionStatus.SU);
            } else {
                if (state.isForUpdate() || state.isForCompensation()) {

                    stateInstance.setStatus(ExecutionStatus.UN);
                    ExceptionUtils.NetExceptionType t = ExceptionUtils.getNetExceptionType(exp);
                    if (t != null) {
                        if (t.equals(ExceptionUtils.NetExceptionType.CONNECT_EXCEPTION)) {
                            stateInstance.setStatus(ExecutionStatus.FA);
                        } else if (t.equals(ExceptionUtils.NetExceptionType.READ_TIMEOUT_EXCEPTION)) {
                            stateInstance.setStatus(ExecutionStatus.UN);
                        }
                    } else {
                        stateInstance.setStatus(ExecutionStatus.UN);
                    }
                } else {
                    stateInstance.setStatus(ExecutionStatus.FA);
                }
            }
        }

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("State[{}] finish with status[{}]", state.getName(), stateInstance.getStatus());
        }
    }

    private Evaluator createEvaluator(EvaluatorFactoryManager evaluatorFactoryManager, String expressionStr) {
        String expressionType = null;
        String expressionContent = null;
        Evaluator evaluator = null;
        if (StringUtils.hasLength(expressionStr)) {
            if (expressionStr.startsWith("$")) {
                int expTypeStart = expressionStr.indexOf("$");
                int expTypeEnd = expressionStr.indexOf("{", expTypeStart);

                if (expTypeStart >= 0 && expTypeEnd > expTypeStart) {
                    expressionType = expressionStr.substring(expTypeStart + 1, expTypeEnd);
                }

                int expEnd = expressionStr.lastIndexOf("}");
                if (expTypeEnd > 0 && expEnd > expTypeEnd) {
                    expressionContent = expressionStr.substring(expTypeEnd + 1, expEnd);
                }
            } else {
                expressionContent = expressionStr;
            }

            EvaluatorFactory evaluatorFactory = evaluatorFactoryManager.getEvaluatorFactory(expressionType);
            if (evaluatorFactory == null) {
                throw new IllegalArgumentException("Cannot get EvaluatorFactory by Type[" + expressionType + "]");
            }
            evaluator = evaluatorFactory.createEvaluator(expressionContent);
            if (evaluator instanceof ExpressionEvaluator) {
                ((ExpressionEvaluator)evaluator).setRootObjectName(DomainConstants.VAR_NAME_OUTPUT_PARAMS);
            }
        }
        return evaluator;
    }
}
