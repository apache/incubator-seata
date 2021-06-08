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

import io.seata.common.exception.FrameworkException;
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
        try {
            // ignored if the global transaction not exists
            if (!RootContext.inGlobalTransaction() || RootContext.inSagaBranch() || !SeataProxyUtil.isNeedProxy()) {
                return invocation.proceed();
            }

            // get method
            Method method = invocation.getMethod();

            // if in the try method of the TCC branch, offer a suggestion
            if (RootContext.inTccBranch() && LOGGER.isWarnEnabled() && StackTraceLogger.needToPrintLog()) {
                LOGGER.warn("Currently in the try method of the TCC branch, it's recommended to" +
                                " transfer the `{}` to the commit method of the TCC action '{}'.",
                        ReflectionUtil.methodToString(method), BusinessActionContextUtil.getContext().getActionName());
            }

            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("`{}` is proxied by the handler '{}' in the '{}'.", ReflectionUtil.methodToString(method),
                        this.seataProxyHandler.getClass().getName(), this.getClass().getName());
            }

            try {
                // do proxy
                Object result = this.seataProxyHandler.doProxy(this.targetBeanName, invocation);

                // check the result, and print warn log
                if (result == null && method.getReturnType() != void.class && method.getReturnType() != Void.class
                        && LOGGER.isWarnEnabled() && StackTraceLogger.needToPrintLog()) {
                    LOGGER.warn("The seata proxy result is null, but the return type of tye method `{}` is not `void.class`. " +
                                "If you do not want the method to be proxied, please use the `SeataProxyUtil.disableProxy()` before calling the method.",
                            ReflectionUtil.methodToString(method));
                }

                return result;
            } catch (Exception e) {
                LOGGER.error("do proxy failed, bean: {}, handler: {}, error: {}", this.targetBeanName,
                        this.seataProxyHandler.getClass().getName(), e.getMessage());
                throw new FrameworkException(e, "do proxy failed: " + e.getMessage());
            }
        } finally {
            SeataProxyUtil.enableProxy();
        }
    }

    @Override
    public int getOrder() {
        return orderNum;
    }
}
