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
package io.seata.spring.proxy;

import java.lang.reflect.Method;

import io.seata.common.util.ReflectionUtil;
import io.seata.core.context.RootContext;
import io.seata.core.logger.StackTraceLogger;
import io.seata.rm.tcc.api.BusinessActionContextUtil;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;

/**
 * Seata Proxy Interceptor
 *
 * @author wang.liang
 */
public class SeataProxyInterceptor implements MethodInterceptor, Ordered {

    private static final Logger LOGGER = LoggerFactory.getLogger(SeataProxyInterceptor.class);

    private final String targetBeanName;
    private final SeataProxyHandler seataProxyHandler;
    private final int orderNum;

    public SeataProxyInterceptor(String targetBeanName, SeataProxyHandler seataProxyHandler, int orderNum) {
        this.targetBeanName = targetBeanName;
        this.seataProxyHandler = seataProxyHandler;
        this.orderNum = orderNum;
    }

    @Override
    public Object invoke(final MethodInvocation invocation) throws Throwable {
        // ignored if the global transaction not exists
        if (!RootContext.inGlobalTransaction() || RootContext.inSagaBranch()) {
            return invocation.proceed();
        }

        // get method
        Method method = invocation.getMethod();

        // check whether the method can be proxied
        if (method.getReturnType() != Void.class) {
            return invocation.proceed();
        }

        // if in the try method of the TCC branch, offer a suggestion
        if (RootContext.inTccBranch() && LOGGER.isWarnEnabled() && StackTraceLogger.needToPrintLog()) {
            LOGGER.warn("Currently in the try method of the TCC branch, it's recommended to transfer the `{}`" +
                        " to the commit method of the TCC action '{}'.",
                ReflectionUtil.methodToString(method), BusinessActionContextUtil.getContext().getActionName());
        }

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("`{}` is proxied by the handler '{}' in the '{}'.", ReflectionUtil.methodToString(method),
                    this.seataProxyHandler.getClass().getName(), this.getClass().getName());
        }

        // do proxy
        this.seataProxyHandler.doProxy(this.targetBeanName, invocation);

        // the return type is Void.class, so return null
        return null;
    }

    @Override
    public int getOrder() {
        return orderNum;
    }
}
