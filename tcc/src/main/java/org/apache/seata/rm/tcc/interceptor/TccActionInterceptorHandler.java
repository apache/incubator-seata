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
import org.apache.seata.integration.tx.api.interceptor.InvocationWrapper;
import org.apache.seata.integration.tx.api.interceptor.SeataInterceptorPosition;
import org.apache.seata.integration.tx.api.interceptor.TwoPhaseBusinessActionParam;
import org.apache.seata.integration.tx.api.interceptor.handler.AbstractProxyInvocationHandler;
import org.apache.seata.rm.tcc.api.TwoPhaseBusinessAction;
import org.slf4j.MDC;

import static org.apache.seata.common.ConfigurationKeys.TCC_ACTION_INTERCEPTOR_ORDER;
import static org.apache.seata.common.Constants.BEAN_NAME_SPRING_FENCE_CONFIG;


public class TccActionInterceptorHandler extends AbstractProxyInvocationHandler {

    private static final int ORDER_NUM = ConfigurationFactory.getInstance().getInt(TCC_ACTION_INTERCEPTOR_ORDER,
            DefaultValues.TCC_ACTION_INTERCEPTOR_ORDER);

    private ActionInterceptorHandler actionInterceptorHandler = new ActionInterceptorHandler();

    private Set<String> methodsToProxy;
    private Object targetBean;

    private Map<Method, TwoPhaseBusinessAction> parseAnnotationCache = new ConcurrentHashMap<>();

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
        TwoPhaseBusinessAction businessAction = parseAnnotation(method);

        //try method
        if (businessAction != null) {
            //save the xid
            String xid = RootContext.getXID();
            //save the previous branchType
            BranchType previousBranchType = RootContext.getBranchType();
            //if not TCC, bind TCC branchType
            if (BranchType.TCC != previousBranchType) {
                RootContext.bindBranchType(BranchType.TCC);
            }
            try {
                TwoPhaseBusinessActionParam businessActionParam = new TwoPhaseBusinessActionParam();
                businessActionParam.setActionName(businessAction.name());
                businessActionParam.setDelayReport(businessAction.isDelayReport());
                businessActionParam.setUseCommonFence(businessAction.useTCCFence());
                businessActionParam.setBranchType(BranchType.TCC);
                Map<String, Object> businessActionContextMap = new HashMap<>(4);
                //the phase two method name
                businessActionContextMap.put(Constants.COMMIT_METHOD, businessAction.commitMethod());
                businessActionContextMap.put(Constants.ROLLBACK_METHOD, businessAction.rollbackMethod());
                businessActionContextMap.put(Constants.ACTION_NAME, businessAction.name());
                businessActionContextMap.put(Constants.USE_COMMON_FENCE, businessAction.useTCCFence());
                businessActionParam.setBusinessActionContext(businessActionContextMap);
                //Handler the TCC Aspect, and return the business result
                return actionInterceptorHandler.proceed(method, invocation.getArguments(), xid, businessActionParam,
                        invocation::proceed);
            } finally {
                //if not TCC, unbind branchType
                if (BranchType.TCC != previousBranchType) {
                    RootContext.unbindBranchType();
                }
                //MDC remove branchId
                MDC.remove(RootContext.MDC_KEY_BRANCH_ID);
            }
        }

        //not TCC try method
        return invocation.proceed();
    }

    private TwoPhaseBusinessAction parseAnnotation(Method methodKey) throws NoSuchMethodException {
        TwoPhaseBusinessAction result = parseAnnotationCache.computeIfAbsent(methodKey, method -> {
            TwoPhaseBusinessAction businessAction = method.getAnnotation(TwoPhaseBusinessAction.class);
            if (businessAction == null && targetBean.getClass() != null) {
                Set<Class<?>> interfaceClasses = ReflectionUtil.getInterfaces(targetBean.getClass());
                if (interfaceClasses != null) {
                    for (Class<?> interClass : interfaceClasses) {
                        try {
                            Method m = interClass.getMethod(method.getName(), method.getParameterTypes());
                            businessAction = m.getAnnotation(TwoPhaseBusinessAction.class);
                            if (businessAction != null) {
                                // init common fence clean task if enable useTccFence
                                initCommonFenceCleanTask(businessAction);
                                break;
                            }
                        } catch (NoSuchMethodException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
            return businessAction;
        });
        return result;
    }

    /**
     * init common fence clean task if enable useTccFence
     *
     * @param twoPhaseBusinessAction the twoPhaseBusinessAction
     */
    private void initCommonFenceCleanTask(TwoPhaseBusinessAction twoPhaseBusinessAction) {
        CommonFenceConfig commonFenceConfig = (CommonFenceConfig) ObjectHolder.INSTANCE.getObject(BEAN_NAME_SPRING_FENCE_CONFIG);
        if (commonFenceConfig == null || commonFenceConfig.getInitialized().get()) {
            return;
        }
        if (twoPhaseBusinessAction != null && twoPhaseBusinessAction.useTCCFence()) {
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

}
