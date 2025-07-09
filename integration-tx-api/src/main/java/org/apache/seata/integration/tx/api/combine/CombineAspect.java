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
package org.apache.seata.integration.tx.api.combine;

import org.apache.seata.core.context.RootContext;
import org.apache.seata.rm.datasource.combine.CombineConnectionHolder;
import org.apache.seata.rm.datasource.combine.CombineContext;
import org.apache.seata.rm.datasource.xa.ConnectionProxyXA;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class CombineAspect {
    private static final Logger LOGGER = LoggerFactory.getLogger(CombineAspect.class);

    @Around("@annotation(org.apache.seata.spring.annotation.CombineTransactional)")
    public Object handleCombine(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!RootContext.inGlobalTransaction() || !RootContext.inXABranch()) {
            // not in transaction, or this interceptor is disabled
            return joinPoint.proceed();
        }

        if (!CombineContext.set()) {
            // 同一个全局事务，切面无须进入
            return joinPoint.proceed();
        }

        try {
            // 第一次切面进入
            Object result = joinPoint.proceed();

            // doCleanupAfterCompletion标识事务结束，重置并关闭连接
            CombineContext.clear();
            // doCommit
            for (ConnectionProxyXA conn : CombineConnectionHolder.getDsConn()) {
                conn.commit();
            }
            return result;
        } catch (Exception e) {
            LOGGER.error(
                    String.format("Failed to handle,xid: %s occur exp msg: %s", RootContext.getXID(), e.getMessage()),
                    e);
            CombineContext.clear();
            // doRollback
            for (ConnectionProxyXA conn : CombineConnectionHolder.getDsConn()) {
                conn.rollback();
            }
            throw e;
        } finally {
            CombineContext.clear();
            for (ConnectionProxyXA conn : CombineConnectionHolder.getDsConn()) {
                try {
                    // 重置自动提交（如果非自动提交）
                    if (!conn.getAutoCommit()) {
                        conn.setAutoCommit(true);
                    }
                } catch (Throwable t) {
                    // 记录重置自动提交的异常，但不中断，继续尝试关闭
                    LOGGER.error("Failed to reset autoCommit to true for connection: {}", conn, t);
                }
                try {
                    if (conn.isClosed()) {
                        LOGGER.error("Connection is closed: {}", conn);
                    }
                    conn.close();
                } catch (Throwable t) {
                    // 记录关闭连接的异常，但不中断循环，继续处理下一个连接
                    LOGGER.error("Failed to close connection: {}", conn, t);
                }
            }
            // 清理本地缓存连接
            CombineConnectionHolder.clear();
        }
    }
}
