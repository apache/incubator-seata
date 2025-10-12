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
package org.apache.seata.rm.tcc.remoting.parser;

import org.apache.seata.common.exception.FrameworkException;
import org.apache.seata.common.util.ReflectionUtil;
import org.apache.seata.integration.tx.api.remoting.Protocols;
import org.apache.seata.integration.tx.api.remoting.RemotingDesc;
import org.apache.seata.integration.tx.api.remoting.parser.AbstractedRemotingParser;
import org.apache.seata.rm.tcc.api.LocalTCC;
import org.springframework.aop.framework.AopProxyUtils;

import java.util.Set;

/**
 * Remoting parser for TCC transaction participant beans with @LocalTCC annotation
 *
 * This parser is specifically designed for TCC (Try-Confirm-Cancel) transaction mode
 * and handles beans annotated with @LocalTCC annotation. It provides proper service
 * detection and proxy enhancement for TCC scenarios.
 *
 * Key Features:
 * - Dedicated support for @LocalTCC annotation
 * - Optimized for TCC Try-Confirm-Cancel patterns
 * - Proper integration with TwoPhaseBusinessAction
 * - High-performance annotation detection
 *
 * Usage Pattern:
 * @LocalTCC
 * public interface PaymentTccService {
 *     @TwoPhaseBusinessAction(name = "payment", commitMethod = "confirmPayment", rollbackMethod = "cancelPayment")
 *     boolean tryPayment(BusinessActionContext context, String orderId, double amount);
 *
 *     boolean confirmPayment(BusinessActionContext context);
 *     boolean cancelPayment(BusinessActionContext context);
 * }
 *
 * Detection Priority:
 * 1. Implementation class annotations (higher priority)
 * 2. Interface annotations (fallback)
 *
 * Note: For Saga scenarios, use @SagaTransactional with SagaTransactionalRemotingParser instead.
 *
 * @see LocalTCC The TCC-specific annotation this parser handles
 * @see org.apache.seata.rm.tcc.api.TwoPhaseBusinessAction Commonly used with @LocalTCC
 * @see org.apache.seata.integration.tx.api.remoting.parser.AbstractedRemotingParser Base class
 * @since 1.0.0
 */
public class LocalTCCRemotingParser extends AbstractedRemotingParser {

    @Override
    public boolean isReference(Object bean, String beanName) {
        return isLocalTCC(bean);
    }

    @Override
    public boolean isService(Object bean, String beanName) {
        return isLocalTCC(bean);
    }

    @Override
    public boolean isService(Class<?> beanClass) throws FrameworkException {
        return isLocalTCC(beanClass);
    }

    @Override
    public RemotingDesc getServiceDesc(Object bean, String beanName) throws FrameworkException {
        if (!this.isRemoting(bean, beanName)) {
            return null;
        }
        RemotingDesc remotingDesc = new RemotingDesc();
        remotingDesc.setReference(this.isReference(bean, beanName));
        remotingDesc.setService(this.isService(bean, beanName));
        remotingDesc.setProtocol(Protocols.IN_JVM);
        Class<?> classType = bean.getClass();

        // First priority: check if @LocalTCC is present on the implementation class itself
        // Implementation class annotations take precedence over interface annotations
        if (hasLocalTCCAnnotation(classType)) {
            remotingDesc.setServiceClass(AopProxyUtils.ultimateTargetClass(bean));
            remotingDesc.setServiceClassName(remotingDesc.getServiceClass().getName());
            remotingDesc.setTargetBean(bean);
            return remotingDesc;
        }

        // Second priority: check if @LocalTCC is present on any implemented interfaces
        // Fall back to interface annotations if no implementation class annotations found
        Set<Class<?>> interfaceClasses = ReflectionUtil.getInterfaces(classType);
        for (Class<?> interClass : interfaceClasses) {
            if (hasLocalTCCAnnotation(interClass)) {
                remotingDesc.setServiceClassName(interClass.getName());
                remotingDesc.setServiceClass(interClass);
                remotingDesc.setTargetBean(bean);
                return remotingDesc;
            }
        }
        throw new FrameworkException("Couldn't parse any Remoting info for LocalTCC bean");
    }

    @Override
    public short getProtocol() {
        return Protocols.IN_JVM;
    }

    /**
     * Check if the given bean is annotated with @LocalTCC annotation
     *
     * @param bean the bean to check
     * @return true if the bean or its interfaces have @LocalTCC annotation
     */
    private boolean isLocalTCC(Object bean) {
        return isLocalTCC(bean.getClass());
    }

    /**
     * Check if the given class or its interfaces are annotated with @LocalTCC annotation
     *
     * @param classType the class type to check
     * @return true if the class has @LocalTCC annotation
     */
    private boolean isLocalTCC(Class<?> classType) {
        // Check the class itself first for better performance
        if (hasLocalTCCAnnotation(classType)) {
            return true;
        }

        // Check all interfaces
        Set<Class<?>> interfaceClasses = ReflectionUtil.getInterfaces(classType);
        return interfaceClasses.stream().anyMatch(this::hasLocalTCCAnnotation);
    }

    /**
     * Check if a class has @LocalTCC annotation
     *
     * @param clazz the class to check
     * @return true if the class has @LocalTCC annotation
     */
    private boolean hasLocalTCCAnnotation(Class<?> clazz) {
        return clazz.isAnnotationPresent(LocalTCC.class);
    }
}
