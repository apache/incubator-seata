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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.seata.common.exception.FrameworkErrorCode;
import io.seata.saga.engine.StateMachineConfig;
import io.seata.saga.engine.StateMachineEngine;
import io.seata.saga.engine.exception.EngineExecutionException;
import io.seata.saga.engine.invoker.ServiceInvoker;
import io.seata.saga.engine.pcext.InterceptibleStateHandler;
import io.seata.saga.engine.pcext.StateHandler;
import io.seata.saga.engine.pcext.StateHandlerInterceptor;
import io.seata.saga.engine.pcext.StateInstruction;
import io.seata.saga.proctrl.HierarchicalProcessContext;
import io.seata.saga.proctrl.ProcessContext;
import io.seata.saga.statelang.domain.CompensateSubStateMachineState;
import io.seata.saga.statelang.domain.DomainConstants;
import io.seata.saga.statelang.domain.ExecutionStatus;
import io.seata.saga.statelang.domain.ServiceTaskState;
import io.seata.saga.statelang.domain.StateInstance;
import io.seata.saga.statelang.domain.StateMachineInstance;
import io.seata.saga.statelang.domain.TaskState;
import io.seata.saga.statelang.domain.impl.AbstractTaskState;
import io.seata.saga.statelang.domain.impl.ServiceTaskStateImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.StringUtils;

/**
 * ServiceTaskState Handler
 *
 * @author lorne.cl
 */
