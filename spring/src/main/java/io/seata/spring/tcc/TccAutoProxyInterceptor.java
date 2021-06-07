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
package io.seata.spring.tcc;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import io.seata.common.Constants;
import io.seata.core.context.RootContext;
import io.seata.rm.tcc.api.BusinessActionContext;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.core.Ordered;

/**
 * TCC Auto Proxy Interceptor
 *
 * @author wang.liang
 */
public class TccAutoProxyInterceptor implements MethodInterceptor, Ordered {

    private final String beanName;
    private final TccAutoProxyAction tccAutoProxyAction;
    private final int orderNum;

    public TccAutoProxyInterceptor(String beanName, TccAutoProxyAction tccAutoProxyAction, int orderNum) {
        this.beanName = beanName;
        this.tccAutoProxyAction = tccAutoProxyAction;
        this.orderNum = orderNum;
    }

    @Override
    public Object invoke(final MethodInvocation invocation) throws Throwable {
        // ignored if the global transaction not exists
        if (!RootContext.inGlobalTransaction()) {
            return invocation.proceed();
        }

        // get method
        Method method = invocation.getMethod();

        // check whether the method can be proxied
        if (method.getReturnType() != Void.class) {
            return invocation.proceed();
        }

        // get the context of the invocation
        String methodName = method.getName();
        Class<?>[] parameterTypes = method.getParameterTypes();
        Object[] args = invocation.getArguments();

        // TODO: 待 PR #3797 合并后，才可以如下编写（try方法，支持方法外自己实例化BusinessActionContext）
        // create actionContext
        BusinessActionContext actionContext = new BusinessActionContext();
        Map<String, Object> context = new HashMap<>();
        actionContext.setActionContext(context);
        // put the context of the invocation into actionContext
        // contain: object, method name, parameter types, arguments
        context.put(Constants.TCC_PROXY_BEAN_NAME, this.beanName);
        context.put(Constants.TCC_PROXY_METHOD_NAME, methodName);
        if (parameterTypes.length > 0) {
            context.put(Constants.TCC_PROXY_METHOD_PARAMETER_TYPES, JSON.toJSONString(parameterTypes));
            context.put(Constants.TCC_PROXY_METHOD_ARGS, JSON.toJSONString(args));
        }
        // do prepare
        tccAutoProxyAction.prepare(actionContext);

        // the return type is Void.class, so return null
        return null;
    }

    @Override
    public int getOrder() {
        return orderNum;
    }
}
