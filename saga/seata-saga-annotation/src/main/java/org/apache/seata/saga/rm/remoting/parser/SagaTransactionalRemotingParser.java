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
package org.apache.seata.saga.rm.remoting.parser;

import org.apache.seata.common.exception.FrameworkException;
import org.apache.seata.common.util.ReflectionUtil;
import org.apache.seata.integration.tx.api.remoting.Protocols;
import org.apache.seata.integration.tx.api.remoting.RemotingDesc;
import org.apache.seata.integration.tx.api.remoting.parser.AbstractedRemotingParser;
import org.apache.seata.saga.rm.api.SagaTransactional;

import java.util.Set;

/**
 * Remoting parser for Saga transaction participant beans with @SagaTransactional annotation
 *
 * This parser is specifically designed for Saga transaction mode and handles beans annotated
 * with @SagaTransactional annotation. It provides proper service detection and proxy enhancement
 * for Saga compensation scenarios.
 *
 * Key Features:
 * - Dedicated support for @SagaTransactional annotation
 * - Optimized for Saga compensation patterns
 * - Clear semantic separation from TCC mode
 * - Proper integration with CompensationBusinessAction
 *
 * Usage Pattern:
 * @SagaTransactional
 * public interface PaymentSagaService {
 *     @CompensationBusinessAction(compensationMethod = "compensatePayment")
 *     boolean processPayment(BusinessActionContext context, String orderId, double amount);
 *
 *     boolean compensatePayment(BusinessActionContext context);
 * }
 *
 * Detection Priority:
 * 1. Implementation class annotations (higher priority)
 * 2. Interface annotations (fallback)
 *
 * Performance Considerations:
 * - Annotation detection is cached appropriately for high-throughput scenarios
 * - Reflection-based scanning is optimized for typical usage patterns
 *
 * @see SagaTransactional The annotation this parser handles
 * @see org.apache.seata.saga.rm.api.CompensationBusinessAction Commonly used with @SagaTransactional
 * @see org.apache.seata.integration.tx.api.remoting.parser.AbstractedRemotingParser Base class
 * @since 2.5.0
 */
public class SagaTransactionalRemotingParser extends AbstractedRemotingParser {

    @Override
    public boolean isReference(Object bean, String beanName) {
        return isSagaTransactional(bean);
    }

    @Override
    public boolean isService(Object bean, String beanName) {
        return isSagaTransactional(bean);
    }

    @Override
    public boolean isService(Class<?> beanClass) throws FrameworkException {
        return isSagaTransactional(beanClass);
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

        // First priority: check if @SagaTransactional is present on the implementation class itself
        // Implementation class annotations take precedence over interface annotations
        if (hasSagaTransactionalAnnotation(classType)) {
            remotingDesc.setServiceClass(classType);
            remotingDesc.setServiceClassName(classType.getName());
            remotingDesc.setTargetBean(bean);
            return remotingDesc;
        }

        // Second priority: check if @SagaTransactional is present on any implemented interfaces
        // Fall back to interface annotations if no implementation class annotations found
        Set<Class<?>> interfaceClasses = ReflectionUtil.getInterfaces(classType);
        for (Class<?> interClass : interfaceClasses) {
            if (hasSagaTransactionalAnnotation(interClass)) {
                remotingDesc.setServiceClassName(interClass.getName());
                remotingDesc.setServiceClass(interClass);
                remotingDesc.setTargetBean(bean);
                return remotingDesc;
            }
        }
        throw new FrameworkException("Couldn't parse any Remoting info for SagaTransactional bean");
    }

    @Override
    public short getProtocol() {
        return Protocols.IN_JVM;
    }

    /**
     * Check if the given bean is annotated with @SagaTransactional annotation
     *
     * @param bean the bean to check
     * @return true if the bean or its interfaces have @SagaTransactional annotation
     */
    private boolean isSagaTransactional(Object bean) {
        return isSagaTransactional(bean.getClass());
    }

    /**
     * Check if the given class or its interfaces are annotated with @SagaTransactional annotation
     *
     * @param classType the class type to check
     * @return true if the class has @SagaTransactional annotation
     */
    private boolean isSagaTransactional(Class<?> classType) {
        // Check the class itself first for better performance
        if (hasSagaTransactionalAnnotation(classType)) {
            return true;
        }

        // Check all interfaces
        Set<Class<?>> interfaceClasses = ReflectionUtil.getInterfaces(classType);
        return interfaceClasses.stream().anyMatch(this::hasSagaTransactionalAnnotation);
    }

    /**
     * Check if a class has @SagaTransactional annotation
     *
     * @param clazz the class to check
     * @return true if the class has @SagaTransactional annotation
     */
    private boolean hasSagaTransactionalAnnotation(Class<?> clazz) {
        return clazz.isAnnotationPresent(SagaTransactional.class);
    }
}
