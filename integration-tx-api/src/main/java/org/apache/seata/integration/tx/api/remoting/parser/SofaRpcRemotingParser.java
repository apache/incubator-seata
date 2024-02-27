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
package org.apache.seata.integration.tx.api.remoting.parser;

import org.apache.seata.common.exception.FrameworkException;
import org.apache.seata.common.util.ReflectionUtil;
import org.apache.seata.integration.tx.api.remoting.Protocols;
import org.apache.seata.integration.tx.api.remoting.RemotingDesc;

/**
 * sofa-rpc remoting bean parsing
 *
 */
public class SofaRpcRemotingParser extends AbstractedRemotingParser {


    @Override
    public boolean isReference(Object bean, String beanName)
            throws FrameworkException {
        String beanClassName = bean.getClass().getName();
        return "com.alipay.sofa.runtime.spring.factory.ReferenceFactoryBean".equals(beanClassName);
    }


    @Override
    public boolean isService(Object bean, String beanName) throws FrameworkException {
        String beanClassName = bean.getClass().getName();
        return "com.alipay.sofa.runtime.spring.factory.ServiceFactoryBean".equals(beanClassName);
    }

    @Override
    public boolean isService(Class<?> beanClass) throws FrameworkException {
        String beanClassName = beanClass.getName();
        return "com.alipay.sofa.runtime.spring.factory.ServiceFactoryBean".equals(beanClassName);
    }

    @Override
    public RemotingDesc getServiceDesc(Object bean, String beanName) throws FrameworkException {
        if (!this.isRemoting(bean, beanName)) {
            return null;
        }
        try {
            RemotingDesc serviceBeanDesc = new RemotingDesc();
            Class<?> interfaceClass = (Class<?>)ReflectionUtil.invokeMethod(bean, "getInterfaceClass");
            String interfaceClassName = ReflectionUtil.getFieldValue(bean, "interfaceType");
            String uniqueId = ReflectionUtil.getFieldValue(bean, "uniqueId");
            serviceBeanDesc.setServiceClass(interfaceClass);
            serviceBeanDesc.setServiceClassName(interfaceClassName);
            serviceBeanDesc.setUniqueId(uniqueId);
            serviceBeanDesc.setProtocol(Protocols.SOFA_RPC);
            if (isService(bean, beanName)) {
                Object targetBean = ReflectionUtil.getFieldValue(bean, "ref");
                serviceBeanDesc.setTargetBean(targetBean);
            }
            serviceBeanDesc.setReference(this.isReference(bean, beanName));
            serviceBeanDesc.setService(this.isService(bean, beanName));
            return serviceBeanDesc;
        } catch (Throwable t) {
            throw new FrameworkException(t);
        }
    }

    @Override
    public short getProtocol() {
        return Protocols.SOFA_RPC;
    }
}
