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
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.ReflectionUtil;
import io.seata.core.context.RootContext;
import io.seata.core.logger.StackTraceLogger;
import io.seata.rm.tcc.api.BusinessActionContextUtil;
import io.seata.spring.annotation.SeataInterceptor;
import io.seata.spring.annotation.SeataInterceptorPosition;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Seata Proxy Interceptor
 *
 * @author wang.liang
 * @see SeataProxyHandler
 */
public class SeataProxyInterceptor implements MethodInterceptor, SeataInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(SeataProxyInterceptor.class);

    private final String targetBeanName;
    private final SeataProxyHandler seataProxyHandler;
    private int orderNum;

    public SeataProxyInterceptor(String targetBeanName, SeataProxyHandler seataProxyHandler, int orderNum) {
        this.targetBeanName = targetBeanName;
        this.seataProxyHandler = seataProxyHandler;
        this.orderNum = orderNum;
    }

    @Override
    public Object invoke(final MethodInvocation invocation) throws Throwable {
        try {
            // ignored if the global transaction not exists
            if (!RootContext.inGlobalTransaction() || RootContext.inSagaBranch() || !SeataProxyUtil.isNeedProxy() ||
                    this.shouldSkip(invocation)) {
                return invocation.proceed();
            }

            // get method
            Method method = invocation.getMethod();

            // if in the try method of the TCC branch, offer a suggestion
            if (RootContext.inTccBranch() && LOGGER.isWarnEnabled() && StackTraceLogger.needToPrintLog()) {
                LOGGER.warn("Currently in the try method of the TCC branch, it's recommended to " +
                            "transfer the `{}` to the commit method of the TCC action '{}'.",
                        ReflectionUtil.methodToString(method), BusinessActionContextUtil.getContext().getActionName());
            }

            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("the method `{}` is proxied by the proxy handler '{}' in the '{}'.", method.getName(),
                        this.seataProxyHandler.getClass().getName(), SeataProxyInterceptor.class.getName());
            }

            try {
                // do proxy
                Object result = this.seataProxyHandler.doProxy(this.targetBeanName, invocation);

                // if the result is null, validate the return type
                if (result == null) {
                    Class<?> returnType = method.getReturnType();

                    // if the return type
                    if (returnType == boolean.class || returnType == Boolean.class) {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("`null` is returned from `{}.doProxy(...)`, but the return type is `{}.class`, " +
                                         "so return `true` to replace the `null`.",
                                    this.seataProxyHandler.getClass().getName(), returnType.getSimpleName());
                        }
                        // return `true` to replace the `null`
                        return true;
                    }

                    // if the return type is not void, print warn log
                    if (returnType != void.class && returnType != Void.class
                            && LOGGER.isWarnEnabled() && StackTraceLogger.needToPrintLog()) {
                        LOGGER.warn("`null` is returned from `{}.doProxy(...)`, but the return type of `{}` is not `void.class`. " +
                                        "If you do not want the method to be proxied, please use the `SeataProxyUtil.disableProxy()` before calling the method, " +
                                        "or add the `@{}(skip = true)` on the method.",
                                this.seataProxyHandler.getClass().getName(), ReflectionUtil.methodToString(method), SeataProxy.class.getSimpleName());
                    }
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

    private boolean shouldSkip(MethodInvocation invocation) {
        Method method = invocation.getMethod();

        // skip `Object.toString()`
        String methodName = method.getName();
        if ("toString".equals(methodName) && CollectionUtils.isEmpty(method.getParameterTypes())) {
            return true;
        }

        // get annotation and skip if {@code skip() == true}
        SeataProxy annotation = method.getAnnotation(SeataProxy.class);
        if (annotation != null && annotation.skip()) {
            return true;
        }

        // check by the handler
        if (this.seataProxyHandler.shouldSkip(this.targetBeanName, invocation)) {
            return true;
        }

        return false;
    }

    @Override
    public int getOrder() {
        return orderNum;
    }

    @Override
    public void setOrder(int order) {
        this.orderNum = order;
    }

    @Override
    public SeataInterceptorPosition getPosition() {
        return SeataInterceptorPosition.BeforeTransaction;
    }
}
