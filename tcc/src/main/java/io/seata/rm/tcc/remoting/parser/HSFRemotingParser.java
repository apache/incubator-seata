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
package io.seata.rm.tcc.remoting.parser;

import io.seata.common.exception.FrameworkException;
import io.seata.common.util.ReflectionUtil;
import io.seata.rm.tcc.remoting.Protocols;
import io.seata.rm.tcc.remoting.RemotingDesc;

/**
 * HSF Remote Bean Parser
 *
 * @author ppf@jiumao.org
 */
public class HSFRemotingParser extends AbstractedRemotingParser {

    /**
     * is HSF env
     */
    private static volatile boolean isHsf;

    static {
        // check HSF runtime env
        try {
            Class.forName("com.taobao.hsf.app.api.util.HSFApiConsumerBean");
            Class.forName("com.taobao.hsf.app.api.util.HSFApiProviderBean");

            Class.forName("com.taobao.hsf.app.spring.util.HSFSpringConsumerBean");
            Class.forName("com.taobao.hsf.app.spring.util.HSFSpringProviderBean");

            isHsf = true;
        } catch (ClassNotFoundException e) {
            isHsf = false;
        }
    }

    @Override
    public boolean isRemoting(Object bean, String beanName) {
        return isHsf && (isReference(bean, beanName) || isService(bean, beanName));
    }

    @Override
    public boolean isReference(Object bean, String beanName) {
        String beanClassName = bean.getClass().getName();
        return isHsf && ("com.taobao.hsf.app.spring.util.HSFSpringConsumerBean".equals(beanClassName) || "org.springframework.beans.factory.FactoryBean".equals(beanClassName));
    }

    @Override
    public boolean isService(Object bean, String beanName) {
        String beanClassName = bean.getClass().getName();
        return isHsf && "com.taobao.hsf.app.spring.util.HSFSpringProviderBean".equals(beanClassName);
    }

    @Override
    public RemotingDesc getServiceDesc(Object bean, String beanName) throws FrameworkException {
        if (!this.isRemoting(bean, beanName)) {
            return null;
        }
        try {
            if (isReference(bean, beanName)) {
                Object consumerBean = ReflectionUtil.getFieldValue(bean, "consumerBean");
                Object metadata = ReflectionUtil.invokeMethod(consumerBean, "getMetadata");

                Class<?> interfaceClass = (Class<?>) ReflectionUtil.invokeMethod(metadata, "getIfClazz");
                String interfaceClassName = (String) ReflectionUtil.invokeMethod(metadata, "getInterfaceName");
                String uniqueId = (String) ReflectionUtil.invokeMethod(metadata, "getVersion");
                String group = (String) ReflectionUtil.invokeMethod(metadata, "getGroup");

                RemotingDesc serviceBeanDesc = new RemotingDesc();
                serviceBeanDesc.setServiceClass(interfaceClass);
                serviceBeanDesc.setServiceClassName(interfaceClassName);
                serviceBeanDesc.setUniqueId(uniqueId);
                serviceBeanDesc.setGroup(group);
                serviceBeanDesc.setProtocol(Protocols.HSF);
                return serviceBeanDesc;
            } else if (isService(bean, beanName)) {
                Object consumerBean = ReflectionUtil.getFieldValue(bean, "providerBean");
                Object metadata = ReflectionUtil.invokeMethod(consumerBean, "getMetadata");

                String interfaceClassName = (String) ReflectionUtil.invokeMethod(metadata, "getInterfaceName");
                Class<?> interfaceClass = Class.forName(interfaceClassName);
                String uniqueId = (String) ReflectionUtil.invokeMethod(metadata, "getVersion");
                String group = (String) ReflectionUtil.invokeMethod(metadata, "getGroup");
                RemotingDesc serviceBeanDesc = new RemotingDesc();
                serviceBeanDesc.setServiceClass(interfaceClass);
                serviceBeanDesc.setServiceClassName(interfaceClassName);
                serviceBeanDesc.setUniqueId(uniqueId);
                serviceBeanDesc.setGroup(group);

                Object targetBean = ReflectionUtil.getFieldValue(metadata, "target");
                serviceBeanDesc.setTargetBean(targetBean);
                serviceBeanDesc.setProtocol(Protocols.HSF);
                return serviceBeanDesc;
            }
        } catch (Throwable t) {
            throw new FrameworkException(t);
        }
        return null;
    }


    @Override
    public short getProtocol() {
        return Protocols.HSF;
    }
}