public class ServiceTaskStateHandler implements StateHandler, InterceptibleStateHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceTaskStateHandler.class);

    private List<StateHandlerInterceptor> interceptors;

    public static void handleException(ProcessContext context, AbstractTaskState state, Throwable e) {
        List<TaskState.ExceptionMatch> catches = state.getCatches();
        if (catches != null && catches.size() > 0) {
            for (TaskState.ExceptionMatch exceptionMatch : catches) {

                List<String> exceptions = exceptionMatch.getExceptions();
                List<Class<? extends Exception>> exceptionClasses = exceptionMatch.getExceptionClasses();
                if (exceptions != null && exceptions.size() > 0) {

                    if (exceptionClasses == null) {
                        synchronized (exceptionMatch) {
                            exceptionClasses = exceptionMatch.getExceptionClasses();
                            if (exceptionClasses == null) {

                                exceptionClasses = new ArrayList<>(exceptions.size());
                                for (String expStr : exceptions) {

                                    Class<? extends Exception> expClass = null;
                                    try {
                                        expClass = (Class<? extends Exception>) ServiceTaskStateHandler.class
                                                .getClassLoader().loadClass(expStr);
                                    } catch (Exception e1) {

                                        LOGGER.warn("Cannot Load Exception Class by getClass().getClassLoader()", e1);

                                        try {
                                            expClass = (Class<? extends Exception>) Thread.currentThread()
                                                    .getContextClassLoader().loadClass(expStr);
                                        } catch (Exception e2) {
                                            LOGGER.warn(
                                                    "Cannot Load Exception Class by Thread.currentThread()"
                                                            + ".getContextClassLoader()",
                                                    e2);
                                        }
                                    }

                                    if (expClass != null) {
                                        exceptionClasses.add(expClass);
                                    }
                                }
                                exceptionMatch.setExceptionClasses(exceptionClasses);
                            }
                        }
                    }

                    for (Class<? extends Exception> expClass : exceptionClasses) {
                        if (expClass.isAssignableFrom(e.getClass())) {
                            ((HierarchicalProcessContext) context).setVariableLocally(
                                    DomainConstants.VAR_NAME_CURRENT_EXCEPTION_ROUTE, exceptionMatch.getNext());
                            return;
                        }
                    }

                }
            }
        }

        LOGGER.error("Task execution failed and no catches configured");
        ((HierarchicalProcessContext) context).setVariableLocally(DomainConstants.VAR_NAME_IS_EXCEPTION_NOT_CATCH, true);
    }

    @Override
    public void process(ProcessContext context) throws EngineExecutionException {

        StateInstruction instruction = context.getInstruction(StateInstruction.class);
        ServiceTaskStateImpl state = (ServiceTaskStateImpl) instruction.getState(context);

        String serviceName = state.getServiceName();
        String methodName = state.getServiceMethod();
        StateInstance stateInstance = (StateInstance) context.getVariable(DomainConstants.VAR_NAME_STATE_INST);

        Object result;
        try {

            List<Object> input = (List<Object>) context.getVariable(DomainConstants.VAR_NAME_INPUT_PARAMS);

            //Set the current task execution status to RU (Running)
            stateInstance.setStatus(ExecutionStatus.RU);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(">>>>>>>>>>>>>>>>>>>>>> Start to execute State[{}], ServiceName[{}], Method[{}], Input:{}",
                        state.getName(), serviceName, methodName, input);
            }

            if (state instanceof CompensateSubStateMachineState) {
                //If it is the compensation of the substate machine,
                // directly call the state machine's compensate method
                result = compensateSubStateMachine(context, state, input, stateInstance,
                        (StateMachineEngine) context.getVariable(DomainConstants.VAR_NAME_STATEMACHINE_ENGINE));
            } else {
                StateMachineConfig stateMachineConfig = (StateMachineConfig) context.getVariable(
                        DomainConstants.VAR_NAME_STATEMACHINE_CONFIG);

                ServiceInvoker serviceInvoker = stateMachineConfig.getServiceInvokerManager().getServiceInvoker(
                        state.getServiceType());
                if (serviceInvoker == null) {
                    throw new EngineExecutionException("No such ServiceInvoker[" + state.getServiceType() + "]",
                            FrameworkErrorCode.ObjectNotExists);
                }
                if (serviceInvoker instanceof ApplicationContextAware) {
                    ((ApplicationContextAware) serviceInvoker).setApplicationContext(
                            stateMachineConfig.getApplicationContext());
                }

                result = serviceInvoker.invoke(state, input.toArray());
            }

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("<<<<<<<<<<<<<<<<<<<<<< State[{}], ServiceName[{}], Method[{}] Execute finish. result: {}",
                        state.getName(), serviceName, methodName, result);
            }

            if (result != null) {
                stateInstance.setOutputParams(result);
                ((HierarchicalProcessContext) context).setVariableLocally(DomainConstants.VAR_NAME_OUTPUT_PARAMS,
                        result);
            }

        } catch (Throwable e) {

            LOGGER.error("<<<<<<<<<<<<<<<<<<<<<< State[{}], ServiceName[{}], Method[{}] Execute failed.",
                    state.getName(), serviceName, methodName, e);

            ((HierarchicalProcessContext) context).setVariableLocally(DomainConstants.VAR_NAME_CURRENT_EXCEPTION, e);

            handleException(context, state, e);
        }

    }

    private Object compensateSubStateMachine(ProcessContext context, ServiceTaskState state, Object input,
                                             StateInstance stateInstance, StateMachineEngine engine) {

        String subStateMachineParentId = (String) context.getVariable(
                state.getName() + DomainConstants.VAR_NAME_SUB_MACHINE_PARENT_ID);
        if (StringUtils.isEmpty(subStateMachineParentId)) {
            throw new EngineExecutionException("sub statemachine parentId is required",
                    FrameworkErrorCode.ObjectNotExists);
        }

        StateMachineConfig stateMachineConfig = (StateMachineConfig) context.getVariable(
                DomainConstants.VAR_NAME_STATEMACHINE_CONFIG);
        List<StateMachineInstance> subInst = stateMachineConfig.getStateLogStore().queryStateMachineInstanceByParentId(
                subStateMachineParentId);
        if (subInst == null || subInst.size() <= 0) {
            throw new EngineExecutionException(
                    "cannot find sub statemachine instance by parentId:" + subStateMachineParentId,
                    FrameworkErrorCode.ObjectNotExists);
        }

        String subStateMachineInstId = subInst.get(0).getId();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(">>>>>>>>>>>>>>>>>>>>>> Start to compensate sub statemachine [id:{}]", subStateMachineInstId);
        }

        Map<String, Object> startParams = new HashMap<>(0);
        if (input instanceof List) {
            List<Object> listInputParams = (List<Object>) input;
            if (listInputParams.size() > 0) {
                startParams = (Map<String, Object>) listInputParams.get(0);
            }
        } else if (input instanceof Map) {
            startParams = (Map<String, Object>) input;
        }

        StateMachineInstance compensateInst = engine.compensate(subStateMachineInstId, startParams);
        stateInstance.setStatus(compensateInst.getCompensationStatus());

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(
                    "<<<<<<<<<<<<<<<<<<<<<< Compensate sub statemachine [id:{}] finished with status[{}], "
                            + "compensateState[{}]",
                    subStateMachineInstId, compensateInst.getStatus(), compensateInst.getCompensationStatus());
        }
        return compensateInst.getEndParams();
    }

    @Override
    public List<StateHandlerInterceptor> getInterceptors() {
        return interceptors;
    }

    public void setInterceptors(List<StateHandlerInterceptor> interceptors) {
        this.interceptors = interceptors;
    }
}