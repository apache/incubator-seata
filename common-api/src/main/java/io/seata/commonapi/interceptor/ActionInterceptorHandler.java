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
package io.seata.commonapi.interceptor;

import com.alibaba.fastjson.JSON;
import io.seata.common.Constants;
import io.seata.common.exception.FrameworkException;
import io.seata.common.exception.SkipCallbackWrapperException;
import io.seata.common.executor.Callback;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.NetUtil;
import io.seata.commonapi.rm.tcc.api.BusinessActionContext;
import io.seata.commonapi.rm.tcc.api.BusinessActionContextParameter;
import io.seata.commonapi.rm.tcc.api.BusinessActionContextUtil;
import io.seata.commonapi.rm.tcc.api.ParamType;
import io.seata.commonapi.fence.CommonFenceHandler;
import io.seata.commonapi.util.DubboUtil;
import io.seata.commonapi.util.SpringProxyUtils;
import io.seata.core.context.RootContext;
import io.seata.commonapi.remoting.RemotingDesc;
import io.seata.rm.DefaultResourceManager;


import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Handler the Tx Participant Aspect : Setting Context, Creating Branch Record
 *
 * @author zhangsen
 */
public class ActionInterceptorHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActionInterceptorHandler.class);

    /**
     * Handler the Tx Aspect
     *
     * @param method         the method
     * @param arguments      the arguments
     * @param xid            the xid
     * @param businessActionParam the business action params
     * @param targetCallback the target callback
     * @return the business result
     * @throws Throwable the throwable
     */
    public Object proceed(Method method, Object[] arguments, String xid, TwoPhaseBusinessActionParam businessActionParam,
                          Callback<Object> targetCallback) throws Throwable {
        //Get action context from arguments, or create a new one and then reset to arguments
        BusinessActionContext actionContext = getOrCreateActionContextAndResetToArguments(method.getParameterTypes(), arguments);

        //Set the xid
        actionContext.setXid(xid);
        //Set the action name
        String actionName = businessActionParam.getActionName();
        actionContext.setActionName(actionName);
        //Set the delay report
        actionContext.setDelayReport(businessActionParam.getDelayReport());
        //Set branch type
        actionContext.setBranchType(businessActionParam.getBranchType());

        //Creating Branch Record
        String branchId = doTxActionLogStore(method, arguments, businessActionParam, actionContext);
        actionContext.setBranchId(branchId);
        //MDC put branchId
        MDC.put(RootContext.MDC_KEY_BRANCH_ID, branchId);

        // save the previous action context
        BusinessActionContext previousActionContext = BusinessActionContextUtil.getContext();
        try {
            //share actionContext implicitly
            BusinessActionContextUtil.setContext(actionContext);

            if (businessActionParam.getUseCommonFence()) {
                try {
                    // Use common Fence, and return the business result
                    return CommonFenceHandler.prepareFence(xid, Long.valueOf(branchId), actionName, targetCallback);
                } catch (SkipCallbackWrapperException | UndeclaredThrowableException e) {
                    Throwable originException = e.getCause();
                    if (originException instanceof FrameworkException) {
                        LOGGER.error("[{}] prepare common fence error: {}", xid, originException.getMessage());
                    }
                    throw originException;
                }
            } else {
                //Execute business, and return the business result
                return targetCallback.execute();
            }
        } finally {
            try {
                //to report business action context finally if the actionContext.getUpdated() is true
                BusinessActionContextUtil.reportContext(actionContext);
            } finally {
                if (previousActionContext != null) {
                    // recovery the previous action context
                    BusinessActionContextUtil.setContext(previousActionContext);
                } else {
                    // clear the action context
                    BusinessActionContextUtil.clear();
                }
            }
        }
    }

    public Object proceedManual(Method method, Object[] arguments, String xid, TwoPhaseBusinessActionParam businessActionParam) throws Throwable {
        //Get action context from arguments, or create a new one and then reset to arguments
        BusinessActionContext actionContext = getOrCreateActionContextAndResetToArguments(method.getParameterTypes(), arguments);

        //Set the xid
        actionContext.setXid(xid);
        //Set the action name
        String actionName = businessActionParam.getActionName();
        actionContext.setActionName(actionName);
        //Set the delay report
        actionContext.setDelayReport(businessActionParam.getDelayReport());
        //Set branch type
        actionContext.setBranchType(businessActionParam.getBranchType());

        //Creating Branch Record
        String branchId = doTxActionLogStore(method, arguments, businessActionParam, actionContext);
        actionContext.setBranchId(branchId);
        //MDC put branchId
        MDC.put(RootContext.MDC_KEY_BRANCH_ID, branchId);

        //share actionContext implicitly
        BusinessActionContextUtil.setContext(actionContext);

        if (businessActionParam.getUseCommonFence()) {
            try {
                // Use common Fence, and return the business result
                EmptyCallBack emptyCallBack = new EmptyCallBack();
                return CommonFenceHandler.prepareFence(xid, Long.valueOf(branchId), actionName, emptyCallBack);
            } catch (SkipCallbackWrapperException | UndeclaredThrowableException e) {
                Throwable originException = e.getCause();
                if (originException instanceof FrameworkException) {
                    LOGGER.error("[{}] prepare common fence error: {}", xid, originException.getMessage());
                }
                throw originException;
            }
        } else {
            return null;
        }
    }

    /**
     * Get or create action context, and reset to arguments
     *
     * @param parameterTypes the par
     * @param arguments the arguments
     * @return the action context
     * @since above 1.4.2
     */
    @Nonnull
    protected BusinessActionContext getOrCreateActionContextAndResetToArguments(Class<?>[] parameterTypes, Object[] arguments) {
        BusinessActionContext actionContext = null;

        // get the action context from arguments
        int argIndex = 0;
        for (Class<?> parameterType : parameterTypes) {
            if (BusinessActionContext.class.isAssignableFrom(parameterType)) {
                actionContext = (BusinessActionContext) arguments[argIndex];
                if (actionContext == null) {
                    // If the action context exists in arguments but is null, create a new one and reset the action context to the arguments
                    actionContext = new BusinessActionContext();
                    arguments[argIndex] = actionContext;
                } else {
                    // Reset the updated, avoid unnecessary reporting
                    actionContext.setUpdated(null);
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
     * @param method              the method
     * @param arguments           the arguments
     * @param businessActionParam the business action param
     * @param actionContext       the action context
     * @return the branchId
     */
    protected String doTxActionLogStore(Method method, Object[] arguments, TwoPhaseBusinessActionParam businessActionParam,
                                        BusinessActionContext actionContext) {
        String actionName = actionContext.getActionName();
        String xid = actionContext.getXid();

        //region fetch context and init action context

        Map<String, Object> context = fetchActionRequestContext(method, arguments);
        context.put(Constants.ACTION_START_TIME, System.currentTimeMillis());

        //Init business context
        initBusinessContext(context, method, businessActionParam.getBusinessActionContext());
        //Init running environment context
        initFrameworkContext(context);

        Map<String, Object> originContext = actionContext.getActionContext();
        if (CollectionUtils.isNotEmpty(originContext)) {
            //Merge context and origin context if it exists.
            //@since: above 1.4.2
            originContext.putAll(context);
            context = originContext;
        } else {
            actionContext.setActionContext(context);
        }

        //endregion

        //Init applicationData
        Map<String, Object> applicationContext = Collections.singletonMap(Constants.TX_ACTION_CONTEXT, context);
        String applicationContextStr = JSON.toJSONString(applicationContext);
        try {
            //registry branch record
            Long branchId = DefaultResourceManager.get().branchRegister(businessActionParam.getBranchType(), actionName, null, xid,
                    applicationContextStr, null);
            return String.valueOf(branchId);
        } catch (Throwable t) {
            String msg = String.format("%s branch Register error, xid: %s", businessActionParam.getBranchType(), xid);
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
     * @param context               the context
     * @param method                the method
     * @param businessActionContext the business action map
     */
    protected void initBusinessContext(Map<String, Object> context, Method method,
                                       Map<String, Object> businessActionContext) {
        if (method != null) {
            //the phase one method name
            context.put(Constants.PREPARE_METHOD, method.getName());
        }
        if (businessActionContext != null && businessActionContext.size() > 0) {
            //the phase two method name
            context.putAll(businessActionContext);
        }
    }

    /**
     * Extracting context data from parameters, add them to the context
     *
     * @param method    the method
     * @param arguments the arguments
     * @return the context
     */
    public Map<String, Object> fetchActionRequestContext(Method method, Object[] arguments) {
        Map<String, Object> context = new HashMap<>(8);

        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        for (int i = 0; i < parameterAnnotations.length; i++) {
            for (int j = 0; j < parameterAnnotations[i].length; j++) {
                if (parameterAnnotations[i][j] instanceof BusinessActionContextParameter) {
                    // get annotation
                    BusinessActionContextParameter annotation = (BusinessActionContextParameter) parameterAnnotations[i][j];
                    if (arguments[i] == null) {
                        throw new IllegalArgumentException("@BusinessActionContextParameter 's params can not null");
                    }

                    // get param
                    Object paramObject = arguments[i];
                    if (paramObject == null) {
                        continue;
                    }

                    // load param by the config of annotation, and then put into the context
                    ActionContextUtil.loadParamByAnnotationAndPutToContext(ParamType.PARAM, "", paramObject, annotation, context);
                }
            }
        }
        return context;
    }

    /**
     * get the method from interface
     *
     * @param invocation the invocation
     * @return the action interface method
     */
    public Method getActionInterfaceMethod(MethodInvocation invocation, RemotingDesc remotingDesc) {
        Class<?> serviceType = null;
        try {
            if (remotingDesc == null) {
                serviceType = getProxyInterface(invocation.getThis());
            } else {
                serviceType = remotingDesc.getServiceClass();
            }
            if (serviceType == null && remotingDesc != null && remotingDesc.getServiceClassName() != null) {
                serviceType = Class.forName(remotingDesc.getServiceClassName(), true,
                        Thread.currentThread().getContextClassLoader());
            }
            if (serviceType == null) {
                return invocation.getMethod();
            }
            return serviceType.getMethod(invocation.getMethod().getName(),
                    invocation.getMethod().getParameterTypes());
        } catch (NoSuchMethodException e) {
            if (serviceType != null && !"toString".equals(invocation.getMethod().getName())) {
                LOGGER.warn("no such method '{}' from interface {}", invocation.getMethod().getName(), serviceType.getName());
            }
            return invocation.getMethod();
        } catch (Exception e) {
            LOGGER.warn("get Method from interface failed", e);
            return invocation.getMethod();
        }
    }

    /**
     * get the interface of proxy
     *
     * @param proxyBean the proxy bean
     * @return proxy interface
     * @throws Exception the exception
     */
    @Nullable
    protected Class<?> getProxyInterface(Object proxyBean) throws Exception {
        if (DubboUtil.isDubboProxyName(proxyBean.getClass().getName())) {
            //dubbo javaassist proxy
            return DubboUtil.getAssistInterface(proxyBean);
        } else {
            //jdk/cglib proxy
            return SpringProxyUtils.getTargetInterface(proxyBean);
        }
    }
}