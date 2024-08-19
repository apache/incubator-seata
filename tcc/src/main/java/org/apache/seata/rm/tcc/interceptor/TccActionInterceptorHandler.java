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
package org.apache.seata.rm.tcc.interceptor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.seata.common.Constants;
import org.apache.seata.common.DefaultValues;
import org.apache.seata.common.holder.ObjectHolder;
import org.apache.seata.common.util.ReflectionUtil;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.core.context.RootContext;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.integration.tx.api.fence.config.CommonFenceConfig;
import org.apache.seata.integration.tx.api.interceptor.ActionInterceptorHandler;
import org.apache.seata.integration.tx.api.interceptor.InvocationHandlerType;
import org.apache.seata.integration.tx.api.interceptor.InvocationWrapper;
import org.apache.seata.integration.tx.api.interceptor.SeataInterceptorPosition;
import org.apache.seata.integration.tx.api.interceptor.TwoPhaseBusinessActionParam;
import org.apache.seata.integration.tx.api.interceptor.handler.AbstractProxyInvocationHandler;
import org.apache.seata.rm.tcc.api.TwoPhaseBusinessAction;
import org.apache.seata.rm.tcc.utils.MethodUtils;
import org.slf4j.MDC;
import org.springframework.transaction.annotation.Transactional;


import static org.apache.seata.common.ConfigurationKeys.TCC_ACTION_INTERCEPTOR_ORDER;
import static org.apache.seata.common.Constants.BEAN_NAME_SPRING_FENCE_CONFIG;

public class TccActionInterceptorHandler extends AbstractProxyInvocationHandler {

    private static final int ORDER_NUM = ConfigurationFactory.getInstance().getInt(TCC_ACTION_INTERCEPTOR_ORDER,
            DefaultValues.TCC_ACTION_INTERCEPTOR_ORDER);

    protected ActionInterceptorHandler actionInterceptorHandler = new ActionInterceptorHandler();

    private Set<String> methodsToProxy;
    protected Object targetBean;

    protected Map<Method, Annotation> parseAnnotationCache = new ConcurrentHashMap<>();

    public TccActionInterceptorHandler(Object targetBean, Set<String> methodsToProxy) {
        this.targetBean = targetBean;
        this.methodsToProxy = methodsToProxy;
    }

    @Override
    protected Object doInvoke(InvocationWrapper invocation) throws Throwable {
        if (!RootContext.inGlobalTransaction() || RootContext.inSagaBranch()) {
            //not in transaction, or this interceptor is disabled
            return invocation.proceed();
        }
        Method method = invocation.getMethod();
        Annotation businessAction = parseAnnotation(method);

        //try method
        if (businessAction != null) {
            //save the xid
            String xid = RootContext.getXID();
            //save the previous branchType
            BranchType previousBranchType = RootContext.getBranchType();
            //if not TCC, bind TCC branchType
            if (getBranchType() != previousBranchType) {
                RootContext.bindBranchType(getBranchType());
            }
            try {
                TwoPhaseBusinessActionParam businessActionParam = createTwoPhaseBusinessActionParam(businessAction);
                initTransactionalAnnotationContext(method, targetBean, businessActionParam.getBusinessActionContext());
                //Handler the TCC Aspect, and return the business result
                return actionInterceptorHandler.proceed(method, invocation.getArguments(), xid, businessActionParam,
                        invocation::proceed);
            } finally {
                //if not TCC, unbind branchType
                if (getBranchType() != previousBranchType) {
                    RootContext.unbindBranchType();
                }
                //MDC remove branchId
                MDC.remove(RootContext.MDC_KEY_BRANCH_ID);
            }
        }

        //not TCC try method
        return invocation.proceed();
    }

    /**
     * Initializes the transaction annotation context
     * @param method                   the method
     * @param targetBean               the target bean
     * @param businessActionContext    the business action context
     */
    private void initTransactionalAnnotationContext(Method method, Object targetBean, Map<String, Object> businessActionContext) {
        Transactional transactionalAnnotation = MethodUtils.getTransactionalAnnotationByMethod(method, targetBean);
        if (transactionalAnnotation != null) {
            businessActionContext.put(Constants.TX_ISOLATION, transactionalAnnotation.isolation().value());
        }
    }

