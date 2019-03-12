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
import com.alibaba.fescar.rm.tcc.api.LocalTCC;
import com.alibaba.fescar.rm.tcc.remoting.Protocols;
import com.alibaba.fescar.rm.tcc.remoting.RemotingDesc;

import java.util.Set;

/**
 * local tcc bean parsing
 *
 * @author zhangsen
 */
public class LocalTCCRemotingParser extends AbstractedRemotingParser {

    @Override
    public boolean isReference(Object bean, String beanName)  {
        Class<?> classType = bean.getClass();
        Set<Class<?>> interfaceClasses = ReflectionUtil.getInterfaces(classType);
        for(Class<?> interClass : interfaceClasses){
            if(interClass.isAnnotationPresent(LocalTCC.class)){
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isService(Object bean, String beanName) {
        Class<?> classType = bean.getClass();
        Set<Class<?>> interfaceClasses = ReflectionUtil.getInterfaces(classType);
        for(Class<?> interClass : interfaceClasses){
            if(interClass.isAnnotationPresent(LocalTCC.class)){
                return true;
            }
        }
        return false;
    }

    @Override
    public RemotingDesc getServiceDesc(Object bean, String beanName) throws FrameworkException {
        if(!this.isRemoting(bean, beanName)){
            return null;
        }
        RemotingDesc remotingDesc = new RemotingDesc();
        remotingDesc.setReference(true);
        remotingDesc.setProtocol(Protocols.IN_JVM.getCode());
        Class<?> classType = bean.getClass();
        Set<Class<?>> interfaceClasses = ReflectionUtil.getInterfaces(classType);
        for(Class<?> interClass : interfaceClasses){
            if(interClass.isAnnotationPresent(LocalTCC.class)){
                remotingDesc.setInterfaceClassName(interClass.getName());
                remotingDesc.setInterfaceClass(interClass);
                remotingDesc.setTargetBean(bean);
                return remotingDesc;
            }
        }
        throw new FrameworkException("Couldn't parser any Remoting info");
    }

    @Override
    public Protocols getProtocol() {
        return Protocols.IN_JVM;
    }
}
