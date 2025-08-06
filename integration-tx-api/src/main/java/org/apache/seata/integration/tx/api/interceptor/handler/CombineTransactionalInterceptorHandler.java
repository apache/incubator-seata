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
package org.apache.seata.integration.tx.api.interceptor.handler;

import org.apache.seata.core.context.RootContext;
import org.apache.seata.integration.tx.api.interceptor.InvocationHandlerType;
import org.apache.seata.integration.tx.api.interceptor.InvocationWrapper;
import org.apache.seata.integration.tx.api.interceptor.SeataInterceptorPosition;
import org.apache.seata.integration.tx.api.util.ClassUtils;
import org.apache.seata.rm.datasource.combine.CombineConnectionHolder;
import org.apache.seata.rm.datasource.xa.ConnectionProxyXA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * The type Combine transactional interceptor handler.
 *
 */
public class CombineTransactionalInterceptorHandler extends AbstractProxyInvocationHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CombineTransactionalInterceptorHandler.class);

    private Set<String> methodsToProxy;

    private static volatile ScheduledThreadPoolExecutor executor;

    public CombineTransactionalInterceptorHandler(Set<String> methodsToProxy) {
        this.methodsToProxy = methodsToProxy;
    }

    @Override
    protected Object doInvoke(InvocationWrapper invocation) throws Throwable {
        Class<?> targetClass = invocation.getTarget().getClass();
        Method specificMethod = ClassUtils.getMostSpecificMethod(invocation.getMethod(), targetClass);
        if (specificMethod != null && !specificMethod.getDeclaringClass().equals(Object.class)) {
            return handleCombineTransactional(invocation);
        }
        return invocation.proceed();
    }

    private Object handleCombineTransactional(final InvocationWrapper methodInvocation) throws Throwable {
        if (!RootContext.inGlobalTransaction()) {
            // not in transaction, or this interceptor is disabled
            return methodInvocation.proceed();
        }

        RootContext.bindCombineTransaction();

        try {
            Object result = methodInvocation.proceed();

            for (ConnectionProxyXA conn : CombineConnectionHolder.getDsConn()) {
                conn.setCombine(false);
                conn.commit();
            }
            return result;
        } catch (Exception e) {
            LOGGER.error(
                    String.format(
                            "@CombineTransactional failed to handle,xid: %s occur exp msg: %s",
                            RootContext.getXID(), e.getMessage()),
                    e);
            // doRollback
            for (ConnectionProxyXA conn : CombineConnectionHolder.getDsConn()) {
                conn.setCombine(false);
                conn.rollback();
            }
            throw e;
        } finally {
            for (ConnectionProxyXA conn : CombineConnectionHolder.getDsConn()) {
                try {
                    // Reset autocommit (if not autocommitting)
                    if (!conn.getAutoCommit()) {
                        conn.setAutoCommit(true);
                    }
                } catch (Throwable t) {
                    // Record the exception of resetting the auto-commit, but do not interrupt and continue to try to
                    // close
                    LOGGER.error("Failed to reset autoCommit to true for connection: {}", conn, t);
                }
                try {
                    if (conn.isClosed()) {
                        LOGGER.warn("Connection is closed: {}", conn);
                    }
                    conn.close();
                } catch (Throwable t) {
                    // Record the exception of closing the connection, but do not interrupt the loop and continue to
                    // process the next connection
                    LOGGER.error("Failed to close connection: {}", conn, t);
                }
            }
            // Clean up local cache connections
            CombineConnectionHolder.clear();
            RootContext.unbindCombineTransaction();
        }
    }

    @Override
    public Set<String> getMethodsToProxy() {
        return methodsToProxy;
    }

    @Override
    public SeataInterceptorPosition getPosition() {
        return SeataInterceptorPosition.AfterTransaction;
    }

    @Override
    public String type() {
        return InvocationHandlerType.CombineTransactional.name();
    }

    @Override
    public int order() {
        return 1;
    }
}
