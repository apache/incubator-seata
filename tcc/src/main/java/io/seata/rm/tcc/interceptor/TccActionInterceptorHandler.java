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

import io.seata.common.Constants;
import io.seata.common.util.ReflectionUtil;
import io.seata.core.context.RootContext;
import io.seata.core.model.BranchType;
import io.seata.integrationapi.interceptor.ActionInterceptorHandler;
import io.seata.integrationapi.interceptor.InvocationWrapper;
import io.seata.integrationapi.interceptor.TwoPhaseBusinessActionParam;
import io.seata.integrationapi.interceptor.handler.AbstractProxyInvocationHandler;
import io.seata.integrationapi.remoting.RemotingDesc;
import io.seata.rm.tcc.api.TwoPhaseBusinessAction;
import org.slf4j.MDC;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author leezongjie
 * @date 2022/11/26
 */
public class TccActionInterceptorHandler extends AbstractProxyInvocationHandler {

    private ActionInterceptorHandler actionInterceptorHandler = new ActionInterceptorHandler();

    private Set<String> methodsToProxy;
    private RemotingDesc remotingDesc;

    private Map<Method, TwoPhaseBusinessAction> parseAnnotationCache = new ConcurrentHashMap<>();

    public TccActionInterceptorHandler(RemotingDesc remotingDesc, Set<String> methodsToProxy) {
        this.remotingDesc = remotingDesc;
        this.methodsToProxy = methodsToProxy;
    }

    @Override
    public Set<String> getMethodsToProxy() {
        return methodsToProxy;
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
            if (businessAction == null && remotingDesc.getServiceClass() != null) {
                Set<Class<?>> interfaceClasses = ReflectionUtil.getInterfaces(remotingDesc.getServiceClass());
                if (interfaceClasses != null) {
                    for (Class<?> interClass : interfaceClasses) {
                        try {
                            Method m = interClass.getMethod(method.getName(), method.getParameterTypes());
                            businessAction = m.getAnnotation(TwoPhaseBusinessAction.class);
                            if (businessAction != null) {
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

}
