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
package io.seata.rm.tcc.remoting.parser;

import io.seata.common.exception.FrameworkException;
import io.seata.common.util.ReflectionUtil;
import io.seata.integration.tx.api.remoting.Protocols;
import io.seata.integration.tx.api.remoting.RemotingDesc;
import io.seata.integration.tx.api.remoting.parser.AbstractedRemotingParser;
import io.seata.rm.tcc.api.LocalTCC;
import org.springframework.aop.framework.AopProxyUtils;

import java.util.Set;

/**
 * local tcc bean parsing
 *
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
    public RemotingDesc getServiceDesc(Object bean, String beanName) throws FrameworkException {
        if (!this.isRemoting(bean, beanName)) {
            return null;
        }
        RemotingDesc remotingDesc = new RemotingDesc();
        remotingDesc.setReference(this.isReference(bean, beanName));
        remotingDesc.setService(this.isService(bean, beanName));
        remotingDesc.setProtocol(Protocols.IN_JVM);
        Class<?> classType = bean.getClass();
        // check if LocalTCC annotation is marked on the implementation class
        if (classType.isAnnotationPresent(LocalTCC.class)) {
            remotingDesc.setServiceClass(AopProxyUtils.ultimateTargetClass(bean));
            remotingDesc.setServiceClassName(remotingDesc.getServiceClass().getName());
            remotingDesc.setTargetBean(bean);
            return remotingDesc;
        }
        // check if LocalTCC annotation is marked on the interface
        Set<Class<?>> interfaceClasses = ReflectionUtil.getInterfaces(classType);
        for (Class<?> interClass : interfaceClasses) {
            if (interClass.isAnnotationPresent(LocalTCC.class)) {
                remotingDesc.setServiceClassName(interClass.getName());
                remotingDesc.setServiceClass(interClass);
                remotingDesc.setTargetBean(bean);
                return remotingDesc;
            }
        }
        throw new FrameworkException("Couldn't parser any Remoting info");
    }

    @Override
    public short getProtocol() {
        return Protocols.IN_JVM;
    }

    /**
     * Determine whether there is an annotation on interface or impl {@link LocalTCC}
     * @param bean the bean
     * @return boolean
     */
    private boolean isLocalTCC(Object bean) {
        Class<?> classType = bean.getClass();
        Set<Class<?>> interfaceClasses = ReflectionUtil.getInterfaces(classType);
        for (Class<?> interClass : interfaceClasses) {
            if (interClass.isAnnotationPresent(LocalTCC.class)) {
                return true;
            }
        }
        return classType.isAnnotationPresent(LocalTCC.class);
    }
}
