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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

import io.seata.common.util.CollectionUtils;
import io.seata.common.util.StringUtils;
import io.seata.saga.engine.AsyncCallback;
import io.seata.saga.engine.StateMachineConfig;
import io.seata.saga.engine.pcext.StateInstruction;
import io.seata.saga.engine.pcext.handlers.ScriptTaskStateHandler;
import io.seata.saga.proctrl.HierarchicalProcessContext;
import io.seata.saga.proctrl.ProcessContext;
import io.seata.saga.statelang.domain.DomainConstants;
import io.seata.saga.statelang.domain.StateInstance;
import io.seata.saga.statelang.domain.StateMachineInstance;
import io.seata.saga.statelang.domain.TaskState;
import io.seata.saga.statelang.domain.TaskState.ExceptionMatch;
import io.seata.saga.statelang.domain.impl.AbstractTaskState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author lorne.cl
 */
public class EngineUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(EngineUtils.class);

    /**
     * generate parent id
     *
     * @param stateInstance the state instance
     * @return the state instance parent id
     */
    public static String generateParentId(StateInstance stateInstance) {
        return stateInstance.getMachineInstanceId() + DomainConstants.SEPERATOR_PARENT_ID + stateInstance.getId();
    }

    /**
     * get origin state name without suffix like fork
     *
     * @param stateInstance the state instance
     * @return the origin state name
     * @see LoopTaskUtils#generateLoopStateName(ProcessContext, String)
     */
    public static String getOriginStateName(StateInstance stateInstance) {
        String stateName = stateInstance.getName();
        if (StringUtils.isNotBlank(stateName)) {
            int end = stateName.lastIndexOf(LoopTaskUtils.LOOP_STATE_NAME_PATTERN);
            if (end > -1) {
                return stateName.substring(0, end);
            }
        }
        return stateName;
    }

    /**
     * end StateMachine
     *
     * @param context the process context
     */
    public static void endStateMachine(ProcessContext context) {

        if (context.hasVariable(DomainConstants.VAR_NAME_IS_LOOP_STATE)) {
            if (context.hasVariable(DomainConstants.LOOP_SEMAPHORE)) {
                Semaphore semaphore = (Semaphore)context.getVariable(DomainConstants.LOOP_SEMAPHORE);
                semaphore.release();
            }
            return;
        }

        StateMachineInstance stateMachineInstance = (StateMachineInstance)context.getVariable(
            DomainConstants.VAR_NAME_STATEMACHINE_INST);

        stateMachineInstance.setGmtEnd(new Date());

        Exception exp = (Exception)context.getVariable(DomainConstants.VAR_NAME_CURRENT_EXCEPTION);
        if (exp != null) {
            stateMachineInstance.setException(exp);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Exception Occurred: " + exp);
            }
        }

        StateMachineConfig stateMachineConfig = (StateMachineConfig)context.getVariable(
            DomainConstants.VAR_NAME_STATEMACHINE_CONFIG);

        stateMachineConfig.getStatusDecisionStrategy().decideOnEndState(context, stateMachineInstance, exp);

        stateMachineInstance.getEndParams().putAll(
            (Map<String, Object>)context.getVariable(DomainConstants.VAR_NAME_STATEMACHINE_CONTEXT));

        StateInstruction instruction = context.getInstruction(StateInstruction.class);
        instruction.setEnd(true);

        stateMachineInstance.setRunning(false);
        stateMachineInstance.setGmtEnd(new Date());

        if (stateMachineInstance.getStateMachine().isPersist() && stateMachineConfig.getStateLogStore() != null) {
            stateMachineConfig.getStateLogStore().recordStateMachineFinished(stateMachineInstance, context);
        }

        AsyncCallback callback = (AsyncCallback)context.getVariable(DomainConstants.VAR_NAME_ASYNC_CALLBACK);
        if (callback != null) {
            if (exp != null) {
                callback.onError(context, stateMachineInstance, exp);
            } else {
                callback.onFinished(context, stateMachineInstance);
            }
        }
    }

    /**
     * fail StateMachine
     *
     * @param context the process context
     * @param exp the exception
     */
    public static void failStateMachine(ProcessContext context, Exception exp) {

        if (context.hasVariable(DomainConstants.VAR_NAME_IS_LOOP_STATE)) {
            return;
        }

        StateMachineInstance stateMachineInstance = (StateMachineInstance)context.getVariable(
            DomainConstants.VAR_NAME_STATEMACHINE_INST);

        StateMachineConfig stateMachineConfig = (StateMachineConfig)context.getVariable(
            DomainConstants.VAR_NAME_STATEMACHINE_CONFIG);

        stateMachineConfig.getStatusDecisionStrategy().decideOnTaskStateFail(context, stateMachineInstance, exp);

        stateMachineInstance.getEndParams().putAll(
            (Map<String, Object>)context.getVariable(DomainConstants.VAR_NAME_STATEMACHINE_CONTEXT));

        StateInstruction instruction = context.getInstruction(StateInstruction.class);
        instruction.setEnd(true);

        stateMachineInstance.setRunning(false);
        stateMachineInstance.setGmtEnd(new Date());
        stateMachineInstance.setException(exp);

        if (stateMachineInstance.getStateMachine().isPersist() && stateMachineConfig.getStateLogStore() != null) {
            stateMachineConfig.getStateLogStore().recordStateMachineFinished(stateMachineInstance, context);
        }

        AsyncCallback callback = (AsyncCallback)context.getVariable(DomainConstants.VAR_NAME_ASYNC_CALLBACK);
        if (callback != null) {
            callback.onError(context, stateMachineInstance, exp);
        }
    }

    /**
     * test if is timeout
     * @param gmtUpdated the engine update gmt
     * @param timeoutMillis the timeout millis
     * @return the boolean
     */
    public static boolean isTimeout(Date gmtUpdated, int timeoutMillis) {
        if (gmtUpdated == null || timeoutMillis < 0) {
            return false;
        }
        return System.currentTimeMillis() - gmtUpdated.getTime() > timeoutMillis;
    }

    /**
     * Handle exceptions while ServiceTask or ScriptTask Executing
     *
     * @param context the process context
     * @param state the task state
     * @param e the throwable
     */
    public static void handleException(ProcessContext context, AbstractTaskState state, Throwable e) {
        List<ExceptionMatch> catches = state.getCatches();
        if (CollectionUtils.isNotEmpty(catches)) {
            for (TaskState.ExceptionMatch exceptionMatch : catches) {

                List<String> exceptions = exceptionMatch.getExceptions();
                List<Class<? extends Exception>> exceptionClasses = exceptionMatch.getExceptionClasses();
                if (CollectionUtils.isNotEmpty(exceptions)) {
                    if (exceptionClasses == null) {
                        synchronized (exceptionMatch) {
                            exceptionClasses = exceptionMatch.getExceptionClasses();
                            if (exceptionClasses == null) {

                                exceptionClasses = new ArrayList<>(exceptions.size());
                                for (String expStr : exceptions) {

                                    Class<? extends Exception> expClass = null;
                                    try {
                                        expClass = (Class<? extends Exception>) ScriptTaskStateHandler.class
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
}