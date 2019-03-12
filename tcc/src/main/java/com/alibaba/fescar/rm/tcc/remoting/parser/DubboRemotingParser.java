/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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
package com.alibaba.fescar.rm.tcc.remoting.parser;

import com.alibaba.fescar.common.exception.FrameworkException;
import com.alibaba.fescar.common.util.ReflectionUtil;
import com.alibaba.fescar.rm.tcc.remoting.Protocols;
import com.alibaba.fescar.rm.tcc.remoting.RemotingDesc;

/**
 * dubbo  remoting bean parsing
 *
 * @author zhangsen
 */
public class DubboRemotingParser extends AbstractedRemotingParser {

    @Override
    public boolean isReference(Object bean, String beanName) throws FrameworkException {
        Class<?> c = bean.getClass();
        if("com.alibaba.dubbo.config.spring.ReferenceBean".equals(c.getName())
                || "org.apache.dubbo.config.spring.ReferenceBean".equals(c.getName())){
            return true;
        }
        return false;
    }

    @Override
    public boolean isService(Object bean, String beanName) throws FrameworkException {
        Class<?> c = bean.getClass();
        if("com.alibaba.dubbo.config.spring.ServiceBean".equals(c.getName())
                || "org.apache.dubbo.config.spring.ServiceBean".equals(c.getName())){
            return true;
        }
        return false;
    }

    @Override
    public RemotingDesc getServiceDesc(Object bean, String beanName) throws FrameworkException {
        if(!this.isRemoting(bean, beanName)){
            return null;
        }
        try{
            RemotingDesc serviceBeanDesc = new RemotingDesc();
            Class<?> interfaceClass = (Class<?>) ReflectionUtil.invokeMethod(bean, "getInterfaceClass");
            String interfaceClassName = (String) ReflectionUtil.getFieldValue(bean, "interfaceName");
            String version = (String) ReflectionUtil.invokeMethod(bean, "getVersion");
            String group = (String)ReflectionUtil.invokeMethod(bean, "getGroup");
            serviceBeanDesc.setInterfaceClass(interfaceClass);
            serviceBeanDesc.setInterfaceClassName(interfaceClassName);
            serviceBeanDesc.setUniqueId(version);
            serviceBeanDesc.setGroup(group);
            serviceBeanDesc.setProtocol(Protocols.DUBBO.getCode());
            if(isService(bean, beanName)){
                Object targetBean = ReflectionUtil.getFieldValue(bean, "ref");
                serviceBeanDesc.setTargetBean(targetBean);
            }
            return serviceBeanDesc;
        }catch (Throwable t){
            throw new FrameworkException(t);
        }
    }

    @Override
    public Protocols getProtocol() {
        return Protocols.DUBBO;
    }
}
