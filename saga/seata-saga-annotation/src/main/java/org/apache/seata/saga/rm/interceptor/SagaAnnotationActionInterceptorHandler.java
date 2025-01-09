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
package org.apache.seata.saga.rm.interceptor;

import org.apache.seata.common.Constants;
import org.apache.seata.common.util.ReflectionUtil;
import org.apache.seata.core.context.RootContext;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.integration.tx.api.interceptor.ActionInterceptorHandler;
import org.apache.seata.integration.tx.api.interceptor.InvocationHandlerType;
import org.apache.seata.integration.tx.api.interceptor.InvocationWrapper;
import org.apache.seata.integration.tx.api.interceptor.SeataInterceptorPosition;
import org.apache.seata.integration.tx.api.interceptor.TwoPhaseBusinessActionParam;
import org.apache.seata.integration.tx.api.interceptor.handler.AbstractProxyInvocationHandler;
import org.apache.seata.saga.rm.api.CompensationBusinessAction;
import org.slf4j.MDC;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * saga-annotation invocationHandler
 */
public class SagaAnnotationActionInterceptorHandler extends AbstractProxyInvocationHandler {

    private Set<String> methodsToProxy;

    protected Object targetBean;

    protected ActionInterceptorHandler actionInterceptorHandler = new ActionInterceptorHandler();

    protected Map<Method, Annotation> parseAnnotationCache = new ConcurrentHashMap<>();

    public SagaAnnotationActionInterceptorHandler(Object targetBean, Set<String> methodsToProxy) {
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


    @Override
    public Set<String> getMethodsToProxy() {
        return methodsToProxy;
    }

    @Override
    public SeataInterceptorPosition getPosition() {
        return SeataInterceptorPosition.Any;
    }

    @Override
    public String type() {
        return InvocationHandlerType.SagaAnnotation.name();
    }

    @Override
    public int order() {
        return 2;
    }

    private TwoPhaseBusinessActionParam createTwoPhaseBusinessActionParam(Annotation annotation) {
        CompensationBusinessAction businessAction = (CompensationBusinessAction) annotation;

        TwoPhaseBusinessActionParam businessActionParam = new TwoPhaseBusinessActionParam();
        businessActionParam.setActionName(businessAction.name());
        businessActionParam.setDelayReport(businessAction.isDelayReport());
        businessActionParam.setUseCommonFence(businessAction.useFence());
        businessActionParam.setBranchType(BranchType.SAGA_ANNOTATION);

        Map<String, Object> businessActionContextMap = new HashMap<>(4);
        businessActionContextMap.put(Constants.ROLLBACK_METHOD, businessAction.compensationMethod());
        businessActionContextMap.put(Constants.ACTION_NAME, businessAction.name());
        businessActionContextMap.put(Constants.USE_COMMON_FENCE, businessAction.useFence());
        businessActionParam.setBusinessActionContext(businessActionContextMap);

        return businessActionParam;
    }

    private Annotation parseAnnotation(Method methodKey) {
        return parseAnnotationCache.computeIfAbsent(methodKey, method -> {
            Annotation twoPhaseBusinessAction = method.getAnnotation(getAnnotationClass());
            if (twoPhaseBusinessAction == null) {
                Set<Class<?>> interfaceClasses = ReflectionUtil.getInterfaces(targetBean.getClass());
                for (Class<?> interClass : interfaceClasses) {
                    try {
                        Method m = interClass.getMethod(method.getName(), method.getParameterTypes());
                        twoPhaseBusinessAction = m.getAnnotation(getAnnotationClass());
                    } catch (NoSuchMethodException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            return twoPhaseBusinessAction;
        });
    }

    private BranchType getBranchType() {
        return BranchType.SAGA_ANNOTATION;
    }

    private Class<? extends Annotation> getAnnotationClass() {
        return CompensationBusinessAction.class;
    }

}