    private Annotation parseAnnotation(Method methodKey) throws NoSuchMethodException {
        Annotation result = parseAnnotationCache.computeIfAbsent(methodKey, method -> {
            Annotation twoPhaseBusinessAction = method.getAnnotation(getAnnotationClass());
            if (twoPhaseBusinessAction == null && targetBean.getClass() != null) {
                Set<Class<?>> interfaceClasses = ReflectionUtil.getInterfaces(targetBean.getClass());
                if (interfaceClasses != null) {
                    for (Class<?> interClass : interfaceClasses) {
                        try {
                            Method m = interClass.getMethod(method.getName(), method.getParameterTypes());
                            twoPhaseBusinessAction = m.getAnnotation(getAnnotationClass());
                            if (twoPhaseBusinessAction != null) {
                                // init common fence clean task if enable useTccFence
                                initCommonFenceCleanTask(twoPhaseBusinessAction);
                                break;
                            }
                        } catch (NoSuchMethodException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
            return twoPhaseBusinessAction;
        });
        return result;
    }

    protected TwoPhaseBusinessActionParam createTwoPhaseBusinessActionParam(Annotation annotation) {
        TwoPhaseBusinessAction businessAction = (TwoPhaseBusinessAction) annotation;
        TwoPhaseBusinessActionParam businessActionParam = new TwoPhaseBusinessActionParam();
        businessActionParam.setActionName(businessAction.name());
        businessActionParam.setDelayReport(businessAction.isDelayReport());
        businessActionParam.setUseCommonFence(businessAction.useTCCFence());
        businessActionParam.setBranchType(getBranchType());
        Map<String, Object> businessActionContextMap = new HashMap<>(4);
        //the phase two method name
        businessActionContextMap.put(Constants.COMMIT_METHOD, businessAction.commitMethod());
        businessActionContextMap.put(Constants.ROLLBACK_METHOD, businessAction.rollbackMethod());
        businessActionContextMap.put(Constants.ACTION_NAME, businessAction.name());
        businessActionContextMap.put(Constants.USE_COMMON_FENCE, businessAction.useTCCFence());
        businessActionParam.setBusinessActionContext(businessActionContextMap);
        return businessActionParam;
    }

    protected boolean parserCommonFenceConfig(Annotation annotation) {
        if (annotation == null) {
            return false;
        }
        TwoPhaseBusinessAction businessAction = (TwoPhaseBusinessAction) annotation;
        return businessAction.useTCCFence();
    }

    protected BranchType getBranchType() {
        return BranchType.TCC;
    }

    protected Class<? extends Annotation> getAnnotationClass() {
        return TwoPhaseBusinessAction.class;
    }

    /**
     * init common fence clean task if enable useTccFence
     *
     * @param twoPhaseBusinessAction the twoPhaseBusinessAction
     */
    private void initCommonFenceCleanTask(Annotation twoPhaseBusinessAction) {
        CommonFenceConfig commonFenceConfig = (CommonFenceConfig) ObjectHolder.INSTANCE.getObject(BEAN_NAME_SPRING_FENCE_CONFIG);
        if (commonFenceConfig == null || commonFenceConfig.getInitialized().get()) {
            return;
        }
        if (twoPhaseBusinessAction != null && parserCommonFenceConfig(twoPhaseBusinessAction)) {
            if (commonFenceConfig.getInitialized().compareAndSet(false, true)) {
                // init common fence clean task if enable useTccFence
                commonFenceConfig.init();
            }
        }
    }

    @Override
    public Set<String> getMethodsToProxy() {
        return methodsToProxy;
    }

    @Override
    public int getOrder() {
        return ORDER_NUM;
    }

    @Override
    public SeataInterceptorPosition getPosition() {
        return SeataInterceptorPosition.Any;
    }

    @Override
    public int order() {
        return 1;
    }

    @Override
    public String type() {
        return InvocationHandlerType.TwoPhaseAnnotation.name();
    }
}
