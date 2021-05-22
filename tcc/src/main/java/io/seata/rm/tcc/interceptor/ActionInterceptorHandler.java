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
package io.seata.rm.tcc.interceptor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import io.seata.common.Constants;
import io.seata.common.exception.FrameworkException;
import io.seata.common.executor.Callback;
import io.seata.common.util.NetUtil;
import io.seata.common.util.StringUtils;
import io.seata.core.context.RootContext;
import io.seata.core.model.BranchType;
import io.seata.rm.DefaultResourceManager;
import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.BusinessActionContextParameter;
import io.seata.rm.tcc.api.BusinessActionContextUtil;
import io.seata.rm.tcc.api.TwoPhaseBusinessAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Handler the TCC Participant Aspect : Setting Context, Creating Branch Record
 *
 * @author zhangsen
 */
public class ActionInterceptorHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActionInterceptorHandler.class);

    /**
     * Handler the TCC Aspect
     *
     * @param method         the method
     * @param arguments      the arguments
     * @param businessAction the business action
     * @param targetCallback the target callback
     * @return the business result
     * @throws Throwable the throwable
     */
    public Object proceed(Method method, Object[] arguments, String xid, TwoPhaseBusinessAction businessAction,
                                       Callback<Object> targetCallback) throws Throwable {
        //Get action context from arguments, or create a new one and then reset to arguments
        BusinessActionContext actionContext = getOrCreateActionContextAndResetToArguments(method.getParameterTypes(), arguments);

        //Set the xid
        actionContext.setXid(xid);
        //Set the action name
        String actionName = businessAction.name();
        actionContext.setActionName(actionName);

        //Creating Branch Record
        String branchId = doTccActionLogStore(method, arguments, businessAction, actionContext);
        actionContext.setBranchId(branchId);
        //MDC put branchId
        MDC.put(RootContext.MDC_KEY_BRANCH_ID, branchId);

        //share actionContext implicitly
        BusinessActionContextUtil.setContext(actionContext);
        try {
            //Execute business, and return business result
            return targetCallback.execute();
        } finally {
            BusinessActionContextUtil.clear();
            //to report business action context finally.
            if (businessAction.isDelayReport() || Boolean.TRUE.equals(actionContext.getUpdated())) {
                BusinessActionContextUtil.reportContext(actionContext);
            }
        }
    }

    /**
     * Get or create action context, and reset to arguments
     *
     * @param arguments the arguments
     * @return the action context
     * @since above 1.4.2
     */
    protected BusinessActionContext getOrCreateActionContextAndResetToArguments(Class<?>[] parameterTypes, Object[] arguments) {
        BusinessActionContext actionContext = null;

        // get the action context from arguments
        int argIndex = 0;
        for (Class<?> parameterType : parameterTypes) {
            if (BusinessActionContext.class.isAssignableFrom(parameterType)) {
                actionContext = (BusinessActionContext)arguments[argIndex];
                //If the action context exists in arguments but is null, create a new one and reset the action context to the arguments
                if (actionContext == null) {
                    actionContext = new BusinessActionContext();
                    arguments[argIndex] = actionContext;
                }
                break;
            }
            argIndex++;
        }

        // if null, create a new one
        if (actionContext == null) {
            actionContext = new BusinessActionContext();
        }
        return actionContext;
    }

    /**
     * Creating Branch Record
     *
     * @param method         the method
     * @param arguments      the arguments
     * @param businessAction the business action
     * @param actionContext  the action context
     * @return the branchId
     */
    protected String doTccActionLogStore(Method method, Object[] arguments, TwoPhaseBusinessAction businessAction,
                                         BusinessActionContext actionContext) {
        String actionName = actionContext.getActionName();
        String xid = actionContext.getXid();

        Map<String, Object> context = fetchActionRequestContext(method, arguments);
        context.put(Constants.ACTION_START_TIME, System.currentTimeMillis());

        //Init business context
        initBusinessContext(context, method, businessAction);
        //Init running environment context
        initFrameworkContext(context);
        actionContext.setDelayReport(businessAction.isDelayReport());
        //Merge context and origin context if it exists.  @since above 1.4.2
        Map<String, Object> originContext = actionContext.getActionContext();
        if (originContext == null) {
            actionContext.setActionContext(context);
        } else {
            originContext.putAll(context);
            context = originContext;
        }

        //init applicationData
        Map<String, Object> applicationContext = new HashMap<>(4);
        applicationContext.put(Constants.TCC_ACTION_CONTEXT, context);
        String applicationContextStr = JSON.toJSONString(applicationContext);
        try {
            //registry branch record
            Long branchId = DefaultResourceManager.get().branchRegister(BranchType.TCC, actionName, null, xid,
                    applicationContextStr, null);
            return String.valueOf(branchId);
        } catch (Throwable t) {
            String msg = String.format("TCC branch Register error, xid: %s", xid);
            LOGGER.error(msg, t);
            throw new FrameworkException(t, msg);
        }
    }

    /**
     * Init running environment context
     *
     * @param context the context
     */
    protected void initFrameworkContext(Map<String, Object> context) {
        try {
            context.put(Constants.HOST_NAME, NetUtil.getLocalIp());
        } catch (Throwable t) {
            LOGGER.warn("getLocalIP error", t);
        }
    }

    /**
     * Init business context
     *
     * @param context        the context
     * @param method         the method
     * @param businessAction the business action
     */
    protected void initBusinessContext(Map<String, Object> context, Method method,
                                       TwoPhaseBusinessAction businessAction) {
        if (method != null) {
            //the phase one method name
            context.put(Constants.PREPARE_METHOD, method.getName());
        }
        if (businessAction != null) {
            //the phase two method name
            context.put(Constants.COMMIT_METHOD, businessAction.commitMethod());
            context.put(Constants.ROLLBACK_METHOD, businessAction.rollbackMethod());
            context.put(Constants.ACTION_NAME, businessAction.name());
        }
    }

    /**
     * Extracting context data from parameters, add them to the context
     *
     * @param method    the method
     * @param arguments the arguments
     * @return the context
     */
    protected Map<String, Object> fetchActionRequestContext(Method method, Object[] arguments) {
        Map<String, Object> context = new HashMap<>(8);

        // get the parameter names
        String[] parameterNames = ActionContextUtil.getParameterNames(method);
        Parameter[] parameters = null;
        if (parameterNames == null) {
            parameters = method.getParameters();
        }

        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        for (int i = 0; i < parameterAnnotations.length; i++) {
            for (int j = 0; j < parameterAnnotations[i].length; j++) {
                if (parameterAnnotations[i][j] instanceof BusinessActionContextParameter) {
                    // get annotation
                    BusinessActionContextParameter annotation = (BusinessActionContextParameter)parameterAnnotations[i][j];
                    if (arguments[i] == null) {
                        throw new IllegalArgumentException("@BusinessActionContextParameter 's params can not null");
                    }

                    // get param
                    Object paramObject = arguments[i];
                    if (paramObject == null) {
                        continue;
                    }

                    // if the parameter names is null, print log
                    if (parameterNames == null && StringUtils.isBlank(annotation.paramName()) && !annotation.isParamInProperty()) {
                        String errorMsg = String.format("Unable to get parameter names from the method `%s.%s(...)`." +
                                        " Please execute 'javac -parameters' to re-compile of the method code," +
                                        " or set the field `paramName` of the `@%s` by yourself",
                                method.getDeclaringClass().getSimpleName(), method.getName(), BusinessActionContextParameter.class.getSimpleName());
                        throw new FrameworkException(errorMsg);
                    }

                    // load param by the config of annotation, and then put to the context
                    String paramName = parameterNames != null ? parameterNames[i] : parameters[i].getName();
                    ActionContextUtil.loadParamByAnnotationAndPutToContext("param", paramName, paramObject, annotation, context);
                }
            }
        }
        return context;
    }
}
