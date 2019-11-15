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
package io.seata.integration.dubbo.alibaba;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.rpc.Filter;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.RpcInvocation;
import io.seata.core.context.RootContext;
import io.seata.core.model.BranchType;
import io.seata.rm.tcc.api.TwoPhaseBusinessAction;
import io.seata.rm.tcc.interceptor.ActionInterceptorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Map;

@Activate(group = {Constants.CONSUMER}, order = 95)
public class TccReferenceAnnotationFilter implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(TccReferenceAnnotationFilter.class);

    private ActionInterceptorHandler actionInterceptorHandler = new ActionInterceptorHandler();

    private final static String DUBBO_GENERIC_SERVICE_INVOKE = "$invoke";

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        try {
            RpcInvocation rpcInvocation = (RpcInvocation) invocation;
            String methodName = rpcInvocation.getMethodName();
            Class<?>[] parameterTypes = rpcInvocation.getParameterTypes();
            Object[] arguments = rpcInvocation.getArguments();
            Class interfaceClass = invoker.getInterface();
            if (DUBBO_GENERIC_SERVICE_INVOKE.equals(methodName)) {
                return invoker.invoke(invocation);
            }

            Method method = interfaceClass.getMethod(methodName, parameterTypes);
            //not in transaction
            if (!RootContext.inGlobalTransaction()) {
                return invoker.invoke(invocation);
            }

            TwoPhaseBusinessAction businessAction = method.getAnnotation(TwoPhaseBusinessAction.class);
            //try method
            if (businessAction != null) {
                //save the xid
                String xid = RootContext.getXID();
                //clear the context
                RootContext.unbind();
                RootContext.bindFilterType(xid, BranchType.TCC);
                try {
                    Map<String, Object> ret = actionInterceptorHandler.proceed(method, arguments, xid, businessAction,
                            () -> invoker.invoke(invocation));
                    return (Result) ret.get(io.seata.common.Constants.TCC_METHOD_RESULT);
                }  finally {
                    //recovery the context
                    RootContext.unbindFilterType();
                    RootContext.bind(xid);
                }
            }
        } catch (Throwable e) {
            LOGGER.error("Tcc dubbo invokes service to register branch transaction exception:{}", e.getMessage(), e);
            throw new RpcException(e);
        }
        return invoker.invoke(invocation);
    }
}
